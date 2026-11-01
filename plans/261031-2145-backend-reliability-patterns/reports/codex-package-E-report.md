Phase: E
Status: IN_PROGRESS
Commits:
- `3e79f7b5` — test(notification): cover graceful shutdown behavior
- `238f463f` — feat(resilience): configure graceful service shutdown
Files:
- `services/notification-service/src/test/java/com/ieltspath/notification/infrastructure/shutdown/GracefulShutdownTest.java`
- `services/notification-service/src/test/java/com/ieltspath/notification/infrastructure/shutdown/SchedulerShutdownTest.java`
- `infra/config-server/config-repo/application.yaml`
- `infra/config-server/config-repo/eureka-server.yaml`
- `infra/config-server/config-repo/api-gateway.yaml`
- `docker-compose.yml`
- `docs/system-architecture.md`
- `plans/261031-2145-backend-reliability-patterns/phase-08-test-loi-tong.md`
Red tests:
- `GracefulShutdownTest` and `SchedulerShutdownTest` were committed as test-only changes before the graceful shutdown implementation. The initial tests exposed missing explicit lifecycle and scheduler shutdown configuration.
Verification:
- Graceful shutdown and scheduler focused tests: 3 passed, 0 failures/errors/skips.
- `docker compose config --quiet` passed using a temporary placeholder env file; no `.env` was read.
- Full `mvn -B -ntp verify` stopped in learning-service: RabbitMQ Testcontainers failed its startup log wait in `LearningDeadLetterReplayIntegrationTest`. Partial reactor totals from generated Surefire XML: 1,858 tests, 0 failures, 1 error, 4 skipped.
Deviations:
- No production or Compose behavior outside the approved graceful shutdown/draining spec was changed.
Concerns:
- E is incomplete against phase 8: the new cross-service correlation, downstream timeout, Redis outage, DLQ replay, and remaining end-to-end reliability scenarios have not all been implemented or verified. The full reactor did not finish; packages after learning-service were not reached. Do not mark the overall plan complete.
