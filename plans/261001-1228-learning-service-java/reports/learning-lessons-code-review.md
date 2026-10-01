## Code Review Summary

### Scope

- Branch: `feat/learning-lessons`; baseline `9d6f192`.
- Files: new Learning Service API/controller/DTOs, application use cases/ports/results, five domain services and value objects, REST Content adapter, JDBC progress adapter, their tests; modified exception handler and module README.
- LOC: approximately 2,611 new nonempty Java lines, excluding README and the modified handler.
- Focus: approved phase 2 and the six learner routes. Content readers, topic update API, shared authentication, V1 schema and runtime configuration were checked as affected dependencies.
- Protocol: scout edge cases first, then contract compliance, then production risks. Review was read-only; the lead applied corrections.

### Overall Assessment

No remaining implementation blocker found after the two corrections below. Fresh compiled MVC and PostgreSQL runs pass 39/39 tests, with no failures, errors or skips. The test owner subsequently completed the required plain full regression: 125 tests (Learning 117, common-security 8), zero failures, errors or skips.

### Critical Issues

No open critical issue.

Closed: implicit path-variable names prevented valid requests reaching four routes. The module does not enable compiler `-parameters`; Spring reported missing reflection parameter names, and the generic invalid-input handler converted that server binding error to 422. `services/learning-service/src/main/java/com/group01/learning/api/controller/LessonLearningController.java:35` (also lines 40, 45, 51) now names every `id` and `blockId` explicitly. Fresh MVC evidence is 27 tests, zero failures/errors/skips.

### High Priority

No open source defect.

Closed with fresh compiled PostgreSQL evidence: shared catalog upserts followed Content topic order. Two users receiving opposite orders could acquire catalog row locks in opposite orders and deadlock, aborting one request with 500. The new `concurrentUsersCanRefreshSharedCatalogInOppositeContentOrders` PostgreSQL test reproduced it. `services/learning-service/src/main/java/com/group01/learning/infrastructure/persistence/JdbcLearningProgressStore.java:62` now sorts catalog rows by KP UUID before batch upsert; this preserves snapshot semantics and gives all transactions one row-lock order. The initial failure used pre-fix bytecode. Fresh compilation includes `Stream.sorted`; all 12 PostgreSQL tests now pass, including opposite-order concurrent refresh.

### Medium Priority

No current phase-2 blocker. Catalog freshness is snapshot-based: an older delayed Content response may overwrite a newer shared snapshot. The approved plan promises upsert per topic refresh and per-user serialization, not globally latest Content revisions. MVP uses seed lessons and has no lesson authoring API; a global lock across Content HTTP would add behavior outside this acceptance scope. Treat live content revision/versioning as future work, not a requested fix.

### Low Priority

No style-only findings raised.

### Edge Cases Found by Scout

- Shared-catalog row-lock inversion: reproduced; source corrected as described above.
- Duplicate request replay precedes gates and Content reads: saved original response is returned, including original `lessonCompleted` and solution visibility, even after pending review insertion.
- Global request-id conflicts across different users: database uniqueness plus `ON CONFLICT DO NOTHING` prevents duplicate submission. A losing transaction throws 409 and rolls back its tentative evidence/progress; another user's saved response is not returned.
- Same-user writes: all writing entry points first acquire the user transaction advisory lock. JDBC calls use the Spring-bound connection; dependent topic refresh joins the existing transaction.
- First submissions: evidence is inserted only before any prior block submission exists. First-response selection uses insertion `clock_timestamp()` after lock acquisition; mastery uses `ordinal`, not potentially equal timestamps.
- Completion/review rollback: integration tests inject a review-insert failure and assert submission, passed-block state and completion roll back together.
- Unknown KPs: one batched catalog refresh is attempted, gate status is rechecked afterward, remaining unknown KP mappings are skipped with id-only logging.
- Removed topics: their sequence becomes NULL and the pure status derivation omits them. Lesson/question authoring and content revision snapshots are outside the current API scope; block-ID solution grants and current Content mapping therefore remain an MVP versioning limitation.
- Output trust boundary: learner question DTOs expose only `questionVersionId`, `sortOrder`, `stem`, `options`; null fill options are preserved. Failed submissions omit solutions; passed blocks expose separate solution records. No arbitrary Content object is serialized to the learner.
- Input/errors: missing, duplicate and foreign answers are rejected before writes; scalar answers accept only string/null. Dependency 404, transport/503, other HTTP failures, malformed JSON and missing body map to safe 404/503/502 errors. Verified JWT and a bounded correlation header are forwarded; upstream response bodies and credentials do not enter learner errors.
- Performance: repository/HTTP calls are outside loops; evidence, catalog and review writes are batched; mastery is one user-scoped query that retains the latest five ordinals while counting all evidence. Existing relevant indexes are present.

### Positive Observations

The domain is framework-free, and the adapters implement inward-facing application ports. V1, dependency declarations, bootstrap configuration, Gateway and shared security are unchanged by this implementation. UUIDv5 evidence references are deterministic and source-namespaced. The new PostgreSQL tests prove state and rollback behavior rather than only executing happy paths.

### Recommended Actions

1. Full module Maven regression is recorded in [learning-lessons-verification.md](./learning-lessons-verification.md); fresh focused MVC/PostgreSQL verification is also green.
2. Phase-2 success criteria are complete: the six routes, five domain laws, all 27 answer vectors, no N+1, transaction semantics and learner allowlist are present and checked.
3. Keep global snapshot freshness, lesson content versioning and whole-repository documentation migration with their approved future scope. Review API/test assignment/consumer remain later phases.

### Metrics

- Domain tests: 52 passing, including all 27 shared answer vectors and unchanged Lan rounded expectations (actual mastery 0.7285714285714285 / 0.875 / 0.48717948717948717).
- Content adapter tests: 23 passing.
- Fresh MVC tests: 27 passing; existing security test: 2 passing.
- PostgreSQL integration: 12 passing after fresh compilation; the added opposite-order catalog test reproduced the old deadlock and passes with sorted writes.
- Context/Flyway: 1 passing in available artifact; Docker tests were executed, not skipped, in these artifacts.
- Type coverage/test coverage percentages: not measured; no project coverage gate.
- Lint: no configured project linter. No lint claim made.
- Verification source: inspected source, compiled annotations/bytecode and Surefire artifacts generated by the test owner. This reviewer did not run competing Maven processes.

### Unresolved Questions

No product decision or implementation fix remains. Full module regression completed successfully.
