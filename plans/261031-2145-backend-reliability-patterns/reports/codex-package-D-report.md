Phase: D
Status: IN_PROGRESS
Commits:
- `be9c3082` — test(access): cover subscription grant idempotency header
- `2735035b` — feat(access): make admin subscription grants idempotent
Files:
- Access admin grant idempotency implementation, migration `V7__admin_grant_idempotency.sql`, and `GrantSubscriptionIdempotencyTest` (see commit `2735035b`).
Red tests:
- `GrantSubscriptionIdempotencyTest.missingKeyIsBadRequest` — red before implementation because a grant without an idempotency key returned 201 and did not expose the key on the command.
Verification:
- `GrantSubscriptionIdempotencyTest`: 7 tests passed, 0 failures/errors/skips.
- `BusinessControllerRoleConventionTest`: 1 test passed after the approved operator-controller allowlist entry.
- Full `mvn -B -ntp verify` stopped during learning-service because RabbitMQ Testcontainers timed out waiting for the configured startup log. Partial reactor totals from generated Surefire XML: 1,858 tests, 0 failures, 1 error, 4 skipped.
Deviations:
- No production behavior outside the approved admin-grant idempotency slice was changed.
Concerns:
- The package remains incomplete against the full spec: `UnlockCourseFailureWindowTest`, `HumanGradingCreditFailureWindowTest`, and the required listener duplicate-delivery tests were not implemented. The reactor did not pass, and downstream packages were not covered by the full run.
