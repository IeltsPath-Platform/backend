# Phase 9 report: seed trap practice data

**Status: DONE**

## Files changed

- `services/content-service/src/main/resources/db/migration/V80__seed_error_trap_practice_data.sql` — adds 19 published practice sets with 4 questions each for the 10 in-scope error codes. Each set has at least two questions mapped to its parent knowledge point and carrying that code on a real wrong option. T/F/NG distractor questions use valid `TRUE`, `FALSE`, and `NOT_GIVEN` keys and preserve the intended correct answer.
- `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/ErrorTrapCoverageMigrationTest.java` — dynamically derives scope and verifies coverage, T/F/NG thresholds, valid distractor options and active codes, and that no question is assigned to a trap child point.
- `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/ErrorTrapBaselineDataTest.java` — deleted as required; its baseline measurements remain in `reports/baseline-data.md`.
- `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/PracticeSetCoverageMigrationTest.java` — pins its pre-existing seed inventory assertions to V79.
- `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/ErrorTrapKnowledgePointMigrationTest.java` — pins its migration-era counts and search-order fixture to V79.
- `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/LessonPipelineSeedTest.java` — pins its pre-existing fixed seed inventory assertions to V79; assertions were preserved.
- `plans/261024-1720-error-trap-sub-knowledge-points/reports/seed-trap-scope.md` — records the measured pre-seed scope and post-seed coverage.

## Baseline and red-first evidence

- Latest migration before creating the new migration: `V79__error_trap_knowledge_points.sql`. No existing migration was modified.
- Phase-9 baseline before V80: `mvn -q -pl services/content-service -am test` passed with 167 suites, 952 tests, 0 failures, 0 errors, 1 skipped.
- Before authoring seed data, `ErrorTrapCoverageMigrationTest` was run against the full schema and failed for the intended reason: nine in-scope codes had zero eligible sets, `INFERENCE_UNSUPPORTED` had one, and the separate T/F/NG threshold was unmet.
- Scope measurement found 10 codes needing practice data, below the stop threshold of 15. It required 19 new sets (two for nine codes and one for `INFERENCE_UNSUPPORTED`). `reports/seed-trap-scope.md` was written before the migration.
- A later strengthened assertion caught an invalid `NOT GIVEN` answer key in two draft questions. The keys were corrected to `NOT_GIVEN`; the final focused coverage test passed.

## Tests run and actual results

- Initial red: `mvn -q -pl services/content-service -am '-Dtest=ErrorTrapCoverageMigrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` — exit 1, expected pre-seed coverage gaps.
- Focused final migration verification: `mvn -q -pl services/content-service -am '-Dtest=ErrorTrapCoverageMigrationTest,PracticeSetCoverageMigrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` — exit 0; 9 tests passed.
- Focused pinned-fixture verification: `mvn -q -pl services/content-service -am '-Dtest=ErrorTrapCoverageMigrationTest,ErrorTrapKnowledgePointMigrationTest,LessonPipelineSeedTest,PracticeSetCoverageMigrationTest,DistractorSeedMigrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` — exit 0; 58 tests passed.
- Final `mvn -q -pl services/content-service -am test` after the final migration correction — exit 0; 168 suites, 955 tests, 0 failures, 0 errors, 1 skipped.

## Existing count tests pinned

- `PracticeSetCoverageMigrationTest` → `Flyway.target("79")`: V80 adds practice sets that change the old knowledge-point inventory counts.
- `ErrorTrapKnowledgePointMigrationTest` → `Flyway.target("79")`: its published T/F/NG counts and search-order fixture are defined against V79 and change when V80 adds trap sets.
- `LessonPipelineSeedTest` → `Flyway.target("79")`: its exact practice-set search and eligible-set counts describe the pre-V80 seed inventory; all original assertions remain unchanged.
- `DistractorSeedMigrationTest` remains pinned to V78 from phase 2 to preserve V46/V47 distractor counts.

## Success criteria

- The dynamically computed in-scope codes all have at least two eligible practice sets with at least two matching trap questions per set.
- Each T/F/NG error independently has at least two sets with two matching T/F/NG questions and the expected correct key.
- All distractor codes are active; each mapped wrong-option key exists on the question; no question is assigned to a trap child point.
- The scope report names excluded codes and why they were excluded.
- Existing content tests and the complete content-service module pass.
- `ErrorTrapBaselineDataTest` is removed and its previous measurements remain documented.

## Tests not run / open questions

- No phase-9 automated test remains unrun. The AI-authored practice content still requires the IELTS-qualified examiner review stated in the migration and plan before learner release.

## Acceptance follow-up (2026-10-24)

This follow-up supersedes earlier totals and records the changes requested after content acceptance:

- `V80__seed_error_trap_practice_data.sql` remains the latest migration and was corrected in place; no V81 was created and no prior migration changed.
- The 19 sets now use 76 individually authored rows. Seven codes have two new sets with eight distinct stems per code; the one new `INFERENCE_UNSUPPORTED` set has four stems distinct from its existing eligible set.
- Multiple-choice answer keys vary across A/B/C, with no key above half of any set of at least three questions. Trap mappings use varied wrong-option keys within each multiple-choice set.
- Both `TFNG_*` sets per code consist of four T/F/NG questions, each set has an untagged TRUE, and the `TFNG_NG_AS_FALSE` second set now has two genuinely NOT_GIVEN questions with FALSE tagged as the trap.
- Added two regression tests for cross-set duplicate stems and answer-key distribution. The existing T/F/NG coverage test also asserts all four questions are T/F/NG and at least one is untagged TRUE.
- `JdbcLearningContentReader.topicSequence` excludes trap points by `NOT EXISTS` on `error_types.trap_knowledge_point_id`. It checks for that column on older migration-era schemas; before V79 the relation is absent, so historical schema tests continue to exercise their original content.
- Updated `ErrorTrapKnowledgePointMigrationTest` assertions to identify trap points through the actual parent relation rather than the `TRAP_` code prefix. The N1 author-code regression test remains in place.
- `ErrorTrapBaselineDataTest.java` was already absent at the start of this follow-up; its measured baseline remains in `reports/baseline-data.md`.

### Verification

- Red-first focused run of `ErrorTrapCoverageMigrationTest` and `ErrorTrapKnowledgePointMigrationTest`: failed on the intended quality gaps (duplicate stems, imbalanced answer keys, TFNG composition) and the legacy prefix-based assertion.
- Focused regression run for the two trap migration tests and the four historical course/multiskill seed suites: exit 0.
- Final full module run `mvn -q -pl services/content-service -am test`: exit 0; 168 suites, 957 tests, 0 failures, 0 errors, 1 skipped. The stale Surefire XML for the already-absent `ErrorTrapBaselineDataTest` was excluded from the total.
- Testcontainers used Docker Desktop with PostgreSQL 15.18.

### Open item

- The AI-authored practice content still requires review by an IELTS-qualified examiner before learner release.
