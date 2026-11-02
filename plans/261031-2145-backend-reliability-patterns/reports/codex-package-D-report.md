Phase: D
Status: DONE_WITH_CONCERNS
Commits:
- `be9c3082` — test(access): cover subscription grant idempotency header
- `2735035b` — feat(access): make admin subscription grants idempotent
- `8b403546` — test(reliability): cover saga failures and duplicate delivery
Files:
- Added three human-grading credit failure-window cases in `HumanGradingCreditFailureWindowTest`.
- Expanded `UnlockCourseFailureWindowTest` to cover pending retries and Access breaker-open behavior, and aligned the test method names with phase 6.
- Added `RewardGrantRelayIntegrationTest.secondDeliveryHasNoSideEffect` for `PointsGrantSettledListener` duplicate delivery.
- Added `AssessmentCompletedListenerDuplicateDeliveryTest.secondDeliveryHasNoSideEffect`, using PostgreSQL and the real listener/use case to verify the persisted result version is not rewritten on redelivery.
- Audited the remaining phase 6 listener table: existing duplicate cases are present for the four grading notification listeners, subscription-granted and community-event consumers, and the Access learning-points grant listener.
Red tests (commit test): no red-first test commit was created for the C/D/E continuation. The original access grant idempotency red test is documented above.
Verification:
- `HumanGradingCreditFailureWindowTest` → 3 tests, 0 failures, 0 errors, 0 skipped.
- Learning saga and duplicate-delivery suites (`UnlockCourseFailureWindowTest`, `RewardGrantRelayIntegrationTest`, `AssessmentCompletedListenerDuplicateDeliveryTest`) → 14 tests, 0 failures, 0 errors, 0 skipped.
- Learning clean focused test compilation → BUILD SUCCESS; the earlier 21 errors were stale target output and cleared after a clean rebuild.
- Assessment human-grading failure-window suite → 3 tests, 0 failures, 0 errors, 0 skipped (focused run already recorded above).
- Full reactor `verify` was not run.
Deviations from skeleton: The human-grading failure-window test uses the existing use case with ports mocked rather than a PostgreSQL integration fixture. Learning saga cases use PostgreSQL Testcontainers and a mocked Access port rather than `MockRestServiceServer`. The `PointsGrantSettledListener` duplicate case remains in the existing relay integration class; the new `AssessmentCompletedListener` case uses the requested dedicated test class.
Concerns: No full-reactor verify was run, per the user's instruction. Focused verification is being rerun for the final continuation commits; no saga production logic was changed.

Current focused verification (2026-11-02):
- `UnlockCourseFailureWindowTest`, `RewardGrantRelayIntegrationTest`, and `AssessmentCompletedListenerDuplicateDeliveryTest` → 14 learning tests, 0 failures, 0 errors, 0 skipped.
- `HumanGradingCreditFailureWindowTest` → 3 assessment tests, 0 failures, 0 errors, 0 skipped.
- Combined D-focused rerun → 17 tests, 0 failures, 0 errors, 0 skipped.
