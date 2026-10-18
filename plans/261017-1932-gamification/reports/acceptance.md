# Acceptance: gamification (plan 261017-1932)

Updated 2026-10-18. Only what was run is marked as run. "Compose" columns are empty until someone runs the demo on a clean
Compose stack; nothing below claims they were.

## Verification run so far

| Check | Result |
|---|---|
| `mvn -B -ntp -o -pl services/learning-service test` | 712 tests, 0 failures, 0 skipped, PostgreSQL 15 Testcontainers ran (2026-10-18, after commit 14ff31d plus the plan notes) |
| `mvn -B -ntp -o verify` whole reactor | BUILD SUCCESS, 23 min 34 s, 0 failures, 0 skipped, Testcontainers ran (2026-10-18 07:57, working tree with Codex's in-progress edits to Access, Content and docs, not a clean commit). Tests per module: common-security 8, api-gateway 9, config-server 1, eureka-server 1, user-service 115, access-service 65, content-service 421, library-service 67, learning-service 712, assessment-service 309, game-service 15, notification-service 1, community-service 25 |
| `docker compose config --quiet` | exit 0 (2026-10-18) |
| Access suite (`LearningPointsGrantListener` with all four reasons) | Codex reports 64 tests, 0 failures; the fix is not committed yet and I did not rerun it |
| Learning ↔ Access over a real RabbitMQ | not run |

## Demos

Learning-side coverage means a test in learning-service holds the behaviour with Access, Content and the broker stubbed.
It does not replace the Compose run.

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

## Open

- Codex: commit the Access listener fix; contracts for Access points credit and for `display-names` in `user-internal-v1.md`.
- Whoever runs Compose: demos 1 to 6 above, and a real grant going from learning through RabbitMQ to Access and back as
  `GRANTED`. A final `mvn verify` over a clean commit once Codex has committed.
- `docs/system-architecture.md` does not exist; `.sdd/global/system-architecture.md` does not describe learning internals.
