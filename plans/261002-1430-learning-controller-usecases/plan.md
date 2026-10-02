# Learning controller use cases

Status: completed

Complete the interrupted controller refactor using content-service as the reference:
one action per use case with `execute`, named controller dependencies, and response
DTOs in `api/dto/response`. Preserve HTTP/event contracts, security, transactions,
learner locks, grading, evidence and replay behavior. Existing DDD changes remain intact.

Steps:
1. Connect the four lesson use cases already created; split review and essay actions.
2. Update controllers, consumer callers and existing MVC/integration tests.
3. Run focused MVC tests, then all learning-service tests; review moved logic.

Acceptance: no callers of removed multi-action use cases; all application use cases
expose one `execute`; unchanged routes and JSON; tests pass (report Docker skips).

Risk: moving transaction boundaries or Writing grade visibility. Preserve annotations
and transaction templates; share the existing Writing view logic through an assembler.
Rollback: reverse only this task's edits, preserving the pre-existing working tree.

## Verification ? 2026-10-02

- Focused MVC tests: 32 passed.
- `mvn -q -pl services/learning-service -am test`: exit 0; learning-service 198 passed,
  common-security 8 passed; zero failures/errors/skips. PostgreSQL Testcontainers ran.
- Scoped review: no behavioral defects found; see [review](reports/review.md).
- Source scan: 12 use cases expose only `execute`; no removed-class references remain.
- Graphify updated successfully. No commit or runtime restart performed.
