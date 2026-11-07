# Vocabulary study final source review

Date: 2026-11-07 (Asia/Bangkok). Branch: `feat/mobile-vocabulary-study`.

## Review status

Phase-four source approved with no remaining concrete blockers in the reviewed scope. Final required Library clean passed. Final accepted live gateway verification passed 404 checks with zero failures; earlier exploratory harness failures remain recorded separately below. Physical-device media acceptance and branch PR CI remain outstanding.

## Verified changes

- V42 uses the existing education and environment catalog. On an untouched catalog, each topic receives three eight-sense LEARN units and an eight-sense REVIEW unit reusing earlier words. Titles describe their curated contents; no existing catalog rows are changed or reactivated.
- The seed preflights each whole topic before inserting anything. It selects the primary active sense by `sort_order, id`, checks normalized Vietnamese meanings for per-unit duplicates and checks whole-word examples, then reuses the prepared sense IDs for insertion. An unavailable or invalid curated topic is skipped without blocking the migration or creating a partial demo curriculum.
- Seed units append after existing authored orders. Curriculum version becomes 1 on a fresh topic or increments once when seed content is added to an existing topic. Skipped topics retain their existing state.
- `StudyUnitSeedMigrationTest` checks the eight fresh units, author order, active senses, 24 distinct items per topic, publication validity, and stored metadata against the actual generator: eight CHOICE, two MATCH, eight FILL, and 18 graded steps per unit.
- `StudyUnitSeedUpgradeTest` covers deactivated catalog items, edited examples, inactive topics, duplicate edited meanings, authored order preservation, and existing/fresh curriculum versions. The implementation agent reported three failing upgrade regressions before the fix; the duplicate-meaning case was added afterward.
- `StudyMetrics` registers attempt and purchase counters after transaction commit. New mutations count; retries and rolled-back effects do not. Regression coverage includes start/resume, completion retry, replay, pending retry, and failed purchase finalization.
- The Access availability counter records one logical unavailable result after shared retries. Successful retry, 402/409 business outcomes, and ordinary 204 subscription responses do not count as availability failures. `StudyAccessMetricsTest` covers these boundaries.
- The corrected `StudyMeSecurityTest` fixture matches existing rules: a positive streak qualified today, without an owned freeze or consecutive-freeze limit, can buy a freeze for 10 points and has a null blocked reason. Removing the invented `QUALIFIED_TODAY` fixture value changes no production behavior.
- `scripts/verify-vocabulary-study.ps1` matches the public/authenticated endpoints and nested JSON contracts. It accepts a credential object, suppresses authentication bodies in errors, and outputs no token or password. PowerShell parsing completed with zero errors; separate live gateway evidence is recorded below.

## Review findings resolved

The original V42 could fail startup after supported catalog edits because it used strict lookups, and it could leave the curriculum version unchanged after adding published units. Whole-topic preflight, prepared sense reuse, conditional seeding, appended orders, and the version upsert resolve those findings. Preflight also protects the fixed generator metadata from edited examples or duplicate meanings.

The separate Access idempotency dependency fix was reviewed and committed with its own evidence: [access-idempotency-review.md](access-idempotency-review.md). The accepted window for two heart refills with different request IDs remains unchanged.

## Verification evidence

- Parent-confirmed final required Library command: `$env:JAVA_HOME='D:\jdk-21'; mvn.cmd -pl services/library-service -o clean test` — 407 tests, zero failures/errors/skips, exit 0, 8m49s, completed 2026-11-07 at 11:37:53 Asia/Bangkok. Surefire forced JVM exit after its 30-second shutdown wait after tests completed; the build exited successfully. Earlier phase-three Library baseline was 394 passing tests.
- Gateway clean: 58 tests, zero failures/errors/skips. Access clean: 112 tests, zero failures/errors/skips, exit 0, 5m55s. The Access report retains the earlier RED and corrected database-timestamp precision assertion evidence.
- Parent-confirmed final focused phase-four run: 38 tests passed, zero failures, errors, or skips, in 2m19s. Coverage comprises parent client tests (14), Access metrics (5), gamification (13), fresh seed (2), and seed upgrade (4).
- Final accepted live Gateway verification: 404 HTTP/DB assertions passed, zero failed, exit 0. Components are fresh raw-state baseline 5, main learner flow 239 (9.760s, XP 50 and streak 1), OpenAPI/public/media 58, timezone 14, and the corrected complete supplemental run 88. Details: [live E2E verification](vocabulary-study-e2e-verification.md). The earlier main runner took 19.327s; that duration is not attributed to the final fresh run.
- Earlier exploratory supplemental runs are separate from that accepted total: initially 45 passes and one harness assumption failure, followed by 44 continuation passes; another attempt recorded 76 passes and one web-fixture failure from reusing a global submission UUID, returning 409 `REQUEST_CONFLICT`. Neither is represented as an all-green run. The accepted supplemental run passed all 88 checks; its first freeze call returned 200.
- Live supplemental evidence covers all four Prometheus counters, Premium capacity 7/downgrade 5/failure fallback, real flashcard streak with zero XP, a concurrent same-key debit against an actual five-Point wallet charging once, and an actual web objective with all-column raw database comparisons proving independence in both directions.
- The first diagnostic post-recovery freeze returned 503, but its JSON body was not retained; do not claim its exact error code was directly observed. Archived evidence shows the unavailable counter and PENDING purchase, then a same-key 200 retry reaching DONE with one Access ledger debit, proving recovery after the intentionally opened Access circuit. This is distinct from the accepted run's first freeze call returning 200.
- Non-null remote media/device rendering and client fallback UI remain unverified. No reconciliation job is included; old PENDING purchases require manual inspection. The accepted different-request-ID refill charge window is unchanged.
- Branch PR CI has not yet run. Community PR 154's separately observed Compose success and reactor cancellation are not evidence of a green vocabulary-branch CI run.
- `git diff --check` passed during review. No Maven runs, production edits, index changes, or commits were performed by the reviewer for this pass.
- PR-preparation scope check covered the full branch against `origin/main` plus working additions: 120 paths, limited to Library, three Access fix files, Gateway/configuration, supporting docs and the smoke script. A scan of 4,969 added lines found no high-risk credential literals or private-file candidates; mock JWT strings and Testcontainers credential access are test references. No dotenv or runtime credential files were read. Recheck after the final changes before publishing.
- Final documentation-inclusive scan covered 122 paths and 5,121 added lines with zero high-risk credential literals/private-path candidates; tracked diff check and untracked whitespace checks passed. The new live report exists and its final timings, failure history and artifact provenance were reviewed. Only report/journal wording changed afterward; no production inputs changed.
