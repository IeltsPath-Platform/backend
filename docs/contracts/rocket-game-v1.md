# Rocket Game API v1

Updated: 2026-10-22. Source of truth: `RocketGameController`, `RocketGameplayResults` and the shared room routes in
[Wordly Game v1](./wordly-game-v1.md). Client routes use the Gateway with `Authorization: Bearer <access-token>`;
identity always comes from the verified token. SOLO play is complete. MULTIPLAYER rooms can be created, joined,
edited and started, and their rounds are played through the same REST routes; round events over WebSocket and leaving a
multiplayer match are not available yet (see the end of this document).

## Flow

1. `POST /api/games/rocket/rooms` `{playerMode, questionCount, initialLives?, secondsPerQuestion?}` → 201 Room
   (`gameType: ROCKET`). SOLO: `questionCount` 3–20, always 4 lives and 15 seconds (the other two fields are ignored).
   MULTIPLAYER: `questionCount` 5–20, `initialLives` 3–6 (default 4), `secondsPerQuestion` 10, 15 or 20 (default 15),
   up to 4 players; anything else is 400. Others join with the shared room routes (`/rooms/code/{code}/join`, …). 409 `ACTIVE_GAME_EXISTS` while the user is in any other room
   (Wordly or Rocket, see "One room per user" in the Wordly contract).
2. `POST /api/games/rooms/{roomId}/ready` `{ready:true}` then `POST /api/games/rooms/{roomId}/start` → 200 Match
   (`matchId`, `status`, `players`; `sessionId` is null). Starting reads the vocabulary catalog from Library, creates
   every question and opens the first one.
3. `GET /api/games/rocket/matches/{matchId}/rounds/current` → 200 Current.
4. `POST /api/games/rocket/matches/{matchId}/rounds/{roundId}/answers` `{requestId, optionId}` → 200 AnswerAck.
5. Repeat 3–4 until `matchFinished`. `GET /api/games/me/active-game` reports the running match with `gameType: ROCKET`.
6. `GET /api/games/matches/{matchId}/result` → 200 Result once the match has ended; 409 `MATCH_NOT_FINISHED` before.
7. To quit early: `POST /api/games/matches/{matchId}/leave` → 200 Left, or `POST /api/games/me/active-game/leave` → 204.
   The shared match routes hand Rocket matches to the Rocket flow; Wordly matches keep their own behaviour.
8. `GET /api/games/matches/{matchId}/ranking` → 200 Ranking, the same shape as Wordly's (see the Wordly contract);
   404 for a match the caller is not in. `GET /api/games/matches/{matchId}` also carries `playerMode`, `settings`,
   `ownPlayer` and `ranking` for Rocket matches.

## WebSocket

Players connected to the room socket receive `MATCH_STARTED` (shape as in the Wordly contract) after the start
commits, so a lobby can move into the match. Then, to match players only, after each commit:
- `ROCKET_ROUND_STARTED` `{matchId, round {roundId, number, total, meaning, options, opensAt, deadlineAt}, serverTime}`;
  never the answer.
- `ROCKET_PLAYER_ANSWERED` `{matchId, roundId, userId, answeredCount, aliveCount}` while the round stays open; not the choice.
- `ROCKET_ROUND_RESULT` `{matchId, roundId, correctOptionId, answers [{userId, optionId|null, correct, points,
  livesRemaining, totalScore, status}], ranking, nextRoundAt, matchFinished, endReason}`, also when the worker closes a
  round at its deadline. The last one is followed by `MATCH_FINISHED` (final ranking) and `ROOM_STATE`.
Current stays the way to resync after a reconnect.

## Models

- Room `settings` for a Rocket room: `initialLives`, `questionCount`, `secondsPerQuestion`, `timeEnabled: true`;
  `wordTimeLimitSeconds`, `playMode` and `wordLimit` are null (they belong to Wordly, whose rooms carry
  `questionCount: null` and `secondsPerQuestion: null`). The host edits them while waiting with
  `PUT /api/games/rooms/{roomId}/settings` `{initialLives, questionCount?, secondsPerQuestion?}` (null keeps a value);
  a change sets every READY member back to JOINED and is pushed as ROOM_STATE.

