# Acceptance: gamification (plan 261017-1932)

Updated 2026-10-18. Demos 1–6 ran against a real Compose stack (gateway, user, content, assessment, access, learning,
PostgreSQL, RabbitMQ) with real HTTP calls through the gateway; the observed results are in the table below. Docker's disk
image was moved from C: to E: first (the earlier attempt failed for lack of space on C:).

## Verification run so far

| Check | Result |
|---|---|
| Access suite (`LearningPointsGrantListener`, all four reasons, and invalid HTTP reason) | 65 tests, 0 failures, 0 errors, 0 skipped; PostgreSQL 15 Testcontainers ran (2026-10-18) |
| `mvn -B -ntp verify` whole reactor | BUILD SUCCESS, 24 min 27 s, 1,751 tests, 0 failures, 0 errors, 0 skipped; Testcontainers ran (2026-10-18, code changes later committed without further source edits). Per module: common-security 8, api-gateway 9, config-server 1, eureka-server 1, user-service 115, access-service 65, content-service 421, library-service 67, learning-service 714, assessment-service 309, game-service 15, notification-service 1, community-service 25 |
| `docker compose config --quiet` | exit 0 (2026-10-18) |
| First Compose attempt | `docker compose --parallel 1 up -d --build` failed: BuildKit `rpc error ... EOF`, C: had no free space. Fixed by moving Docker's data disk to E: (C: went from 2.6 GB to 28.2 GB free) |
| Compose stack (2026-10-18, after the move) | Build and start succeeded. `access-service` was `unhealthy`: it had no RabbitMQ settings in `access-service.yaml` or in `docker-compose.yml`, so its points listener connected to `localhost` and failed. Added the `rabbitmq` block (host, port, user, password from env) and the `RABBITMQ_*` variables plus `depends_on: rabbitmq` for `access-service`. After that every service is `healthy` and all six services are `UP` in Eureka. No test caught this: the Access tests use a Testcontainers broker whose address they inject |
| Learning ↔ broker (`PointsGrantBrokerRoundTripTest`, RabbitMQ 3.13 container) | 2 tests pass: the relay publishes the agreed JSON on `learning.points` / `points.grant.requested`, and an answer in Access's shape (`GRANTED`, `FAILED` with error, plus an unreadable message dropped) settles the grant. The Access side is simulated by a bound queue and hand-built messages |
| Real Access consumer talking to real learning over RabbitMQ | Run (demo 3): two grants, `STREAK_MILESTONE` 10 points and `BADGE` 5 points, went from `PENDING` to `GRANTED` after one send (`attempts` 1) within about 14 seconds; the Access wallet went from 25 to 40 points |

## Demos

Learning-side coverage means a test in learning-service holds the behaviour with Access, Content and the broker stubbed.
It does not replace the Compose run.

The Compose column records what was observed. Learners: `learner@ielts.demo` (hearts, streak, freeze, weakness quest) and
`admin@ielts.demo` used as a second learner (XP cap, skipped day).

