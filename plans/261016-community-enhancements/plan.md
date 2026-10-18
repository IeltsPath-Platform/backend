# Community enhancements

- Status: #6, #5, #1, #4 complete; #2 implementation and automated gates complete, browser verification pending. The user confirmed the real-browser scenarios for #6, #5, #1 and #4 passed.
- Baseline: [Community and realtime plan](../261016-1045-community-realtime-feed/plan.md).
- Selected order: #6 following feed, #5 PostgreSQL search, #1 comment mentions, #4 notification preferences, #2 content reports.
- Each slice must add forward-only database migration(s), domain/application/API behavior, focused tests, FE, CSS/JS-only motion where needed, and pass its gates before the next slice starts.
- No new FE test dependency until user approval. Until then, run lint/typecheck/build and manually verify each changed interaction in a browser with two demo accounts. Do not commit.

| Slice | Status | Gate |
|---|---|---|
| #6 Following feed | Complete | Mockito/use case, WebMvc, PostgreSQL cursor integration, FE checks; real-browser scenarios confirmed by user |
| #5 Full-text search | Complete | Mockito/use case, WebMvc, PostgreSQL Testcontainers (`simple`, bounded/plain-text input and cursor), FE page tests; browser scenarios confirmed by user |
| #1 Comment mentions | Complete | Mockito use case, WebMvc security/validation/status, PostgreSQL query and outbox/notification persistence, consumer redelivery idempotency, FE page test; browser scenarios confirmed by user |
| #4 Notification preferences | Complete | Default enabled; disabled event is neither stored nor pushed; PostgreSQL + RabbitMQ full outbox→relay→consumer verification for every event type; FE page-level and browser tests confirmed |
| #2 Content reports | Automated gates complete; browser verification pending | Unique reporter/target, no self-report; ADMIN-only queue and resolution; see [phase plan](phase-02-content-reports.md) |

See the main plan and per-slice reports for implementation and actual test counts.