- Current: `matchId`, `settings {questionCount, initialLives, secondsPerQuestion}`, `round` (null once the match ends),
  `lastRoundResult` (latest closed round, or null), `matchFinished`, `serverTime`.
- Round: `roundId`, `number`, `total`, `meaning`, `options [{optionId, word}]` (three, lower case, shuffled),
  `opensAt`, `deadlineAt`, `serverTime`, `me {answered, optionId}`, `player {livesRemaining, totalScore, status}`.
  A round waiting out the reveal pause has `opensAt` in the future and `deadlineAt` null; fetch again at `opensAt`.
  Clients compute the countdown from `deadlineAt` corrected by `serverTime`. A round never carries its answer.
- RoundResult (the caller's view of a closed round): `roundId`, `correctOptionId`, `chosenOptionId` (null when time ran
  out), `correct`, `points`, `livesRemaining`, `totalScore`, `status`, `nextRoundAt`, `matchFinished`, `endReason`.
  `nextRoundAt` is when the next round is scheduled (close time plus the reveal pause), null when none follows; it stays
  the same after that round opens, even if it opened a little later.
- AnswerAck: `requestId`, `roundId`, `optionId`, `receivedAt`, `roundResult` (present when the answer closed the round,
  which is always the case in solo play).
- Result: `matchId`, `endReason`, `settings`, `totalRounds`, `answered` (rounds the caller took part in, misses
  included), `correctCount`, `livesRemaining`, `totalScore`, `playerStatus`, `durationMs`, `startedAt`, `endedAt`.
- Left: `matchId`, `status`, `endReason`, `playerStatus`, `totalScore`, `resultAvailable`.

## Rules

- A correct answer scores 100 plus `floor(100 × time left / time limit)`, using the time the database received it.
  A wrong answer or no answer before the deadline scores 0 and costs one life.
- The next round opens 2.5 s after a solo round closes. When a deadline passes, the round closes at the deadline even if
  nobody asks; a background worker (`game.rocket.round-scan-ms`, default 500 ms) and every request settle due rounds.
- The match ends with `ALL_ROUNDS_COMPLETED` after the last round or `NO_LIVES_LEFT` when lives reach zero (checked
  first). Player status becomes FINISHED or ELIMINATED; the room returns to WAITING.
- Leaving forfeits the player (FORFEITED) and keeps points already won; the open round closes as `MATCH_ENDED` without
  costing a life. A solo match ends with `PLAYER_LEFT` and its room is closed. Leaving again, or after the match has
  ended, returns the stored outcome without changing the end reason.
- Ranking: higher `totalScore` first, then less time spent on correct answers, then who joined the match first. In
  the Ranking model `wordsProcessed` is rounds played (misses included), `wordsSolved` is correct answers,
  `totalSolveTimeMs` is the time spent on correct answers and `currentStreak` is always 0. `rank` is live while the
  match runs and fixed when it ends (`isFinal: true`).
- Wrong options never share the answer's word or meaning and favour words of similar length.

## Errors

ErrorResponse with `code`: 400 `INVALID_OPTION`, validation errors, `MULTIPLAYER_NOT_AVAILABLE`; 401 without identity;
404 for a match the caller is not in, or `ROUND_NOT_FOUND`; 409 `ROUND_CLOSED` (at or after the deadline, or the round
is not open), `ALREADY_ANSWERED`, `REQUEST_ID_CONFLICT`, `MATCH_FINISHED`, `MATCH_NOT_FINISHED`, `PLAYER_NOT_ACTIVE`,
`ACTIVE_GAME_EXISTS`.
Retrying with the same `requestId` and option returns the stored answer instead of answering twice.
A `requestId` belongs to one answer for the whole match: reusing it on another round, or with another option, is
409 `REQUEST_ID_CONFLICT`.

## Not yet available

Leaving a running multiplayer match (409
`GAME_NOT_SUPPORTED`; the match still ends once everyone runs out of lives or rounds) and reconnect grace.
