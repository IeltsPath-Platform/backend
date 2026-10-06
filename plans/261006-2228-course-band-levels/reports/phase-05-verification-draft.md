# Learner placement verification draft

Date: 2026-10-07

Status: DONE — the authorized migration-version and table-count expectations are updated. Focused and full Docker-backed learning suites pass; phase 5 is ready to commit.

## Resumed validation — 2026-10-07

The focused command `mvn -q -pl services/learning-service -am test '-Dtest=BandLevelTest,LearnerPlacementTest,ApplyPlacementResultUseCaseTest,AssessmentCompletedParserTest,AssessmentResultIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false'` exited 0: 19 selected Learning tests passed, with 0 failures, 0 errors and 0 skipped. `AssessmentResultIntegrationTest` ran through Testcontainers and applied migrations V1–V6.

The first full command failed on the then-unapproved migration-count expectation. After authorization, only the two requested numbers changed (`5` → `6`, `14` → `15`); the health check remained unchanged. SQL count: 14 baseline tables + the single `CREATE TABLE` in V6 (`learner_placements`) = 15. The rerun `mvn -q -pl services/learning-service -am test` exited 0: Learning Service 228 passed, 0 failures, 0 errors, 0 skipped; shared `common-security` 8 passed, 0 failures, 0 errors, 0 skipped.

## RED evidence

Executed before production implementation, with elevated Maven access and no concurrent Maven process:

```powershell
mvn -q -pl services/learning-service -am test '-Dtest=BandLevelTest,LearnerPlacementTest,AssessmentCompletedParserTest,AssessmentResultIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false'
```

Exit code: 1. Maven failed at learning-service `testCompile` because `LearnerPlacementTest` referenced the not-yet-created `com.group01.learning.domain.vo.BandLevel` (lines 3 and 48). This was the expected RED result for the new type. No learning tests executed, so there are no learning pass/failure/skip counts for this run. No old test failure was observed. Existing Surefire files must not be presented as evidence for this RED run.

Local log: `services/learning-service/target/phase-05-red.log` (ignored build output; not committed).

## Preserved implementation

New production files, relative to `services/learning-service/`:

- `src/main/java/com/group01/learning/domain/vo/BandLevel.java`
- `src/main/java/com/group01/learning/domain/aggregate/LearnerPlacement.java`
- `src/main/java/com/group01/learning/domain/repository/LearnerPlacementRepository.java`
- `src/main/java/com/group01/learning/infrastructure/persistence/JdbcLearnerPlacementRepository.java`
- `src/main/resources/db/migration/V6__learner_placements.sql`

Modified production files:

- `src/main/java/com/group01/learning/application/command/AssessmentResult.java`
- `src/main/java/com/group01/learning/application/usecase/ApplyAssessmentResultUseCase.java`
- `src/main/java/com/group01/learning/infrastructure/messaging/AssessmentCompletedParser.java`

The written implementation adds a pure half-step band value object, a learner placement aggregate and JDBC repository, optional numeric `overall_band` parsing, a compatible old `AssessmentResult` constructor, and placement persistence after the existing lock/replay/version bookkeeping. The aggregate accepts newer different attempts and regrades of the currently stored attempt, while ignoring older or equally dated different attempts. Parser numeric handling preserves decimal precision before validation. These descriptions reflect source written, not executed validation.

New test files:

- `src/test/java/com/group01/learning/domain/vo/BandLevelTest.java`
- `src/test/java/com/group01/learning/domain/aggregate/LearnerPlacementTest.java`
- `src/test/java/com/group01/learning/application/usecase/ApplyPlacementResultUseCaseTest.java`

Modified test files:

- `src/test/java/com/group01/learning/infrastructure/messaging/AssessmentCompletedParserTest.java`
- `src/test/java/com/group01/learning/infrastructure/persistence/AssessmentResultIntegrationTest.java`

Added cases cover band bounds/half steps, optional and invalid event values, placement ordering/regrades/replay, and persistence without mastery/review/topic-gate effects. Existing expectations were not changed. The integration setup additionally clears the new placement table. The Mockito use-case cases and a high-precision parser regression were added after the RED command; they have not run.

## Resolved phase-3 dependency

The earlier blockers were in content-service `LessonPipelineSeedTest` and have since been authorized and resolved in phase 3:

1. `lessonPracticeAndTestQuestionsAreReservedForLearning` reads all question versions and expects 147 reserved versions. V21 adds 24 reserved LEARNING versions, producing 171.
2. `practiceSetsBelongToTheEarliestLessonTeachingTheirKnowledgePoint` queries all PRACTICE_SET mappings and expects the exact old 28-entry map. V21 adds `R65-PS-INFERENCE` and `R65-PS-PARAPHRASE`, both mapped to `R65-I1`, producing 30 entries.

The exact authorized changes were exercised successfully with Docker: reserved versions now assert 171 and the exact package map includes only the two new `R65-*` entries. Phase 3 committed as `feat(content): seed courses and higher-band reading`.

## Verification outcome

Final suite results and counts are recorded above and in the Phase 5 Verification sections of `plan.md` and `phase-05-learning-learner-level.md`. No real LLM call occurred.

The placement implementation is verified and ready for its phase commit.
