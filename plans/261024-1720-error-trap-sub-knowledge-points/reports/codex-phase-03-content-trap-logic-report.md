# Phase 3 report: content trap logic and set search

**Status: DONE**

## Files changed

- `services/content-service/src/main/java/com/ieltspath/content/domain/service/TrueFalseNotGivenTraps.java`
- `services/content-service/src/main/java/com/ieltspath/content/application/service/TrapKnowledgePointSync.java`
- Error type aggregate/repository and create/update use cases for trap-point linkage and synchronization.
- Knowledge-point activation/deactivation/update and prerequisite use cases for synchronization and child-point restrictions.
- `AddQuestionVersionUseCase` for T/F/NG distractor completion and rejection of child-point assignment.
- Topic-sequence and practice-set search ports, use cases, JDBC reader, controller and response/request DTOs.
- `GlobalExceptionHandler.java` to expose the required `TRAP_KNOWLEDGE_POINT_NOT_ASSIGNABLE` error code.
- `docs/contracts/learning-content-internal-v1.md` and `docs/contracts/answer-spec-v1.md`.
- Unit and integration tests for trap mapping, synchronization, question creation, point filtering, prerequisites, topic sequence and search ranking.
- `KnowledgeTreeIntegrationTest.java` fixture updated for the additional collaborators.

## Baseline and red-first evidence

- Baseline before phase-3 changes: `mvn -q -pl services/content-service -am test` passed; the prior phase-2 report records 928 tests, 0 failures, 0 errors, 1 skipped.
- The new search-ranking integration assertion was run against the old ranking SQL and failed on the expected trap-first ordering. The ranking implementation was restored and the focused assertion passed.
- An earlier focused implementation run passed the trap migration, topic sequence, API, use-case, synchronization, prerequisite and point-tree tests. The latest full run below includes the final expanded ranking assertions.
- No pre-existing red test was observed in the baseline.

## Tests run and actual results

- Focused phase-3 test selection (`ErrorTrapKnowledgePointMigrationTest`, `LearningContentUseCasesTest`, `InternalLearningContentControllerTest`, `TrueFalseNotGivenTrapsTest`, `TrapKnowledgePointSyncTest`, `AddQuestionVersionUseCaseTest`, `GetKnowledgePointsUseCaseTest`, `ReplaceKnowledgePointPrerequisitesUseCaseTest`, `KnowledgeTreeIntegrationTest`): exit 0.
- `ErrorTrapKnowledgePointMigrationTest#practiceSetSearchRanksByRequestedTrapBeforeLegacyLessonPreference` with old ranking temporarily restored: expected red due to incorrect old ordering; after implementation: 1 test, 0 failures, 0 errors.
- Final `mvn -q -pl services/content-service -am test`: exit 0; Surefire reports 167 suites, 952 tests, 0 failures, 0 errors, 1 skipped.

## Success criteria

- Trap child points synchronize from active error types and active parent points; changes to error type and parent-point status update the child state.
- T/F/NG questions receive distractor mappings only when eligible mappings are active and the answer specification does not already supply them.
- Question and prerequisite assignment to trap child points is rejected; author point picker omits child points.
- Practice-set search accepts an optional trap code, returns `trapQuestionCount`, prioritizes qualifying trap counts/ratios, and preserves legacy ordering below the threshold and when no code is requested.
- Default topic sequence omits child points; opting in returns parent/error metadata and marks child points as not directly practiceable.
- Internal learning-content and answer-spec contracts describe the new fields and auto-mapping behavior.
- Full content-service module passed after the final changes.

## Tests not run / open questions

- No phase-3 tests remain unrun; no open questions.
