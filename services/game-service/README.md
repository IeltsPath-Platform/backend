# Game Service

Public REST/WebSocket contract: [Wordly Game v1](../../docs/contracts/wordly-game-v1.md).

## Create a Wordly room

POST `/api/games/wordly/rooms` with an authenticated bearer token (local Gateway port 18080).

```json
{"playerMode":"MULTIPLAYER","initialLives":4,"playMode":"FINITE","wordLimit":10}
```

Use `playerMode: SOLO` for a private one-player room; multiplayer capacity is four.
Use `playMode: ENDLESS` with `wordLimit: null` for unlimited words. Lives must be 3–6;
finite word limit must be at least five. Time is fixed server-side at 180 seconds.
HTTP 201 returns the room ID, six-character code, verified host ID, `playerMode`,
`maxPlayers`, `settings`, and the host at slot 1. Room status is WAITING.
GET `/api/games/rooms/{id}` retrieves the settings for active members.
Other users cannot join solo rooms. V3 adds room player_mode for solo/multiplayer.
Room creation prepares the lobby; ready/start APIs initialize the Wordly match.
Spring WebSocket is already included for subsequent realtime updates.

## Game catalog (P-60)

`GET /api/games/catalog` through Gateway requires `Authorization: Bearer <access-token>`.
It returns HTTP 200 with the cards in display order:

```json
{
  "games": [
    {"gameType": "ROCKET", "name": "Tên lửa", "description": "Chọn từ tiếng Anh phù hợp với nghĩa được cho.", "status": "COMING_SOON"},
    {"gameType": "WORDLY", "name": "Wordly", "description": "Đoán từ tiếng Anh dựa trên nghĩa và gợi ý từng chữ cái.", "status": "AVAILABLE"}
  ]
}
```

Frontend disables `COMING_SOON` cards and opens the mode-selection popup for `AVAILABLE` cards.
The catalog is a fixed product list; it does not create rooms, sessions or call content services.
Catalog availability controls entry to the UI flow. Create Wordly rooms through
POST `/api/games/wordly/rooms`; legacy create endpoints keep their original game types.
Missing or invalid authentication returns HTTP 401. No database migration is needed for this endpoint.

## Wordly database migration

`V2__add_wordly_game_schema.sql` extends `game_db` without modifying V1 or removing legacy data.
It adds room/match settings, player progress and reconnect deadlines, and the tables
`game_match_words`, `game_player_words`, and `game_guesses`. All seven room/match/Wordly tables
have `is_delete` and `deleted_at`; mark both together to soft-delete. No purge job is installed.

Existing names are retained: room `status = IN_MATCH`, match `status = COMPLETED`, player
`status = ACTIVE/FINISHED`, player total `score` and final `rank`. Member `status = READY`
represents readiness instead of a duplicate boolean. Room codes remain VARCHAR(20) for legacy
data, with a six-character constraint and four-player limit for `game_type = WORDLY`.
Member slots are nullable for legacy rows; Wordly use cases must allocate slots 1–4.
V7 upgrades player `score` to BIGINT, matching the long-valued score mappings for endless play.

Flyway applies migrations on service startup. Wordly has its own room, word, guess and
solo gameplay use cases; legacy session/answer APIs retain their existing behavior.

`game-service` runs on port `8087` and uses its own `game_db` in the root Compose PostgreSQL instance (host port `5440`). Gateway routes
`/api/games/**` and `/ws/games/**` here.

Before starting a room or session, Game requests an immutable content snapshot via
`POST /internal/game-content/snapshots`. It chooses the owner from `learningDomain`: `VOCABULARY` calls Library
Service; `GRAMMAR` calls Content Service. Both use the same
[snapshot contract](../../docs/contracts/game-content-snapshot-v1.md). Game forwards its authenticated bearer
token and `X-Correlation-Id`.

