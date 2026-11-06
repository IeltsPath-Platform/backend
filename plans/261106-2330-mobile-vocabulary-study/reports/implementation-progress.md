# Vocabulary study implementation evidence

## Units and authoring

- Tests first: StudyUnitRulesTest failed to compile because the feature types did not exist, before implementation.
- Full required `mvn -pl services/library-service -o clean test`: 317 tests, zero failures/errors/skips (2026-11-07).
- Reviewer regressions observed RED before fixes: changing kind leaked REVIEW before publication; interleaved publication labelled old content with curriculumVersion 2 instead of 1.
- Final focused clean run after all fixes: StudyUnitSecurityTest 10, StudyUnitListQueryTest 1, StudyUnitRulesTest 3, StudyUnitJdbcIntegrationTest 6; 20 passed, zero skipped.
- PostgreSQL Testcontainers actually ran. Tests cover revision visibility, active filtering, item deduplication, author-only roles, exact public allowlist, coded errors, FK/CHECK/cascade, concurrent publication, batch list reads, and consistent concurrent curriculum reads.
- Closest-plan persistence resolution: revision.kind is added to preserve draft publication isolation. No API shape or product decision changed.
- Gateway anonymous unit whitelist is a separate verified gap to fix with gateway routing before release. Live gateway/token acceptance remains pending phase 4.
