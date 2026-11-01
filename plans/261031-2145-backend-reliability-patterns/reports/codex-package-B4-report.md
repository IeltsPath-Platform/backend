Phase: B4
Status: DONE_WITH_CONCERNS
Commits:
- 77e58f51 test(reliability): cover concurrent state and DLQ replay
- c293576f docs(messaging): classify shared listener retry policy
Files:
- `services/game-service/src/test/java/com/ieltspath/game/infrastructure/persistence/GameRoomConcurrentUpdateTest.java`
- `services/learning-service/src/test/java/com/ieltspath/learning/api/controller/LearningDeadLetterReplayIntegrationTest.java`
- `services/learning-service/src/test/java/com/ieltspath/learning/infrastructure/persistence/LessonProgressConcurrentUpdateTest.java`
- `services/library-service/src/test/java/com/ieltspath/library/infrastructure/persistence/FlashcardConcurrentUpdateTest.java`
- `services/access-service/src/main/java/com/ieltspath/access/infrastructure/messaging/LearningPointsGrantListener.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/infrastructure/messaging/PointsGrantSettledListener.java`
- `services/notification-service/src/main/java/com/ieltspath/notification/infrastructure/GradingCompletedListener.java`
- `services/notification-service/src/main/java/com/ieltspath/notification/infrastructure/GradingDueSoonListener.java`
- `services/notification-service/src/main/java/com/ieltspath/notification/infrastructure/GradingOverdueListener.java`
- `services/notification-service/src/main/java/com/ieltspath/notification/infrastructure/GradingRequestedListener.java`
Red tests (commit test):
- None. The four new behavioral tests passed against the existing locking and replay implementations, so no behavior regression could honestly be demonstrated before the implementation commit.
Verification:
- Focused new tests: 4 tests, 0 failures, 0 errors, 0 skipped. `LessonProgressConcurrentUpdateTest`, `LearningDeadLetterReplayIntegrationTest`, `FlashcardConcurrentUpdateTest`, and `GameRoomConcurrentUpdateTest` all passed.
- Required package command `mvn -B -ntp -pl services/learning-service,services/library-service,services/game-service,services/access-service,services/notification-service -am verify` stopped in `learning-service`: 1,593 tests reached, 1 failure, 0 errors, 3 skipped. `access-service`: 100 tests passed. `library-service`: 243 tests passed. `learning-service`: 1,140 tests, 1 failure. The failure is `BusinessControllerRoleConventionTest.businessEndpointsDoNotGrantSystemAdministratorAccess` because `DeadLetterAdminController` has `@PreAuthorize("hasRole('ADMIN')")`; this pre-existing B1 failure occurs outside B4 scope, so Maven did not reach Game or Notification in this command.
- Supplementary full Game verification `mvn -B -ntp -pl services/game-service -am verify`: 187 game-service tests passed, 0 failures/errors/skips; upstream common-security 9 and common-observability 22 also passed.
- Notification compile/package `mvn -B -ntp -pl services/notification-service -am -DskipTests package`: BUILD SUCCESS. Tests were intentionally skipped in this compile-only fallback.
Deviations from skeleton:
- No behavioral red test: the current lesson-progress advisory lock, flashcard row lock, game-room row lock, and Learning DLQ replay endpoint already satisfy the new tests. The tests are committed separately before the documentation/code commit, but they were green on the existing implementation.
- The exact package verification command stopped at the known Learning role-convention failure and did not reach Game/Notification. Game was verified with its own full reactor command; Notification sources were compiled and packaged, but its tests were not run in this B4 pass.
Concerns:
- Full B4 reactor verification remains blocked by the existing `DeadLetterAdminController` role-convention failure. It was not changed because it is not among B4's allowed files. Notification still has previously reported B3 integration failures; this package changes only its Javadocs and did not run those tests.

Follow-up (2026-11-01): per Claude's decision, `BusinessControllerRoleConventionTest` now allows exactly `DeadLetterAdminController.class` with the comment `Dead-letter replay is an operator task on the message broker, not a business capability.` (`3a37361d`); the targeted convention test passed 1/1. The later full `mvn -B -ntp verify` passed through library-service but stopped in learning-service because RabbitMQ Testcontainers timed out waiting for `Server startup complete` in `LearningDeadLetterReplayIntegrationTest`. The latest partial generated Surefire totals were 1,858 tests, 0 failures, 1 error, 4 skipped; modules after learning were not reached. This is recorded as a verification deviation from the earlier role-convention-only blocker.
