# Wordly Game API and WebSocket v1

Updated: 2026-10-22. Source of truth: Game controllers, response DTOs and GameWebSocketHandler.
Client routes use the Gateway; `/internal/**` is never a browser route. UUIDs and timestamps
below are schematic; timestamps use ISO-8601 UTC. REST requires `Authorization: Bearer <access-token>`.
Gateway validates it and forwards an internal JWT. Identity always comes from the verified token.

## REST routes

| Method and route | Request | Response / access |
| --- | --- | --- |
| GET /api/games/catalog | none | 200 `{games:[{gameType,name,description,status}]}`; WORDLY and ROCKET AVAILABLE |
| GET /api/games/me/active-game | none | 200 ActiveGame, or 204 when the caller belongs to no room in any room game |
| POST /api/games/me/active-game/leave | none | 204; leaves the caller's active room (see One room per user) |
| GET /api/games/wordly/me/active-game, POST …/leave | none | Aliases of the two routes above, kept for existing clients; same cross-game result |
| POST /api/games/wordly/rooms | `{playerMode,initialLives,playMode,wordLimit,timeEnabled}` | 201 Room; caller becomes host; 409 ACTIVE_GAME_EXISTS while in another room |
| GET /api/games/rooms/{roomId} | none | 200 Room; active member |
| GET /api/games/rooms/code/{roomCode} | none | 200 Room; active member, not a public lobby lookup |
| POST /api/games/rooms/{roomId}/join | none | 200 Room; MULTIPLAYER WAITING with free slot; 409 ACTIVE_GAME_EXISTS for a room game while in another room |
| POST /api/games/rooms/code/{roomCode}/join | none | same join behavior |
| PUT /api/games/rooms/{roomId}/settings | `{initialLives,wordLimit,timeEnabled}` | 200 Room; WAITING host only; resets readiness, preserves members |
| POST /api/games/rooms/{roomId}/ready | `{ready:true}` | 200 Room; active member in WAITING |
| POST /api/games/rooms/{roomId}/members/{userId}/kick | none | 200 Room; WAITING host, cannot kick self |
| POST /api/games/rooms/{roomId}/leave | none | 200 Room; waiting-room lifecycle, use match leave during game |
| POST /api/games/rooms/{roomId}/close | none | 200 Room; waiting host only |
| POST /api/games/rooms/{roomId}/start | none | 200 Match; host, all ready, minimum 1 SOLO / 2 MULTIPLAYER |
| POST /api/games/rooms/{roomId}/websocket-ticket | none | 200 `{ticket,expiresAt,subprotocols}`; active room member |
| GET /api/games/matches/{matchId} | none | 200 Match; match participant |
| GET /api/games/matches/{matchId}/words/current | none | 200 CurrentWord; caller's board only; no active board returns 409 in multiplayer or 404 in SOLO |
| POST /api/games/matches/{matchId}/guesses | `{requestId,playerWordId,guess}` | 200 GuessResult, 202 pending, or error |
| POST /api/games/matches/{matchId}/words/{playerWordId}/hint | `{requestId}` | 200 Hint; caller's active word |
| POST /api/games/matches/{matchId}/leave | none | 200 `{matchId,status,endReason,playerStatus,totalScore,resultAvailable}` |
| GET /api/games/matches/{matchId}/ranking | none | 200 Ranking; includes terminal/forfeited match participants |
| GET /api/games/matches/{matchId}/result | none | 200 Result after completion; 409 MATCH_NOT_FINISHED before completion |

Legacy `/api/games/rooms` creation and `/api/games/sessions/**` remain unchanged; they are not
Wordly entry points. Swagger selection already includes `/api/games/**` in
`infra/config-server/config-repo/game-service.yaml`; `/v3/api-docs` is the runtime schema.

## One room per user

A user belongs to at most one room across every room game (WORDLY and ROCKET; legacy session games such as
WORD_MEANING_MATCH do not count). Being in a Wordly room blocks creating or joining a Rocket room and the reverse.
ActiveGame fields: roomId, roomCode, playerMode, roomStatus (WAITING or IN_MATCH), matchId (running match or null),
playerStatus (caller's match status or null), gameType (WORDLY or ROCKET; clients route to that game's lobby or play
page). Active means an active room member (JOINED, READY, DISCONNECTED) of a MULTIPLAYER room that is WAITING or
IN_MATCH, or of a SOLO room with an IN_PROGRESS match; a running match is preferred. Unstarted or finished SOLO rooms of
any room game are not active: creating or joining closes them. Room create and join take a per-user transaction lock,
then return 409 ACTIVE_GAME_EXISTS when an active room remains. Clients offer to rejoin (match play page when matchId is
set, otherwise the lobby) or to leave and continue. Leave uses the waiting-room lifecycle while WAITING, forfeits an
ACTIVE or DISCONNECTED player like match leave, and only removes FINISHED or ELIMINATED spectators from the room without
changing their match status. A running Rocket match is forfeited as in the Rocket contract; a game without a match
flow would answer 409 GAME_NOT_SUPPORTED. Leaving without a room is a no-op.

