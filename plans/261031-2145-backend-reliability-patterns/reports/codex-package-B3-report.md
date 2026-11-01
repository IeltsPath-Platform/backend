Phase: B3
Status: NEEDS_CONTEXT
Commits:
- 84c9b70f test(messaging): cover grant retry and rollback
- cd8c6f17 docs(messaging): document subscription grant event flow
Files:
- `services/access-service/src/test/java/com/ieltspath/access/infrastructure/messaging/AccessOutboxRelayIntegrationTest.java`
- `services/notification-service/src/test/java/com/ieltspath/notification/infrastructure/SubscriptionGrantedBrokerIntegrationTest.java`
- `docs/system-architecture.md`
Red tests (commit test):
- `SubscriptionGrantedBrokerIntegrationTest.temporaryFailureIsRetriedThenSucceeds` — after the first transient DB error, the retry queue returns the message to the main queue using the queue name as the received routing key. The consumer validates that routing key against the event type, rejects the retry as a contract violation, and dead-letters it. The test times out with no notice stored.
Verification:
- Focused notification red test: `mvn -B -ntp -pl services/notification-service -am -Dtest=SubscriptionGrantedBrokerIntegrationTest#temporaryFailureIsRetriedThenSucceeds -Dsurefire.failIfNoSpecifiedTests=false test` → 1 test, 1 failure, 0 errors, 0 skipped.
- Focused rollback test: `mvn -B -ntp -pl services/access-service -am -Dtest=AccessOutboxRelayIntegrationTest#rollbackLeavesNoOutboxRow -Dsurefire.failIfNoSpecifiedTests=false test` → 1 test, 0 failures, 0 errors, 0 skipped.
- Package command: `mvn -B -ntp -pl services/access-service,services/notification-service -am verify` → 224 tests, 1 failure, 1 error, 0 skipped. Access: 100 tests, all passed. The new retry test failed as above. Separate existing `CorrelationListenerBrokerIntegrationTest.listenerSeesTheCorrelationIdOfTheMessageAndClearsIt` errored because Spring `SimpleMessageConverter` does not accept `CommunityEvent`.
Deviations from skeleton:
- The spec names `SubscriptionGrantedListenerIntegrationTest`; the repository has `SubscriptionGrantedBrokerIntegrationTest`, so the required retry test was added to that existing broker integration class.
- No production files were changed. The remaining B3 scope only permits tests and documentation; fixing the retry routing key requires production messaging changes outside that remaining scope.
Concerns:
- Retry is not operationally correct until the message returns with its original contract routing key (or the consumer contract validation is redesigned). The architecture doc records this finding. This package must be resumed with explicit production-file scope before it can pass.
