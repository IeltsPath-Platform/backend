Phase: B3 — retry routing reliability
Status: DONE_WITH_CONCERNS
Commits:
- `5ad92eee` — test(messaging): verify retry queue routes to source
- `424e71c4` — fix(messaging): route retries through source exchange
Files:
- `shared/common-events/src/main/java/com/ieltspath/events/messaging/RetryTopology.java`
- `shared/common-events/src/test/java/com/ieltspath/events/messaging/RetryTopologyTest.java`
Red tests:
- `RetryTopologyTest.retryQueueReturnsToTheSourceExchangeWithTheOriginalRoutingKey` — red because the retry queue was configured with an empty dead-letter exchange and queue-name routing key.
- `SubscriptionGrantedBrokerIntegrationTest.temporaryFailureIsRetriedThenSucceeds` was red before the production change: retry delivery used the wrong routing key and was rejected by the consumer contract check.
Verification:
- `common-events`: 20 tests, 0 failures, 0 errors, 0 skipped.
- Notification retry integration: 1 test passed.
- Access `LearningPointsGrantListenerFailureTest`: 5 passed.
- Learning `PointsGrantSettledListenerTest`: 4 passed.
- Access `DeadLetterReplayBrokerIntegrationTest`: 2 passed.
- Learning `LearningDeadLetterReplayIntegrationTest`: blocked by Testcontainers startup; RabbitMQ `3.13-management-alpine` did not emit the configured `Server startup complete` log before timeout. No assertion ran.
- Full `mvn -B -ntp verify`: stopped in learning-service at the same RabbitMQ container startup error. Partial reactor totals from generated Surefire XML: 1,858 tests, 0 failures, 1 error, 4 skipped; modules after learning were not reached.
Deviations:
- Production `RetryTopology` and its test were added to scope per Claude's decision because retry routing could not be corrected by the original test/docs-only B3 scope.
Concerns:
- Learning DLQ replay could not be verified in this environment due RabbitMQ Testcontainers log-wait timeout. The feature test needs a successful run in a Docker environment that satisfies its wait strategy.
