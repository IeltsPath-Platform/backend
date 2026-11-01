Phase: C (3b)
Status: IN_PROGRESS
Commits:
- `850cf5c3` — test(resilience): preserve library not-found mapping
Files:
- `services/content-service/src/test/java/com/ieltspath/content/infrastructure/client/LibraryVocabularySenseClientTest.java`
Red tests:
- None. Per Claude's decision, `LibraryVocabularySenseClientTest.libraryNotFoundIsReportedAsUnavailable` was rewritten to assert the existing 404 → `VocabularyServiceUnavailableException` mapping; the test is expected to be green immediately.
Verification:
- Focused client test suite: 8 tests passed, 0 failures/errors/skips.
- Full `mvn -B -ntp verify` reached learning-service and stopped on RabbitMQ Testcontainers startup. Partial reactor totals from generated Surefire XML: 1,858 tests, 0 failures, 1 error, 4 skipped. It did not provide a green full-reactor verification.
Deviations:
- The C3b spec in `plans/261101-0930-reliability-followups/phase-03-thiet-ke-lai-resilience.md` describes a 404 mapping for this client that does not match its existing behavior. Per Claude's decision, behavior and mapping were preserved; Claude will correct the spec after the workstream. Codex did not edit the spec.
Concerns:
- The remaining C3b implementation is not complete: the other downstream clients listed for this pass have not yet been wrapped with `ResilientCall`, and their required resilience tests have not been added. Do not treat this package or the overall plan as complete.
