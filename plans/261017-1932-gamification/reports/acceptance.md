# Acceptance: gamification (plan 261017-1932)

Updated 2026-10-18. Demo cells stay blank until each API scenario has run. The clean-stack attempt is recorded below; Compose
startup failed, so none of the demos ran.

## Verification run so far

| Check | Result |
|---|---|
| Access suite (`LearningPointsGrantListener`, all four reasons, and invalid HTTP reason) | 65 tests, 0 failures, 0 errors, 0 skipped; PostgreSQL 15 Testcontainers ran (2026-10-18) |
| `mvn -B -ntp verify` whole reactor | BUILD SUCCESS, 24 min 27 s, 1,751 tests, 0 failures, 0 errors, 0 skipped; Testcontainers ran (2026-10-18, code changes later committed without further source edits). Per module: common-security 8, api-gateway 9, config-server 1, eureka-server 1, user-service 115, access-service 65, content-service 421, library-service 67, learning-service 714, assessment-service 309, game-service 15, notification-service 1, community-service 25 |
| `docker compose config --quiet` | exit 0 (2026-10-18) |
| Clean Compose stack | `down -v` succeeded. `docker compose --parallel 1 up -d --build` failed before services started: BuildKit returned `rpc error ... EOF`, then Docker Engine became unavailable. Engine recovered, but Windows C: still reports 0 GB free after pruning 4.78 GB of inactive BuildKit cache. No demo API calls were made. |
| Learning ↔ broker (`PointsGrantBrokerRoundTripTest`, RabbitMQ 3.13 container) | 2 tests pass: the relay publishes the agreed JSON on `learning.points` / `points.grant.requested`, and an answer in Access's shape (`GRANTED`, `FAILED` with error, plus an unreadable message dropped) settles the grant. The Access side is simulated by a bound queue and hand-built messages |
| Real Access consumer talking to real learning over RabbitMQ | not run; clean Compose stack could not start |

## Demos

Learning-side coverage means a test in learning-service holds the behaviour with Access, Content and the broker stubbed.
It does not replace the Compose run.

No Compose demos ran because the clean stack did not start; keep the Compose cells below blank until each API scenario is
actually exercised.

| # | Demo | Learning-side tests | Compose run |
|---|---|---|---|
| 1 | Five wrong answers in Practice → 0 hearts → `403 HEARTS_DEPLETED`; reviews still submit; refill for 5 points | `HeartsIntegrationTest.aLearnerWithNoHeartsIsRefusedWithTheNextHeartAndTheRefillCost`, `aRefillChargesPointsOnceUnderItsRequestIdAndAFullLearnerCannotRefillAgain`; `LessonLearningWebMvcTest` (403 body); `ReviewAndTestAssignmentIntegrationTest.aBlockingReviewCanBeSubmittedWithNoHeartsAndAWrongAnswerThereCostsNone` | |
| 2 | A correct HARD answer → +20 XP; over 100 XP a day the rest is shadow | `GamificationIntegrationTest.twelveHardAnswersInADayCountOneHundredAndShadowTheRest`, `theDailyCapFollowsTheLearnersLocalDay` | |
| 3 | Seven active days → streak 7, +100 XP, 10 points to Access, badge `STREAK_7` | `StreakIntegrationTest.aSeventhDayPaysOneHundredXpAndQueuesTenPointsOnce`, `BadgeIntegrationTest.aSevenDayStreakAwardsTheBadgeOnceWithItsPoints`, `RewardGrantRelayIntegrationTest` (send, settle, resend). Data: `demo-data-learning.sql` | |
| 4 | Buy a freeze, skip a day, streak kept | `StreakIntegrationTest.aFreezeIsChargedAtTheCurrentPriceAndOnlyOneIsOwnedAtATime`, `StreakRulesTest.aFreezeBoughtTodayCoversTomorrowAndTheStreakCarriesOn`, `aFreezeBoughtOnAnEmptyDayCoversThatDay` | |
| 5 | `WEAKNESS` quest completes when the target point is practised | `QuestIntegrationTest.threeAnswersOnTheTargetPointCompleteTheWeaknessQuest`, `aWeakPointWithPracticeBecomesTheTargetOfTheWeaknessQuest` | |
| 6 | 31 demo learners with XP → 2 league groups; closing the week promotes and demotes correctly | `LeagueIntegrationTest.thirtyOneLearnersFillOneGroupAndOpenASecond`, `closingAWeekPromotesTheTopSevenAndDemotesRanksTwentyThreeAndBelow`, `aSecondCloseChangesNothing`. Data: `demo-data-learning.sql` (verified on a throwaway Postgres: 31 members, 2 groups) | |

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
- Compose acceptance remains open: free/compact Docker's C: storage, start the clean stack, run demos 1–6, and verify a real
  Learning → RabbitMQ → Access → RabbitMQ grant settlement as `GRANTED`.
- `docs/system-architecture.md` does not exist; `.sdd/global/system-architecture.md` does not describe learning internals.
