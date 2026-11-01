Phase: B1
Status: DONE_WITH_CONCERNS
Commits:
- b2d99032 test(reliability): cover scheduler lock and relay concurrency
- 8cfe304c fix(learning): release sync lock before content lookup
Files:
- `services/learning-service/src/main/java/com/ieltspath/learning/application/service/WorkItemSync.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/infrastructure/scheduler/WorkItemSyncJob.java`
- `docs/system-architecture.md`
- `plans/261031-2145-backend-reliability-patterns/phase-01-nen-nhieu-instance.md`
- `plans/261031-2145-backend-reliability-patterns/phase-02-observability.md`
- `plans/261031-2145-backend-reliability-patterns/phase-05-resilience-va-goi-service.md`
- `plans/261031-2145-backend-reliability-patterns/phase-06-idempotency-va-saga.md`
- `plans/261031-2145-backend-reliability-patterns/phase-08-test-loi-tong.md`
- `plans/261031-2145-backend-reliability-patterns/reports/codex-package-B1-report.md`
- New tests are isolated in the first commit: `WorkItemSyncJobTest`, `OutboxRelayConcurrencyTest`, `CommunityOutboxRelayConcurrencyTest`.
Red tests (commit test): `WorkItemSyncJobTest.doesNotHoldTheAdvisoryLockWhileCallingContent` — the pre-change job called Content while the transaction-scoped advisory lock was held; the independent connection could not acquire the lock. The two relay concurrency tests passed before the code commit.
Verification:
- `mvn -B -ntp -pl services/learning-service,services/assessment-service,services/community-service -am verify` → 1,239 tests reached before Maven stopped at learning-service; 1 failure, 0 errors, 3 skipped. Breakdown: common-events 19/0/0/0, common-security 9/0/0/0, common-storage 60/0/0/3, common-observability 18/0/0/0, learning-service 1,133/1/0/0. Assessment and community modules were not reached by this reactor invocation.
- Failure: `BusinessControllerRoleConventionTest.businessEndpointsDoNotGrantSystemAdministratorAccess` rejects `DeadLetterAdminController`'s `@PreAuthorize("hasRole('ADMIN')")`. The controller came from pre-existing commit `32d3886d` (dead-letter replay) and neither it nor the convention test was changed in this package. Per instruction, the existing test was not edited or disabled.
- Focused learning verification: 2 tests, 0 failures, 0 errors, 0 skipped (`WorkItemSyncJobTest`).
- Focused learning verification was rerun on the committed code tree: 2 tests, 0 failures, 0 errors, 0 skipped; reactor build succeeded.
- Focused relay verification before code: assessment 1 test and community 1 test, both 0 failures/errors/skips.
Deviations from skeleton: none.
Concerns: Full B1 reactor verification remains blocked by the unrelated pre-existing role convention failure above; Maven therefore did not reach assessment/community in that command. Claude's manual multi-instance check remains outstanding. `StaffWorkUseCase.list` also calls the compatibility `WorkItemSync.sync()` from a request transaction; changing that request transaction boundary is outside this package's listed files and is not part of the scheduled-job acceptance criterion.
