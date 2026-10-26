# Writing criterion reviews

Status: done with concerns — all tests pass; RabbitMQ teardown reaches Surefire fork shutdown timeout.

Scope: learning-service only. Counted lesson and lesson Practice essays may create optional reviews for failed rubric criterion knowledge points. Free writing, revisions and judged sub knowledge points are excluded.

- [x] Extend evidence outcome and add a pure selection rule with domain tests.
- [x] Share batched review creation between Practice and lesson essay transactions; add V35.
- [x] Add the compatible `weak` basket group and response mapping.
- [x] Verify gating, clearance and the full review lifecycle with stub Content/grading.
- [x] Run focused tests, the full learning-service suite, and review the contracts and layer imports.

Default maximum: two new reviews per counted submission, configurable using `learning.writing.criterion-review-limit`. User and mentor should confirm this default before release. No commit requested.

Report: [implementation-report.md](reports/implementation-report.md).