| # | Demo | Learning-side tests | Compose run |
|---|---|---|---|
| 1 | Five wrong answers in Practice → 0 hearts → `403 HEARTS_DEPLETED`; reviews still submit; refill for 5 points | `HeartsIntegrationTest.aLearnerWithNoHeartsIsRefusedWithTheNextHeartAndTheRefillCost`, `aRefillChargesPointsOnceUnderItsRequestIdAndAFullLearnerCannotRefillAgain`; `LessonLearningWebMvcTest` (403 body); `ReviewAndTestAssignmentIntegrationTest.aBlockingReviewCanBeSubmittedWithNoHeartsAndAWrongAnswerThereCostsNone` | Done. Five wrong lesson answers: hearts 5→0. The sixth submit: `403 HEARTS_DEPLETED` with `nextHeartAt` and `refillCost` 5. `POST /me/hearts/refill`: hearts 5/5, wallet 30→25. A second refill, with the same or a new `requestId`, returns `409 HEARTS_FULL` and the wallet stays 25 (a repeat of a finished refill is not replayed as `200`, but it costs nothing). "Reviews still submit with no hearts" was not run on Compose; held by the learning test only |
| 2 | A correct HARD answer → +20 XP; over 100 XP a day the rest is shadow | `GamificationIntegrationTest.twelveHardAnswersInADayCountOneHundredAndShadowTheRest`, `theDailyCapFollowsTheLearnersLocalDay` | Done, with a setup change: the seeded content has no `HARD` question (216 `EASY`, 157 `MEDIUM`), so five questions were set to `HARD` in the local content DB for the run and set back afterwards. A correct HARD answer: `xpEarned` 20. After 80 XP, a practice set of 4 `EASY` answers: 2 counted (`counted` true), 2 shadow (`counted` false), `xpCapped` true, `xpToday` 100. Milestone XP (`KP_CONSOLIDATE` 25, `KP_MAINTAIN` 50) and quest XP are not capped, total 205 |
| 3 | Seven active days → streak 7, +100 XP, 10 points to Access, badge `STREAK_7` | `StreakIntegrationTest.aSeventhDayPaysOneHundredXpAndQueuesTenPointsOnce`, `BadgeIntegrationTest.aSevenDayStreakAwardsTheBadgeOnceWithItsPoints`, `RewardGrantRelayIntegrationTest` (send, settle, resend). Data: `demo-data-learning.sql` | Done. Streak row seeded at 6 days ending yesterday; one answer today: `currentDays` 7, +100 XP, grants `STREAK_MILESTONE` 10 and `BADGE` 5 both `GRANTED` by the real Access, wallet 25→40, badge `STREAK_7` awarded |
| 4 | Buy a freeze, skip a day, streak kept | `StreakIntegrationTest.aFreezeIsChargedAtTheCurrentPriceAndOnlyOneIsOwnedAtATime`, `StreakRulesTest.aFreezeBoughtTodayCoversTomorrowAndTheStreakCarriesOn`, `aFreezeBoughtOnAnEmptyDayCoversThatDay` | Done. Buying a freeze: wallet 40→30, `freezeAvailable` true; a second purchase: `409 FREEZE_ALREADY_OWNED`, no extra charge. The skipped day, with the second learner seeded as in the demo SQL (5-day streak ending two days ago, freeze owned): reading the streak showed the freeze spent (`nextFreezeCost` 20) and `currentDays` 5; the answer today made it 6, `lostStreakDays` 0 |
| 5 | `WEAKNESS` quest completes when the target point is practised | `QuestIntegrationTest.threeAnswersOnTheTargetPointCompleteTheWeaknessQuest`, `aWeakPointWithPracticeBecomesTheTargetOfTheWeaknessQuest` | Done. After wrong answers on `DR_TOPIC_SENTENCE`, today's quests were recreated (the old rows deleted by hand, since quests are made once a day): `WEAKNESS` appeared with that point as target. Free practice on it with 4 correct answers: `WEAKNESS` 3/3 completed |
| 6 | 31 demo learners with XP → 2 league groups; closing the week promotes and demotes correctly | `LeagueIntegrationTest.thirtyOneLearnersFillOneGroupAndOpenASecond`, `closingAWeekPromotesTheTopSevenAndDemotesRanksTwentyThreeAndBelow`, `aSecondCloseChangesNothing`. Data: `demo-data-learning.sql` (verified on a throwaway Postgres: 31 members, 2 groups) | Done. Seeded 31 learners in one past week (groups of 30 and 1), learning-service restarted with `LEARNING_LEAGUE_CLOSE_CRON="0 * * * * *"` through a temporary Compose override file (not committed). Group of 30: 7 `PROMOTED` (XP 240–300), 8 `DEMOTED` (XP 10–80, Bronze stays Bronze), 15 `STAYED`; group of 1: `PROMOTED`. Tiers after: 23 in Bronze, 8 in Silver. Another close a minute later changed nothing |

To close a seeded week without waiting for Sunday, start learning-service with `LEARNING_LEAGUE_CLOSE_CRON="0 * * * * *"`.

## Decisions taken while closing (2026-10-18)

- No refund route for a learner: a hearts refill and a freeze purchase are charged under their `requestId` and a resend is
  not charged twice. The remaining risk (the learner never resends) is handled by the ADMIN refund route.
- Hearts are charged on every new submission, as built.
- A graded essay counts as an active day and as a practice answer on its first submission; it earns no answer XP and costs no heart.

## Daily points limit (2026-10-18)

Badge rewards are outside the 20 points a day limit and do not use up its budget; streak, quest and knowledge-point rewards
share it. Access commit `f2f50e8`. `mvn -B -ntp -o -pl services/access-service test`: 66 tests, 0 failures, 0 skipped (run
after the commit). Covered by `LearningPointsGrantListenerIntegrationTest.badgeGrantsAreExemptFromDailyLimitAndDailyQuestsOnlyUseTheRemainingAllowance`
(a 50 point badge is granted in full with 18 of 20 already earned; a quest then gets only the 2 left and is `CAPPED`).

## Open

- Access listener, invalid-reason HTTP handling, and contract updates are committed (`168da2a`, `8c5d9ec`, `dd6362c`).
- Compose acceptance is done for demos 1–6 and the real grant settlement (see above). The Compose fix for Access's RabbitMQ
  settings is in `docker-compose.yml` and `access-service.yaml` (uncommitted at the time of writing).
- Not exercised on Compose: submitting a blocking review with no hearts (held by the learning test), and a refund (there is
  no refund route by decision).
- The seeded content has no `HARD` question, so the +20 XP tier is never earned from the real course content until some
  questions are tagged HARD.
- `docs/system-architecture.md` does not exist; `.sdd/global/system-architecture.md` does not describe learning internals.
