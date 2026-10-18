# Gamification v1

Learning Service keeps the learner's XP and level, hearts, streak, daily quests, badges and weekly league. Points live in
Access Service; Learning owes points to a learner for streaks, quests and badges and sends them over RabbitMQ (see
[Points rewards](#points-rewards)). FE: the dashboard (level, hearts, streak, quests), the badges screen and the league tab.

All routes are under `/api/learning`, role `CUSTOMER` unless stated, and act on the verified caller only. Errors use the
body `{ "detail": string, "code": string }`.

## XP and levels

- A correct first answer earns XP by its difficulty tag: `EASY` 10, `MEDIUM` 15, `HARD` 20, untagged 10. Only the first
  submission of a block or set earns XP; a retake earns none.
- Answer XP counted per learner day is capped (default 100). The part over the cap is stored as shadow XP: it is kept for
  history but is not counted toward the total, the level or the league.
- Milestone XP is not capped: a knowledge point reaching consolidate (+25) and maintain (+50), once per point and only for
  practice evidence; streak milestones; completed daily quests.
- The level is `1 + totalXp / levelXp` (default 1000 XP per level). Reaching a new level refills the hearts for free.
- The day is the learner's local day. `GET /me/settings` returns `{ "timezone": "Asia/Ho_Chi_Minh" }` (the default until
  chosen); `PUT /me/settings` with `{ "timezone": "<IANA region id>" }` saves it. An offset or abbreviation such as `UTC+3`
  is `400 INVALID_TIMEZONE`.

### `GET /me/progress`

```json
{ "totalXp": 1200, "level": 2, "nextLevelAt": 2000, "xpToNextLevel": 800, "xpToday": 60, "dailyCap": 100 }
```

### The `gamification` block in submissions

The responses of a lesson exercise submission, a Practice attempt submission (including free practice) and a review set
submission carry a `gamification` object with what that submission did:

```json
{ "xpEarned": 40, "xpCapped": false, "totalXp": 1240, "level": 2, "leveledUp": false,
  "heartsLost": 1, "heartsRemaining": 4, "nextHeartAt": "2026-10-18T05:00:00Z" }
```

`xpCapped` is true when an answer went past the daily cap. `heartsLost`, `heartsRemaining` and `nextHeartAt` are `null` for
a submission that costs no hearts (a review set). A replayed `requestId` returns the original response, so a repeat earns
and costs nothing more.

## Hearts

A wrong objective answer costs one heart in a lesson exercise, a lesson Practice set and free practice. Review sets, quick
checks, diagnostic probes, practice tests, topic and course tests, placement and essays cost none. The standard maximum is 5
(PREMIUM 7). One heart comes back every 5 hours while below the maximum; the clock stops while hearts are full. A learner
may finish with zero hearts; the next start or submission of a heart-costing kind is refused until a heart returns.

### `GET /me/hearts`

```json
{ "hearts": 3, "maxHearts": 5, "nextHeartAt": "2026-10-18T05:00:00Z", "refillCost": 5 }
```

`nextHeartAt` is `null` when the hearts are full.

### `POST /me/hearts/refill`

Request `{ "requestId": "<uuid>" }`. Fills the hearts for `refillCost` points, taken from Access under `requestId` as its
idempotency key: a repeat of the same request costs nothing more. Response: the same body as `GET /me/hearts`.

| HTTP | Code | When |
| --- | --- | --- |
| 409 | `HEARTS_FULL` | Nothing to refill |
| 402 | `INSUFFICIENT_POINTS` | The balance cannot cover `refillCost` |
| 503 | `ACCESS_UNAVAILABLE` | Access could not be reached; nothing was charged |

### `403 HEARTS_DEPLETED`

Starting or submitting a lesson exercise, a Practice attempt or free practice with no hearts left returns
`{ "detail": "…", "code": "HEARTS_DEPLETED", "nextHeartAt": "…", "refillCost": 5 }`. Reviews can still be submitted.

## Streak

The streak is counted on the server from submitted answers, in the learner's local day. A day with an answer continues it.
An empty day is covered by a freeze if one is owned, otherwise the streak breaks and the lost length is kept for display.
A graded essay (lesson or Practice) counts as an answered day like any other answer: it writes a question event. The old client-reported streak in User Service is
unchanged and is not used for rewards.

### `GET /me/streak`

```json
{ "currentDays": 6, "longestDays": 16, "freezeAvailable": false, "nextFreezeCost": 10, "activeToday": true,
  "lostStreakDays": 0 }
```

After a break, `currentDays` is 0 and `lostStreakDays` is the length that was lost (for "streak lost, was 16 days").

### `POST /me/streak/freeze`

Request `{ "requestId": "<uuid>" }`. Buys a freeze with points. A freeze covers the next empty day from the day it is bought:
today if today is still empty when it is judged, otherwise the first empty day after. At most one freeze is owned. The price
is 10 points times the consecutive frozen days counting this one (10, 20, 30, 40, 50); a sixth consecutive freeze is refused.
Response: the same body as `GET /me/streak`.

| HTTP | Code | When |
| --- | --- | --- |
| 409 | `FREEZE_ALREADY_OWNED` | A freeze is already owned |
| 409 | `NO_STREAK_TO_PROTECT` | The current streak is 0 |
| 409 | `FREEZE_LIMIT` | More than 5 consecutive frozen days |
| 402 | `INSUFFICIENT_POINTS` | The balance cannot cover `nextFreezeCost` |
| 503 | `ACCESS_UNAVAILABLE` | Access could not be reached; nothing was charged |

### Streak rewards

Each multiple of 7 days pays +100 XP and +10 points; day 30 pays +500 XP (no points). A milestone pays once per streak run.

## Daily quests

### `GET /me/quests`

Three quests per local day, made on first use of the day:

| Type | Done when | Reward |
| --- | --- | --- |
| `PRACTICE` | 5 objective answers (lesson, Practice, free practice, review) | +20 XP, +5 points |
| `WEAKNESS` | 3 answers on the target point in free practice or review | +30 XP |
| `PRACTICE_PLUS` | 10 objective answers; replaces `WEAKNESS` when no point suits | +30 XP |
| `STREAK` | any new answer today | +10 XP |

```json
{ "date": "2026-10-18", "completed": 1,
  "quests": [
    { "type": "PRACTICE", "required": 5, "progress": 5, "completed": true, "xpReward": 20, "pointsReward": 5,
      "target": null },
    { "type": "WEAKNESS", "required": 3, "progress": 1, "completed": false, "xpReward": 30, "pointsReward": 0,
      "target": { "knowledgePointId": "…", "code": "R60_TFNG_F_VS_NG", "name": "…" } },
    { "type": "STREAK", "required": 1, "progress": 0, "completed": false, "xpReward": 10, "pointsReward": 0,
      "target": null } ] }
```

Only the first submission of a question counts, and only judged answers (a graded essay is one); exams and diagnostic probes
do not. The weakness quest counts free practice and review answers on the target point only. The target is a `PRIORITY` or `CONSOLIDATE` point with a practice set or a blocking review, a blocked one first, then the
lowest mastery. A reward is paid once, when the quest completes; there is no claim endpoint.

## Badges

### `GET /me/badges`

The active badges and any inactive badge the learner already holds, ordered by code:

```json
[{ "code": "STREAK_7", "name": "Chuỗi 7 ngày", "description": "…", "rewardPoints": 5, "awarded": true,
   "awardedAt": "2026-10-17T14:00:00Z", "progress": { "current": 7, "target": 7 } },
 { "code": "COURSE_PASSED", "name": "…", "description": "…", "rewardPoints": 20, "awarded": false, "awardedAt": null,
   "progress": null }]
```

`progress` is present for counted badges and `null` for the others. Catalog: `STREAK_7`, `STREAK_30`, `STREAK_100`,
`KP_MAINTAIN_10`, `KP_MAINTAIN_50`, `FIRST_TOPIC_TEST`, `COURSE_PASSED`, `LEVEL_10`, `QUESTS_30`. A badge is awarded once and
never taken back; its points are paid when it is first awarded.

### `PATCH /admin/badges/{code}`

Role `ADMIN`. Request `{ "active": false }`. A switched-off badge is no longer awarded; those already held are kept. Response
`{ code, name, description, rewardPoints, active }`. `404 BADGE_NOT_FOUND` for an unknown code.

## Weekly league

A league week runs from Sunday 00:00 UTC to the next Sunday 00:00 UTC. A learner with counted XP in the week is placed in a
group of up to 30 learners of the same tier (Bronze, Silver, Gold, Sapphire, Diamond, Ruby, Amethyst) and ranked by weekly
XP; on a tie, whoever reached that XP first ranks higher. When the week closes the top 7 move up a tier and, in a group of
at least 23, ranks 23 and below move down; the others stay. A group of 7 or fewer is promoted whole, and a group under 23
demotes nobody. Amethyst stays at the top and Bronze at the bottom. A learner with no XP in the week is in no group and keeps
their tier.

### `GET /me/league`

```json
{ "tier": "SILVER", "weekStart": "2026-10-18", "closesAt": "2026-10-25T00:00:00Z", "joined": true,
  "table": [{ "rank": 1, "displayName": "Mina", "weeklyXp": 340, "you": false, "zone": "PROMOTION" },
            { "rank": 2, "displayName": "Lan", "weeklyXp": 280, "you": true, "zone": "PROMOTION" }],
  "groupSize": 30, "promoteTop": 7, "demoteFrom": 23 }
```

`zone` is `PROMOTION`, `SAFE` or `DEMOTION`. The table carries display names only: no email and no id of another learner.
`displayName` is `null` if User Service could not give it. `joined` is false and `table` is empty until the learner earns XP
this week.

### `GET /me/league/history?weeks=8`

`weeks` 1 to 26 (default 8), else `400 INVALID_WEEKS`. Closed weeks, newest first:
`[{ "weekStart": "2026-10-11", "tier": "SILVER", "weeklyXp": 340, "finalRank": 2, "outcome": "PROMOTED" }]`.

## Points rewards

Mastered knowledge points, streak milestones, completed quests and badges owe the learner points. The amount is queued as a
`PENDING` grant in the same transaction as the thing that earned it. A relay sends each grant to Access on exchange
`learning.points` (routing key `points.grant.requested`, payload `{ grantId, userId, amount, reason, referenceId }`) and
Access answers on `points.grant.settled` with `{ grantId, status: GRANTED | CAPPED | FAILED, error }`. `reason` is one of
`STREAK_MILESTONE`, `KP_MASTERED` (+5 points the first time a point reaches 90% mastery through practice; `referenceId` is
the point id), `DAILY_QUEST` or `BADGE`. Access accepts every value of `LearningRewardReason` and credits a grant id once,
so a resend is safe; a grant with no answer is sent again after 5 minutes and given up after 10 sends. Points Access caps per
day are reported as `CAPPED`. The agreed message shape is contract C6 in
`plans/261017-1931-mastery-practice-and-progress/reports/work-split.md`; Access HTTP credit and consumer behavior are in
the [Access points contract](access-points-v1.md).

## Configuration

`learning.gamification.default-timezone`, `learning.gamification.xp.*`, `learning.gamification.hearts.*`,
`learning.gamification.quests.*`, `learning.rewards.*`, `learning.league.close-cron`, `learning.league.close-enabled`.
