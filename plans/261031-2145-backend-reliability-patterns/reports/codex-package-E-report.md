Phase: E
Status: DONE_WITH_CONCERNS
Commits:
- `3e79f7b5` — test(notification): cover graceful shutdown behavior
- `238f463f` — feat(resilience): configure graceful service shutdown
- Cross-service test and documentation updates remain uncommitted.
Files:
- `services/learning-service/src/test/java/com/ieltspath/learning/infrastructure/client/CorrelationAcrossServicesReliabilityTest.java`: added the phase 8 named scenario. It exercises the production content and assessment RestClients, JDBC reward outbox, reward service/relay job, and Rabbit publisher; a broker listener thread captures the shared correlation advice and log MDC.
- `services/community-service/src/test/java/com/ieltspath/community/infrastructure/CommunityOutboxRelayIntegrationTest.java`: complementary HTTP → community outbox → relay → consumer advice/log scenario.
- `docs/system-architecture.md`: reliability table maps both correlation tests and references the replica measurement report.
- `infra/api-gateway/src/test/java/com/ieltspath/apigateway/ratelimit/AuthRouteConsistencyTest.java`: inventory covers login, refresh, forgot-password, reset-password, registration, and default-tier logout.
Red tests (commit test): no new red-first commit in this continuation. Graceful-shutdown tests were committed test-first earlier. The cross-service test was added without production changes; it passed once the synthetic request JWT was installed in the test filter.
Verification:
- `CorrelationAcrossServicesReliabilityTest.sameCorrelationIdInHttpAndConsumerLogs` → 1 test, 0 failures, 0 errors, 0 skipped. It verified both downstream HTTP requests carried the original ID, the PostgreSQL outbox retained it, the production relay/publisher sent it through RabbitMQ, and consumer MDC/log contained it.
- `CommunityOutboxRelayIntegrationTest` → 6 tests, 0 failures, 0 errors, 0 skipped.
- `AuthRouteConsistencyTest` → 2 tests, 0 failures, 0 errors, 0 skipped.
- Graceful shutdown and scheduler tests → 3 tests, 0 failures, 0 errors, 0 skipped (previous focused run).
- Initial isolated Compose load check used synthetic credentials and did not read `.env`: 967 requests completed in 59.94 seconds (16.12 requests/second), all HTTP 200; 0 HTTP 5xx, transport failures, or 429. During the 3.98-second graceful stop of `learning-service-2`, 64 requests started and all returned 200; `learning-service-1` remained healthy.
- Follow-up drain check targeted `learning-service-2` directly through its dynamic localhost port. A read request was confirmed active and waiting on `study_tracking_policy` (`pg_stat_activity`: `wait_event_type=Lock`, `wait_event=relation`) before the stop signal. After SIGTERM, the test-only DB lock was released; the in-flight request completed with HTTP 200 before Docker reported the replica stopped. Stop signal: `2026-11-01T17:54:33.7696973Z`; stop completed: `2026-11-01T17:54:57.8000135Z` (24.03 seconds). The remaining replica returned HTTP 200 from health and the study-time endpoint; Eureka reported one learning instance.
- Both isolated Compose projects used for these measurements, their containers/volumes/networks, and temporary env/override/measurement files were removed; Docker checks found no remaining project-labeled resources.
- No whole-reactor `mvn verify` was run.
Deviations from skeleton: The learning correlation test uses a test-only HTTP route to compose production clients and outbox components; content and assessment are simulated through `MockRestServiceServer`, while PostgreSQL and RabbitMQ are Testcontainers. The first load check reached 16.12 requests/second, below the separate phase 4 target of 20 requests/second.
Concerns: The replica-stop drain proof uses a read-only endpoint, so it does not by itself assert a write was not duplicated during that exact stop. Idempotent state and duplicate delivery are separately covered by D tests (`UnlockCourseFailureWindowTest.retryWithSameKeyWhilePendingDoesNotCreateIntent` and `RewardGrantRelayIntegrationTest.secondDeliveryHasNoSideEffect`). No single test combines a process stop with a duplicate write; E remains `DONE_WITH_CONCERNS` for that boundary.