## Settings, lobby and match models

Settings: initialLives integer 3-6; playMode FINITE or ENDLESS. FINITE requires wordLimit >=5;
ENDLESS requires null wordLimit. playerMode SOLO or MULTIPLAYER is fixed at creation.
MULTIPLAYER always has timeEnabled=true and wordTimeLimitSeconds=180. SOLO can disable time;
then wordTimeLimitSeconds and word deadlines are null. Updates cannot change playMode/playerMode.
Room `settings` also carries `questionCount` and `secondsPerQuestion`, which are null for Wordly rooms and set for
Rocket rooms (see the Rocket contract).
Room codes contain six uppercase alphanumeric characters, capacity 1 SOLO / 4 MULTIPLAYER.

Room fields: id, roomCode, hostUserId, gameType, learningDomain, mode, maxPlayers, status,
createdAt, members, items, playerMode, settings, activeMatchId, canStart, message.
Wordly has no public answer-bearing items. Members contain userId, role, status, joinedAt,
displayName, slotNumber; names come from User Service, never from a client request.
Room statuses: WAITING, IN_MATCH, CLOSED. canStart requires WAITING and all active members READY.

Match fields: matchId, roomId, sessionId (null for Wordly), status, startedAt, players,
playerMode, settings, ownPlayer, ranking.
Public player progress does not contain answers, opponent boards or hints.

## Current word and guesses

CurrentWord fields: playerWordId, wordNumber, wordLimit, meaning, columns, maxAttempts,
attemptsUsed, timeEnabled, deadlineAt, wordStatus, player, guesses, keyboard, hint.
`columns = answer length`, `maxAttempts = columns + 1`. Only submitted guesses are exposed;
answer and future vocabulary sequence are never sent to clients.
player contains status, livesRemaining, totalScore, currentStreak, bestStreak.
guesses is `[{attemptNumber,guess,letters}]`; letters uses one result per character:
CORRECT (green, correct position), PRESENT (yellow, other position), ABSENT (gray).
Repeated characters consume answer occurrences after exact matches. Keyboard retains the
strongest evidence CORRECT > PRESENT > ABSENT, including the revealed hint letter.

GuessResult: requestId, playerWordId, guess, letters, keyboard, attemptsUsed, maxAttempts,
wordStatus, points `{base,bonus,total}`, player, nextWord, matchFinished, endReason.
wordStatus is ACTIVE, SOLVED or FAILED. nextWord is caller-only CurrentWord or null.
Timeout updates use TIMED_OUT. CANCELLED words do not count as processed.

requestId is required and globally unique. Retrying identical input with the same requestId
replays the stored result; changed payload returns 409 REQUEST_ID_CONFLICT. Accepted time is
the durable database receipt time after match admission lock, not browser send time.
Receipts accepted at or after deadline return WORD_EXPIRED. Pending receipts settle before timeout.
A valid but wrong row consumes one attempt. Unknown words, invalid length/characters consume
no attempt or life. Exhausting rows or timeout loses exactly one life and breaks the streak.
Each player advances independently along one common server-side word sequence.

Timed base points = floor(remaining seconds), clamped 0-180. Untimed SOLO base =100 per solved
word. Solving three consecutive words without losing a life starts +50 bonus per solved word,
including the third. Wrong rows alone do not break the streak.

Hint is intentionally free: no score, attempt, life or streak penalty in either mode.
One hint per word, after at least one valid guess. Same requestId replays the same hint;
a different requestId after use is rejected. Source is the latest valid guess. Reveal the
leftmost position not already CORRECT in any submitted row. Hint fields: playerWordId,
sourceGuess, sourceLetters, revealedCell `{column,letter,status:"CORRECT"}`, hintUsed.
Column indexes are zero-based.

## Ranking, leaving and results

Ranking fields: matchId, status, playerMode, isFinal, serverTime, players.
Each player: userId, displayName, slotNumber, rank, status, livesRemaining, totalScore,
totalSolveTimeMs, wordsProcessed, wordsSolved, currentStreak, reconnectDeadlineAt.
reconnectDeadlineAt is set only for a DISCONNECTED player of an IN_PROGRESS match; clients show
the remaining grace as a countdown. Match result players never carry it.
Ordering: totalScore DESC, totalSolveTimeMs ASC, score_reached_at ASC, joined_at ASC, player ID.
Ranks are unique. Status: ACTIVE, DISCONNECTED, FINISHED, ELIMINATED, FORFEITED.
Leaving preserves points and ranking entry, marks FORFEITED, cancels the caller's current word.
Host leaving mid-match does not stop other players. Multiplayer finishes when nobody can
continue or only one non-forfeited player with lives remains; a quota-finisher still counts
as surviving. End reason: ALL_PLAYERS_FINISHED or LAST_PLAYER_STANDING.
SOLO end reasons: WORD_LIMIT_REACHED, NO_LIVES_LEFT, PLAYER_LEFT.

