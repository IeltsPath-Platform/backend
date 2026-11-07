# Vocabulary study implementation evidence

## Units and authoring

- Tests first: StudyUnitRulesTest failed to compile because the feature types did not exist, before implementation.
- Full required `mvn -pl services/library-service -o clean test`: 317 tests, zero failures/errors/skips (2026-11-07).
- Reviewer regressions observed RED before fixes: changing kind leaked REVIEW before publication; interleaved publication labelled old content with curriculumVersion 2 instead of 1.
- Final focused clean run after all fixes: StudyUnitSecurityTest 10, StudyUnitListQueryTest 1, StudyUnitRulesTest 3, StudyUnitJdbcIntegrationTest 6; 20 passed, zero skipped.
- PostgreSQL Testcontainers actually ran. Tests cover revision visibility, active filtering, item deduplication, author-only roles, exact public allowlist, coded errors, FK/CHECK/cascade, concurrent publication, batch list reads, and consistent concurrent curriculum reads.
- Closest-plan persistence resolution: revision.kind is added to preserve draft publication isolation. No API shape or product decision changed.
- Gateway anonymous unit whitelist is a separate verified gap to fix with gateway routing before release. Live gateway/token acceptance remains pending phase 4.

## Attempts, grading and progress

- Tests-first clean compile RED: exercise/attempt/grading types were missing before implementation.
- Initial focused clean: 19 passed. Full required clean: 343 passed, zero failures/errors/skips.
- Unicode regression observed RED, then fixed; NBSP/decomposed Unicode answers normalize correctly and whitespace-only answers return INVALID_ANSWER.
- Resume idempotency regression observed behavioral RED in a clean run: retry after completion returned VOCAB_UNIT_COMPLETED. V40 start aliases now retain every logical start key, including RESUME_OR_START aliases to a REPLAY attempt.
- Final focused clean after alias fix: 30 passed, zero failures/errors/skips. PostgreSQL concurrency tests ran.
- Measured progress for 100 topics: exactly 3 real SQL queries, 18 ms in the full test run. This is a local test measurement, not a production SLA.
- Coverage includes cross-device resume, wrong-then-correct accuracy, invalid answer no-write, owner404, path-bound idempotency, published snapshot stability, archived-unit completion, expiry persistence, simultaneous starts/completion, and no leaked answer keys.
- API rewards remain absent at this intermediate commit as prescribed; the branch is not ready for mobile enablement until gamification lands.
