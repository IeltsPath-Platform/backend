# Phase 6 — Learning feedback and report

Status: DONE

## Baseline and red tests

- Baseline: `mvn -q -pl services/learning-service -am test` — 125 suites, 922 tests, 0 failures/errors/skips.
- Added contract-focused tests for answer feedback, nested report/tree children, basket `ERROR_PATTERN` labels, and
  review detail `blocking`. The red run failed at compilation because `AnswerResult.error`, `PracticeSubmission.Answer.error`,
  `KnowledgePointReport.errorTraps`, `GetLearnerTreeUseCase.Node.errorTraps`, and review response `blocking` did not yet
  exist. The later response-DTO red test also failed to compile because `SubmissionResponse` had no `error` field and the
  practice response mapper dropped the feedback. A follow-up red assertion reproduced that review and diagnostic
  response mappers dropped the field too. These were the requested contract gaps.

## Implementation

- Added optional `error: {code, name, fixHint}` feedback to lesson, Practice, review, free-practice, and diagnostic answers.
  Text lookup occurs after grading and is best-effort; unknown codes or Content lookup failure omit the field. Evidence
  mapping and writes continue to use the local catalog without HTTP.
- Wired the feedback through API response DTOs for unpassed and passed lesson blocks and Practice/free-practice
  submissions. The DTO red test showed that application result fields were otherwise dropped at serialization mapping.
  Kept the Practice submission value object independent of application and Jackson layers.
- Nested only trap children with learner evidence under their parent in learner report and tree. Children remain out of
  top-level report/rollups and free-practice availability. The report derives the parent's current `errors` from the
  weakest eligible child rather than `top_error_codes`; per-question evidence still carries its original error code.
- Added `errorTrap` to optional basket entries triggered by `ERROR_PATTERN` and returned the main error's current label.
- Added `blocking` to review detail results, including compatibility constructors whose legacy default is blocking.
  The production result copies `ReviewItem.blocking()`.
- Updated the five related API contracts: lesson learning, free practice/learner tree, diagnostics, learner report, and
  knowledge tree.
- Updated the existing report integration assertion: a parent with no trap-child profile now has no main-error summary.
  This reflects the changed contract; no count was pinned or relaxed.

## Verification

- Focused answer feedback, response DTO mapping, report/tree, and legacy report/tree tests — pass.
- `ReviewBasketIntegrationTest` (Docker/Testcontainers enabled) — pass, including main-error basket label and
  `blocking` result assertions.
- Final `mvn -q -pl services/learning-service -am test` — exit 0; 128 suites, 929 tests, 0 failures, 0 errors, 0 skips.
  The Surefire fork printed its existing delayed-shutdown warning after tests completed; Maven exited successfully.
- No tests were skipped for missing Docker. No count tests were pinned in this phase.

## Files changed

- `services/learning-service/src/main/java/com/ieltspath/learning/application/result/LearnerReportResult.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/result/ReviewBasketResult.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/result/ReviewResult.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/result/SubmissionResult.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/api/dto/response/SubmissionResponse.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/api/dto/response/PracticeSubmissionResponse.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/api/dto/response/ReviewSubmissionResponse.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/api/dto/response/DiagnosticProbeResponse.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/service/ErrorTrapEvidence.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/service/FreePracticeSubmission.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/service/KnowledgePointDirectory.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetLearnerReportUseCase.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetLearnerTreeUseCase.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetReviewBasketUseCase.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetReviewUseCase.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitDiagnosticProbeUseCase.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitLessonExerciseUseCase.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitPracticeAttemptUseCase.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/SubmitReviewUseCase.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/domain/vo/PracticeSubmission.java`
- `services/learning-service/src/test/java/com/ieltspath/learning/application/result/ErrorTrapFeedbackContractTest.java`
- `services/learning-service/src/test/java/com/ieltspath/learning/application/result/ErrorTrapNestingContractTest.java`
- `services/learning-service/src/test/java/com/ieltspath/learning/api/dto/response/ErrorTrapFeedbackResponseTest.java`
- `services/learning-service/src/test/java/com/ieltspath/learning/infrastructure/persistence/MasteryProfileIntegrationTest.java`
- `services/learning-service/src/test/java/com/ieltspath/learning/infrastructure/persistence/ReviewBasketIntegrationTest.java`
- `docs/contracts/diagnostics-v1.md`
- `docs/contracts/free-practice-v1.md`
- `docs/contracts/knowledge-tree-v1.md`
- `docs/contracts/learner-report-v1.md`
- `docs/contracts/lesson-learning-v1.md`

## Open questions

- None. Compose walkthrough and repository-wide verification are tracked in phase 8.

## Addendum after acceptance feedback

- Testcontainers: lesson submission checks the `error` code/name/fixHint; a Content lookup exception still returns a successful
  submission with no `error` while persisting child evidence. Review basket integration checks `ERROR_PATTERN.errorTrap` and
  `blocking=false` on review details. Learner report/tree integration checks that only an evidenced child is nested below its parent.
- Red-proof Content failure: `ErrorTypeTexts.byCode` was temporarily changed to rethrow. The Testcontainers submission test failed
  because Content failure escaped; the best-effort catch was restored.
- The end-to-end alternative for V1 and the fact that Compose HTTP was not run are recorded precisely in `verification.md`.
- Cleanup S1: removed null-passing compatibility constructors from `ErrorTrapEvidence`, `FreePracticeSubmission`,
  `GetLearnerReportUseCase`, `GetReviewBasketUseCase`, `GetReviewUseCase`, `StartBasketReviewUseCase`, and
  `SubmitPracticeAttemptUseCase`; scorer paths no longer silently skip ErrorTrapEvidence. The unit tests now pass a mock bean.
- Cleanup S2: Practice feedback lookup filters actual non-null graded error codes and does no lookup for an essay-only result.
  The red-proof temporarily restored the `ESSAY_RESULT` sentinel and `SubmitPracticeAttemptUseCaseTest` failed on the unexpected lookup.
- Cleanup S3: `GetReviewUseCase` computes the error trap only for `PENDING`; the DONE/SKIPPED red-proof is recorded in phase 5.
- Final full-module command: `mvn -q -pl services/learning-service -am test` — exit 0; 129 suites, 941 tests,
  0 failures, 0 errors, 0 skips. Docker/Testcontainers was available.
- No legacy pinned-count assertion was relaxed or changed.
