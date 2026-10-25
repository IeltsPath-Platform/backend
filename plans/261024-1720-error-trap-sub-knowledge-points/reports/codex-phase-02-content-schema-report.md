# Phase 2 report: content schema and trap knowledge points

**Status: DONE**

## Files changed

- `services/content-service/src/main/resources/db/migration/V79__error_trap_knowledge_points.sql` — adds the unique trap-point FK, creates child points for active error types with active parents, and backfills T/F/NG distractors without changing correct answers.
- `services/content-service/src/main/java/com/ieltspath/content/infrastructure/persistence/adapter/JdbcLearningContentReader.java` — hides reserved `TRAP_` child points from the default topic sequence. The code-prefix filter also supports older Testcontainers fixtures at V58, where `error_types.trap_knowledge_point_id` does not exist yet.
- `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/ErrorTrapKnowledgePointMigrationTest.java` — verifies trap-point relationships, unchanged parent IDs and correct-answer counts, T/F/NG distractors, and default topic-sequence filtering.
- `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/PracticeSetCoverageMigrationTest.java` — excludes trap children from parent-KP practice coverage counts.
- `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/DistractorSeedMigrationTest.java` — pins the existing V46/V47 seed assertions to Flyway V78.

## Baseline and red-first evidence

- Docker was available (`docker ps`, exit 0); Testcontainers connected to PostgreSQL 15.
- Latest migration before V79 was created: `V78__section_question_sort_order_deferrable.sql`.
- Original content-service baseline before V79: exit 0; 921 tests, 0 failures, 0 errors, 1 intentional skip.
- Resumed phase baseline with V79 present but the remaining phase-2 fixes absent: exit 1; 927 tests, 4 failures, 0 errors, 1 intentional skip. Failures were the V46/V47 global distractor counts and the two topic-sequence expectations in `LessonPipelineSeedTest`.
- `ErrorTrapKnowledgePointMigrationTest.topicSequenceDoesNotListTrapPoints` before the filter: exit 1 as expected; it found 13 `TRAP_*` child points in the default topic sequence.
- Earlier phase-2 migration tests before V79 failed as expected because `trap_knowledge_point_id` and the T/F/NG mappings did not yet exist. The initial test attempt also caught a test-side SQL parameter parsing issue, corrected before confirming the expected red result.

## Tests run and actual results

- `mvn -q -pl services/content-service -am test '-Dtest=ErrorTrapKnowledgePointMigrationTest' '-Dsurefire.failIfNoSpecifiedTests=false'` before V79: expected red for missing schema/backfill.
- `mvn -q -pl services/content-service -am test '-Dtest=ErrorTrapKnowledgePointMigrationTest#topicSequenceDoesNotListTrapPoints' '-Dsurefire.failIfNoSpecifiedTests=false'` before topic filter: expected red with 13 trap points present.
- `mvn -q -pl services/content-service -am test '-Dtest=ErrorTrapKnowledgePointMigrationTest,PracticeSetCoverageMigrationTest' '-Dsurefire.failIfNoSpecifiedTests=false'` after V79: exit 0; 12 tests, 0 failures, 0 errors.
- `mvn -q -pl services/content-service -am test '-Dtest=ErrorTrapKnowledgePointMigrationTest,PracticeSetCoverageMigrationTest,DistractorSeedMigrationTest,LessonPipelineSeedTest,CourseDemoContentSeedTest,CourseSeedTest,MultiSkillSeedTest' '-Dsurefire.failIfNoSpecifiedTests=false'`: exit 0.
- `mvn -q -pl services/content-service -am test`: exit 0; 928 tests, 0 failures, 0 errors, 1 intentional skip across 164 suites.

`LessonPipelineSeedTest` was not modified; all 34 tests passed. The six `DistractorSeedMigrationTest` tests and their assertions were preserved, with only Flyway pinned to V78 as authorized by the plan. The newly added migration test passed all 7 tests.

## Test-count exception applied

- `DistractorSeedMigrationTest`: pinned to V78 because it verifies the V46/V47 seed counts (20 mapped questions and 31 wrong options). V79 adds later T/F/NG mappings; those are covered by `ErrorTrapKnowledgePointMigrationTest`.

## Success criteria

- Migration and coverage Testcontainers tests passed.
- V79 establishes a unique FK; the migration creates points only for active errors with active parents, leaving other error types without a trap-point link.
- T/F/NG distractor mappings were added without changing published correct-answer counts.
- Default `topicSequence(3)` has no `TRAP_` points; the unmodified `LessonPipelineSeedTest` passes.
- Full content-service module passed.

## Tests not run / open questions

- No phase-2 tests remain unrun.
- No open questions.
