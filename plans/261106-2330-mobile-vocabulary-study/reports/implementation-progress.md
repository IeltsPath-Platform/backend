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
- At the gamification commit checkpoint, seed content, live ingress acceptance, metrics and final operational validation remained pending; final results follow below.

## Demo curriculum and operational verification

- Tests-first behavioral RED: missing curated units (expected 4, got 0), absent publication metadata and absent logical-operation counter. Parent Access metrics separately observed 5 tests with 3 behavioral failures before instrumentation; the combined red run had 22 tests, 1 failure and 2 errors.
- Initial focused green: 34 tests, zero failures/errors/skips, including eight curated units and generator-equivalent metadata.
- Upgrade regressions observed real RED before the migration correction: 3 tests failed on author deactivation, edited examples/inactive topics and an unchanged existing curriculum version.
- Final focused green after upgrade correction: 38 tests, zero failures/errors/skips (2m19s). Four upgrade cases cover inactive catalog items, unsuitable examples/inactive topics, duplicate edited meanings, and appending after authored units while bumping the existing version once.
- Operation counters run after commit and count only new persisted attempts, completions and purchase transitions; saved retries and rolled-back effects are tested. Access terminal failures count once after the shared resilience policy.
- Eight-word attempt local mean: 71 ms initially, 132 ms during the later focused run (three warm samples each, under 200 ms). Progress for 100 topics remains exactly three real queries, measured at 18 ms in phase 2. These are local observations, not production SLAs.
- Fresh origin/main and open-PR recheck at 11:29 Asia/Bangkok on 2026-11-07: main remains e43c7dbd, only open PR 154 changes Community V11, so Library V39-V42 have no migration-number conflict.
- Necessary Access dependency fix is committed separately as 1ec9f465; its full clean passed 112 tests with no failures/errors/skips.
- Final required `mvn -pl services/library-service -o clean test`: 407 tests, zero failures/errors/skips, BUILD SUCCESS, actual process exit 0 (8m49s, completed 11:37:53 Asia/Bangkok). Docker-backed migrations and concurrency tests ran. Surefire again emitted its 30-second fork shutdown timeout after tests completed; it did not fail the build.
- Final full-run local measurements: eight-word attempt mean 49 ms across three warm samples; 100-topic progress exactly 3 queries, 29 ms. Final live-stack supplemental results follow below; no unexecuted media/device check is represented as a pass.
- Live isolated Library boot is healthy with Flyway 42 and eight real seed units. Initial live checks: 58 passed, covering all 11 learner/content and 7 author OpenAPI methods, anonymous reads of both topics/eight units, eight words per unit and 24 distinct items per topic. All 192 checked media fields were null because storage configuration is blank; non-null asset availability and physical-device playback are unverified.
- Committed smoke runner executed through the real Gateway: 239 checks passed in 19.327 seconds. Wrong-then-correct first unit grants 20 XP, replay grants zero, perfect second unit grants 30; final XP is 50 and streak remains 1. The runner also checks saved completion replay, all four step types, hearts and progress unlocks. Final supplemental Points/Premium/failure/flashcard/raw-database independence results follow below.
- Final fresh isolated matrix: 404 checks passed, zero failed, process exit 0 (baseline 5 + main learner runner 239 + public/OpenAPI 58 + timezone 14 + supplemental 88). Real checks cover Points outage/recovery, insufficient balance and pending cleanup, Premium/fallback/downgrade, flashcard streak without XP, a concurrent same-key debit against a five-Point wallet, all four live Prometheus counters, and all-column raw database comparisons proving web/mobile independence in both directions.
- Earlier harness evidence is retained separately: 45 passed/1 failed immediate-success assumption after the intended Access circuit opened, followed by 44 passing continuation checks; a later rerun had 76 passed/1 failed because it reused another learner's web request UUID and correctly received 409. The corrected final fresh run passed all 404 checks. No production fix or weakened assertion was used to resolve these fixture assumptions.
- Live timezone checks passed 14 assertions including Auckland settings roundtrip and a Honolulu activity date on November 6 while UTC was November 7. The detailed [live report](vocabulary-study-e2e-verification.md) records the complete commands, outcomes and media limit.
- Backend delivery is complete. Physical-device playback, non-null remote media availability and client fallback UI remain unverified; the API checklist leaves that item unchecked.
