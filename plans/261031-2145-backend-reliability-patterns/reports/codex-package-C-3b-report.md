Phase: C (3b)
Status: DONE_WITH_CONCERNS
Commits:
- `850cf5c3` — test(resilience): preserve library not-found mapping
- `361d767f` — test(resilience): cover library client circuit handling
- `0c08d6f3` — feat(content): add resilience to library client
- `b6fd2788` — feat(assessment): add resilient downstream clients
- `4e8a2a2e` — feat(community): add resilient downstream clients
- `a62dab1f` — feat(game): use configured resilience clients
- `7c22e92a` — feat(learning): add resilient downstream clients
- `5a59ee8f` — feat(content): add resilient downstream clients
- `e349ec16` — fix(resilience): match isolated retry to production defaults
Files:
- Wrapped the remaining production `RestClient` adapters in assessment, community, game, learning, and content with `ResilientCall`; `rg` audit found 22 production sources and 0 without a resilience wrapper.
- Added focused tests for rubric/error-type 404 behavior and extended coverage around `RestUserDirectory` and `UserServiceStudyTimezones`.
Red tests (commit test): no new red-first commit in this continuation. The existing Library 404 test was intentionally green before implementation per the recorded Claude decision.
Verification:
- Assessment focused client/failure-window suite → 33 tests, 0 failures, 0 errors, 0 skipped.
- Community resilience client tests → 8 tests, 0 failures/errors/skips (previous focused run).
- Game client tests → 3 tests, 0 failures/errors/skips (clean focused run).
- Content library/client tests → 9 tests, 0 failures/errors/skips (previous focused run).
- Learning client-focused clean run → 78 tests, 0 failures, 0 errors, 0 skipped. It covers all learning RestClient test classes, including the corrected timezone and display-name retry expectations.
- `mvn -B -ntp -pl services/learning-service -am clean -DskipTests test-compile` → BUILD SUCCESS. The earlier 21 testCompile errors were stale target output and disappeared after a clean focused rebuild.
- `ResilientCallTest` → 14 tests, 0 failures, 0 errors, 0 skipped; includes original-exception rethrow after retry exhaustion.
- Full reactor `verify` was not run.
Deviations from skeleton: no new production dependency or client was added outside the existing 22-client table. C continuation did not have a red-first test commit. The display-name server-error test now expects two configured attempts, matching the production retry settings. LibraryVocabularySenseClient's approved 404 → 503 behavior was preserved; no 404 mapping was added.
Concerns: No full-reactor verify was run, per the user's instruction. Focused verification is being rerun after aligning adapter test constructors with `isolatedDefault()`. The two final wrappers preserve their existing fallbacks (empty display-name result and stored/default timezone).

Current focused verification (2026-11-02):
- Cross-service selection for C adapters → 143 tests, 0 failures, 0 errors, 0 skipped across content (19), learning (83, including the one phase-E correlation test), assessment (30), game (3), and community (8). Thus 142 were client/resilience tests.
- `WordlyWordClientTest` → 2 tests, 0 failures, 0 errors, 0 skipped. A local HTTP server verified retry success, two attempts on exhaustion, and propagation of the original `HttpServerErrorException`.
- `ResilientCallTest` → 14 tests, 0 failures, 0 errors, 0 skipped; includes retry-exhaustion behavior.
- Combined C-focused evidence: 158 tests, 0 failures, 0 errors, 0 skipped. Production source audit: 22 RestClient sources, 0 missing a `ResilientCall` wrapper.
