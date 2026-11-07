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

## Independent gamification and Points purchases

- Tests-first clean RED observed missing gamification, Access client and refresher types before implementation.
- Focused PostgreSQL/domain/flashcard tests: 39 passed. Access client, plan refresher and me-security tests: 29 passed.
- Full required Library clean: 394 tests, zero failures/errors/skips, BUILD SUCCESS (2026-11-07, 6m44s). Surefire emitted a fork shutdown timeout after all tests completed; the build still exited successfully.
- Full Gateway clean: 58 tests, zero failures/errors/skips. Route regression first observed zero matching routes; anonymous gateway regression first observed 401 before the exact GET allowlist fix.
- Real PostgreSQL tests cover independent XP and streak, first-try hearts, Premium capacity, local dates, level-up refill, flashcard activity without profile/XP, same-request concurrency, crash recovery after debit, 402 cleanup, retained 503 intents, and rejection of HTTP calls within an outer transaction.
- Accepted purchase limitation remains: different concurrent request IDs can each debit before either effect is applied. Same-key purchase retries converge on one logical debit and one saved result.
- Necessary Access dependency fix and its independent validation are recorded in phase 3; no Library business HTTP retry was introduced.
- Seed content, live ingress acceptance, metrics and final operational validation remain pending.