Result: matchId, gameType, playerMode, status, endReason, settings, player, startedAt,
endedAt, durationMs; MULTIPLAYER also adds players in final order. The player result includes
userId/displayName, status/livesRemaining, wordsProcessed/wordsSolved/wordsFailed/wordsTimedOut,
bestStreak, baseScore/bonusScore/totalScore, totalSolveTimeMs. No future answers are exposed.
If host remains, completion returns room to WAITING and clears readiness. If host left,
publish final ranking first, then `Chủ phòng [name] đã đóng phòng`, close and soft-delete room.
Retained participants can still read final results.

## Native WebSocket

Get a fresh ticket via REST; connect `/ws/games` using the returned subprotocols
`["game.v1","ticket.<ticket>"]`. Ticket is single use; never put JWTs in the URL.
Wait for CONNECTED before depending on room broadcasts. Tickets are required on every reconnect.

| Client message | Fields / effect |
| --- | --- |
| PING | `{type:"PING"}` -> PONG |
| ROOM_STATE | `{type:"ROOM_STATE"}` -> RoomStateMessage |
| READY | `{type:"READY",ready:true}`; same waiting-room rules as REST |
| START_MATCH | `{type:"START_MATCH"}`; same authorization/readiness as REST |
| RECONNECT | `{type:"RECONNECT",matchId}` -> MATCH_STATE and RANKING_UPDATED |
| LEAVE | `{type:"LEAVE"}`; delegates to match leave while active, room leave otherwise |
| ANSWER | Legacy session-only `{type:"ANSWER",matchId,sessionId,itemSequence,responsePayload:{answer},durationMilliseconds}`; Wordly rejects it, use REST guesses |

| Server message | Payload / audience |
| --- | --- |
| CONNECTED | `{type,roomId,userId}`; connected socket |
| PONG | `{type:"PONG"}` |
| ROOM_STATE | `{type,room:Room}`; active room members |
| MATCH_STARTED / MATCH_STATE | `{type,match:Match}`; personalized state per participant |
| WORD_STATE | `{type,matchId,player,currentWord,endReason}`; caller only |
| RANKING_UPDATED | `{type,matchId?,ranking:Ranking}`; public progress for participants |
| MATCH_FINISHED | MULTIPLAYER `{type,matchId,ranking}`; final public ranking, once per completion; SOLO private state shape |
| MEMBER_LEFT | match leave includes matchId,userId,displayName,message; waiting-room WS LEAVE can include userId only |
| PLAYER_KICKED | `{type,userId,message}`; kicked socket, then close |
| ROOM_CLOSED | `{type,roomId,message}`; final notice then close |
| ERROR | `{type,message}`; invalid/unauthorized/unsupported operation |
| ANSWER_ACCEPTED | Legacy session answer event; not emitted for Wordly guesses |

State changes broadcast only after transaction commit. Events have no durable sequence/replay
contract; after reconnect fetch REST state and ranking. Never overwrite newer local state
with stale parallel responses. Private WORD_STATE is not broadcast to opponents.
Ping frames every 10s detect missing pong after 30s; losing the last socket starts a separate
persisted 30s reconnect grace. Another live tab prevents false disconnect. Timers keep running;
expired grace forfeits the player. Detection and worker scheduling add delay to observed timeout.

## Errors and persistence

Error body uses service ErrorResponse: timestamp, status, error, code when provided, message,
path, details. Common cases: 400 invalid settings/full room/invalid word, 401 missing or invalid
identity, 403 forbidden host operation, 404 inaccessible/missing room or match, 409 wrong state,
MATCH_NOT_FINISHED, WORD_NOT_CURRENT, WORD_EXPIRED, REQUEST_ID_CONFLICT, PLAYER_NOT_ACTIVE,
ACTIVE_GAME_EXISTS.
202 REQUEST_PENDING means retry the same requestId. Do not depend on English message strings.

Library random endpoint enforces count 1-100 in DTO and use case. Catalog pages enforce limit
1-500. Game loads catalog without holding a room transaction, deduplicates/validates it, samples
the initial sequence, and persists one shared immutable dictionary version per distinct catalog.
A SHA-256 content version covers answer, source ID and meaning; active matches pin it.
Dictionary positions use a deterministic shuffle seeded from that version, preserving random
word order without writing a dictionary-sized permutation for each match.
V9 adds game_wordly_dictionaries, game_wordly_dictionary_words and match dictionary version/initial-count fields.
Dictionary rows are immutable references retained for match replay; legacy pools are retained
for older matches. New matches insert no game_match_word_pool rows. Endless progression uses
the pinned dictionary, excluding initial answers until the first cycle is exhausted, then cycles
entries in the version's stable shuffled order; shared per-match sequence rows are stored only as players reach them.

The accepted-guess dictionary currently contains the Library catalog, not a comprehensive
English lexicon. A 100-word starter catalog accepts those entries only. Sharing versions solves
storage duplication; expanding accepted English words requires a curated dictionary source/import.
No fake dictionary or external live validation is added. Hint/start/gameplay time decisions use
an injected UTC Clock; receipt time remains database-authoritative.