The root Compose stack includes Config Server, Content, Library and Game; container URLs use the service names. For
Java running on the host, `LIBRARY_SERVICE_URL` defaults to `http://localhost:8081` in Config Server and
`CONTENT_SERVICE_URL` must be set to `http://localhost:8082`.

Run the full stack with `docker compose up -d --build`; for host development start PostgreSQL using
`docker compose up -d postgres` and set `GAME_DB_URL=jdbc:postgresql://localhost:5440/game_db` plus the local
`GAME_DB_PASSWORD`. Verify with `mvn -q -pl services/game-service -am test`; persistence tests need Docker.

## Room member names

Room REST responses and WebSocket room messages include `members[].displayName`.
The name is the account fullName fetched from User Service `/api/users/me` with the
verified internal bearer and correlation ID when creating/joining a room. It is stored
as a snapshot in `game_room_members.display_name` (V4). Existing members may have null
names until rejoining; changes to account names appear on the next room entry.
Configure `USER_SERVICE_URL` (default http://localhost:8085) when User Service runs elsewhere.
Profile lookup failures prevent creating/joining the room; no client-supplied name is trusted.

## Leaving the waiting room

When the host leaves or closes a waiting room, the room is CLOSED and soft-deleted
(is_delete=true, deleted_at set). Subsequent lookups/joins return 404.
The leave/close response includes a message naming the host. After commit, connected
WebSocket clients receive ROOM_CLOSED and their room connections are closed.
A guest leaving remains recorded as LEFT, frees the slot, and loses room-read access.
A fifth active player is rejected with HTTP 400 (Room is full).

## Ready and start

POST `/api/games/rooms/{roomId}/ready` with `{"ready":true}` or `{"ready":false}`.
Room responses include `canStart`: WAITING, at least two active multiplayer members,
and everyone (including the host) READY. Solo requires only the owner to be ready.
POST `/api/games/rooms/{roomId}/start` has no body and is host-only. It locks the room,
creates the match/players, snapshots settings, initializes lives, and changes the room
to IN_MATCH atomically. Repeat start and joins after start are rejected.
HTTP status is 200 on success, 400 for invalid room state/non-host start, 404 for missing room,
and 401 for missing authentication. Empty ready bodies are rejected with 400.
Committed room updates and match starts are sent to connected WebSocket clients.
Wordly uses game_player_words instead of legacy game_sessions; match response sessionId
is null for Wordly. Start assigns the first word and starts its deadline when the timer is enabled.

## Edit waiting room settings

PUT `/api/games/rooms/{roomId}/settings` with `{"initialLives":3,"wordLimit":10}`.
Only the host can edit (403 otherwise), and the room must be WAITING (400 otherwise).
Initial lives are always 3–6; this is already enforced by V2 database constraints and
WordlyRoomSettings, so no additional migration is required. Finite word count is at
least five; endless requires null wordLimit. Player mode, play mode, capacity and
180-second timer cannot be changed by this endpoint. Both editable values are supplied.
Actual changes reset READY members to JOINED; identical settings preserve readiness.
The room is locked for update, sharing the same lock as start/join/ready operations.
Response is HTTP 200 with updated settings/members/canStart, and the committed room
state is broadcast through the existing WebSocket event listener.

## Optional timer for solo Wordly

Creation accepts optional `timeEnabled` (defaults true). Solo may use false; multiplayer
must use true. Room settings PUT accepts `timeEnabled` as well: omission preserves its
current value. Only the host of a WAITING room can edit. Initial lives remain 3–6 and
finite word counts at least five. Timer changes reset readiness like other settings.
Response settings include timeEnabled and wordTimeLimitSeconds (180 or null).
V5 allows nullable room/match time limits only for solo Wordly and nullable per-word
deadlines. Match start snapshots the selected timer option. Untimed correct answers
award 100 base points; timed answers award whole seconds remaining (0–180).

## Wordly source and current word

Start now loads vocabulary via authenticated Library POST `/internal/game-content/wordly/random`
with `{"count":10}`. Finite matches require their exact wordLimit; endless starts with up to
10 words and requires at least one. Empty/insufficient sources reject start with 409 and
roll back the room/match/player changes. Network failures return 503.
Active alphabetic words with active Vietnamese meanings are deduplicated by uppercase answer.
Snapshots are persisted in game_match_words in randomized sequence. Every player receives
sequence 1 in game_player_words, with a shared start/deadline (180 seconds or null for untimed solo).
GET `/api/games/matches/{matchId}/words/current` returns only the caller's board ID, meaning,
word number/limit, columns, maxAttempts=columns+1, attemptsUsed, timer, player state and saved guesses.
It never returns answer/source list. Nonparticipants return 404. SOLO with no active assigned word retains 404; multiplayer terminal players return 409 PLAYER_NOT_ACTIVE, or MATCH_FINISHED after match completion.
Existing matches created before word allocation was implemented have no current word; create a new match.
Start pins a shared immutable dictionary version loaded through the internal catalog API.
The randomized initial sequence comes first. Endless play then consumes remaining entries in
a stable shuffled order before cycling through the full dictionary. Library changes or outages after
start do not change an existing match. Catalog loading has a 100,000-word safety limit.

## Wordly hint

POST `/api/games/matches/{matchId}/words/{playerWordId}/hint` with `{"requestId":"<UUID>"}`.
Authentication and ownership are required (401/404); at least one valid saved guess is
required (400). Match/player/word must be active and any deadline must not have passed (409).
One hint is allowed per playerWordId. Same requestId replays the saved hint; a different
request after use returns 409. V6 persists the request, source guess, column, letter and time.
The first position not marked CORRECT in any previous valid row is revealed left to right
(column is zero-based). The latest valid row is returned as sourceGuess/sourceLetters for
the popup. Original guesses, attempts, score, lives and streaks are unchanged.
Current-word GET includes hint (null before use) for reload/reconnect recovery.
Submit a valid guess before using hint. Hints and keyboard states are persisted/reconstructed
for reconnect; keyboard feedback keeps CORRECT over PRESENT over ABSENT.


## SOLO Wordly gameplay

The following describes WORDLY/SOLO behavior. The same endpoints also support multiplayer as described below.
All require authentication. The caller must own the match; other accounts receive 404.

### Submit a guess

POST `/api/games/matches/{matchId}/guesses`:

```json
{"requestId":"<UUID>","playerWordId":"<current word UUID>","guess":"AIR"}
```

Response 200 includes requestId, playerWordId, normalized guess, letters (CORRECT/PRESENT/ABSENT),
keyboard, attemptsUsed, maxAttempts, wordStatus, points {base,bonus,total}, player, nextWord,
matchFinished and endReason. nextWord has the same safe shape as current-word GET, or null.
Neither responses nor private gameplay WebSocket events expose the answer or future word list.

Words are trimmed and uppercased. Valid guesses must use A–Z, match the target length and
exist in the match's dictionary snapshot. Invalid guesses return 400 with INVALID_CHARACTERS,
INVALID_GUESS_LENGTH or WORD_NOT_FOUND, without consuming an attempt; a timed clock continues.
The current Library dataset is the dictionary, not every possible English word.
Letter evaluation handles repeated letters by allocating exact matches first.

A target of N letters gives N columns and N+1 attempts. A wrong row preserves lives/streak.
A correct answer advances immediately. Exhausting all rows or the deadline loses one life,
resets the streak and advances. Each solved word increments the streak; from streak 3 onward,
each solved word earns 50 bonus points. Hint has no scoring or attempt cost.
Finite mode counts solved, failed and timed-out words toward its limit; endless continues until
zero lives or leave. Zero lives takes precedence over reaching the word limit on the same word.

V7 stores a durable receipt using database admission time after obtaining the match SHARE lock, normalized payload and the original
HTTP status/JSON response. Same requestId/payload replays that result; conflicting payload
returns 409 REQUEST_ID_CONFLICT. The receipt is committed before adjudication. Match locking
serializes guesses, leave and timeout; the worker processes pending receipts before expiry.
Admission waiting for a database lock is accepted when that lock is obtained; client clocks
are never trusted. A batch backlog can return 202 REQUEST_PENDING; retry the same requestId.
The 1-second worker also recovers pending receipts after a restart (batch 100, scan 50 matches).
A request received at/after deadline returns 409 WORD_EXPIRED with synchronization state;
a stale word ID returns 409 WORD_NOT_CURRENT, and a finished match returns 409 MATCH_FINISHED.

Untimed base score is 100. Timed base is floor(seconds remaining at accepted receipt time),
clamped to 0–180. `time_score` retains its legacy database name and stores this base score in
both modes; public results call it baseScore. Total player score is BIGINT.

### Leave an active match

POST `/api/games/matches/{matchId}/leave` has no body. It settles prior pending guesses and
any expired word first, preserves earned points, cancels an unfinished word, sets player
FORFEITED and match COMPLETED/PLAYER_LEFT, and closes/soft-deletes the solo room.
Response: matchId, status, endReason, playerStatus, totalScore, resultAvailable.
Repeated calls preserve the original result. Leaving an already naturally finished match
also preserves its endReason. Use the existing room leave endpoint for a waiting room.

### Read the final result

GET `/api/games/matches/{matchId}/result` is read-only; an active match returns
409 MATCH_NOT_FINISHED. Response contains matchId, gameType, playerMode, status, endReason,
settings, player, startedAt, endedAt and durationMs. player includes userId/displayName,
status/livesRemaining, wordsProcessed/wordsSolved/wordsFailed/wordsTimedOut, bestStreak,
baseScore/bonusScore/totalScore and totalSolveTimeMs. Cancelled words do not count as processed.
No unplayed answers are returned. End reasons: WORD_LIMIT_REACHED, NO_LIVES_LEFT, PLAYER_LEFT.

Natural completion returns the room to WAITING and clears readiness; start a new match to
play again. Old match results are retained. The private WORD_STATE/MATCH_FINISHED event is sent
only to the caller's room connection after commit. Multiplayer uses the public ranking events described below.
Matches started before V7 without a dictionary reference/pool return VOCABULARY_SNAPSHOT_MISSING;
create a new match. They can still be left; the timeout worker skips them.

Verification: `mvn -q -pl services/game-service,services/library-service -am test`.
PostgreSQL integration tests cover repeated letters, points, idempotency, concurrent requests,
pending receipt versus timeout, loss of all lives, endless cycles and leave/results.


## Multiplayer Wordly

The existing guesses, hint, match leave and result APIs support MULTIPLAYER. Start snapshots
one shared vocabulary sequence and a reference to an immutable shared dictionary version; each player advances independently.
Current-word and guess responses expose only the caller's board. REST room responses include
activeMatchId; GET match includes playerMode/settings/serverTime and public player progress.
No frontend changes are included in this backend implementation.

POST `/api/games/rooms/{roomId}/members/{userId}/kick` has no body. Only the host of a
WAITING room can kick another active member. Self-kick, non-host and in-match kick are rejected.
The removed member receives PLAYER_KICKED and loses room access; other members receive room state.
Room leave/close cannot bypass the active match lifecycle. Waiting host leave closes and
soft-deletes the room. REST guest leave returns MEMBER_LEFT with the member's name.

GET `/api/games/matches/{matchId}/ranking` is available to match participants, including
ELIMINATED, FINISHED and FORFEITED players. Response fields are matchId, status, playerMode,
isFinal, serverTime and players. Each player contains userId, displayName, slotNumber, rank,
status, livesRemaining, totalScore, totalSolveTimeMs, wordsProcessed, wordsSolved and currentStreak.
Ranking orders score descending, total correct-solve time ascending, then when that score was
achieved ascending. Score achievement uses the accepted guess receipt's receivedAt; exact ties
fall back to joined_at then player id. Ranks are unique. Forfeiting preserves earned score and
ranking eligibility; the last player with lives is not automatically first.

Match leave cancels only the caller's active word and marks them FORFEITED. Remaining players
continue until nobody is ACTIVE/DISCONNECTED, or only one non-forfeited player with lives remains.
A player who finished the finite quota with lives still counts as surviving. Early completion
cancels unfinished words without awarding points. Multiplayer endReason is LAST_PLAYER_STANDING
or ALL_PLAYERS_FINISHED. GET result retains the SOLO fields and adds players in final rank order;
while the match runs it returns 409 MATCH_NOT_FINISHED. Terminal players use ranking while waiting.

If the host remains, natural completion returns the room to WAITING and clears readiness.
If the host left during play, the match continues under the same end conditions, then final
ranking/MATCH_FINISHED is broadcast before the named host ROOM_CLOSED notice and connection closure.
That room is CLOSED and soft-deleted; retained match participants can still retrieve results.

WebSocket WORD_STATE remains private; RANKING_UPDATED and MATCH_FINISHED contain public standings,
never answers, future words, opponent guesses or hints. Events follow transaction commit.
Multiplayer completion uses private WORD_STATE followed by one public MATCH_FINISHED;
it must not also send a private MATCH_FINISHED with a different payload shape.
The last socket disconnect starts a persisted 30-second grace period; another tab prevents false disconnect.
The server sends native WebSocket ping every 10 seconds and detects a silent socket after 30 seconds
without pong. That detection precedes the separate grace period, so network loss to forfeiture can
exceed 30 seconds (heartbeat and expiry worker scheduling can add delay).
Timers continue during grace. Reconnect preserves board, score and deadline. Expiry forfeits the
player and cannot be undone by GET or reconnect. Restart recovery treats missing live connections
as disconnected and lets persisted grace deadlines expire rather than leaving players stuck.

Receipt admission is durable database acceptance after acquiring the match SHARE lock, not
browser send time or HTTP arrival. Admission at/after the word deadline is expired. Pending
admitted receipts settle before timeout; retry keeps the original response and scoring time.

V8 adds score_reached_at, player_display_name and player_slot_number snapshots to
 game_match_players, plus a unique partial index permitting only one active Wordly match per room.
Historical rows are backfilled; no applied V1–V7 migration is changed. Match snapshots preserve
names and slots even after membership changes or room closure.

### Verification

Run `mvn -pl services/game-service,services/library-service -am verify` for the
service regression suites. PostgreSQL integration tests require Docker; check the
reported skipped count before treating a run as database validation.

Realtime validation should cover multiple tabs per player, reconnect within the
30-second grace period, heartbeat silence, process restart, concurrent/retried
answers, word expiry, and exactly one public `MATCH_FINISHED` event. Build and
restart the deployed service to apply changes; source verification alone does not
update an already-running process.

Clients must wait for the server `CONNECTED` acknowledgment before expecting
subsequent room events. Native WebSocket `onopen` can precede server-side room
registration. The registration authorization check protects revoked membership.

## Shared dictionary and start transaction

V9 adds content-versioned shared dictionaries. Identical catalogs are stored once, not per match.
Matches pin a version; vocabulary edits affect new matches only. Existing V7/V8 pools remain
readable and no applied migration is rewritten. New matches insert no per-match pool rows.
Game loads and validates Library vocabulary before opening the room transaction; it then locks
the room, checks settings/readiness again and writes the match atomically. A concurrent settings
change rejects start and can be retried. No remote Library request holds the room row lock.
The accepted-guess dictionary is still the Library catalog: the starter 100 words are not a full
English dictionary. Adding a broader curated lexicon is a separate data-import decision.
Hint is intentionally free in SOLO and MULTIPLAYER, once per word; it does not reduce score,
lives, attempts or streak. Start, gameplay and hint time decisions use an injected UTC Clock.
Swagger already selects `/api/games/**` in Config Server; no path filter change is required.
