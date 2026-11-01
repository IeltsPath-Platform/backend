Phase: B2
Status: DONE_WITH_CONCERNS
Commits:
- 7b642c30 test(observability): cover outbox correlation and Prometheus access
- d4c374b8 test(observability): enable metrics in actuator tests
- ae3d084b test(observability): align reward correlation fixtures
- 9d6c81a1 test(observability): verify outbox correlation headers
- 4c8c08f6 feat(observability): propagate outbox correlation and DLQ metrics
Files:
- `shared/common-observability/src/main/java/com/ieltspath/commonobservability/Correlation.java`
- `shared/common-observability/src/main/java/com/ieltspath/commonobservability/DeadLetterMeterBinder.java`
- Assessment, Access, Community, and Learning outbox persistence, entities/mappers, publishers, and migrations `V21`, `V8`, `V9`, and `V41` for nullable `correlation_id`.
- Access, Learning, and Notification dead-letter `MeterBinder` configuration.
- Correlation persistence, relay, consumer, publisher, DLQ gauge, and Prometheus security tests in the affected modules.
- `services/learning-service/src/test/java/com/ieltspath/learning/LearningServiceApplicationTests.java` (updated the schema migration version assertion from 40 to 41).
- `docs/system-architecture.md`
- `plans/261031-2145-backend-reliability-patterns/reports/codex-package-B2-report.md`
Red tests (commit test):
- `CorrelationTest.currentReturnsMdcValue`, `CorrelationTest.currentIsNullOutsideRequest`, `DeadLetterMeterBinderTest.publishesOneGaugePerQueue`, `DeadLetterMeterBinderTest.reportsNaNWhenBrokerCountFails`, and the new persistence/Prometheus tests failed before the code commit because the helper, meter binder, migration columns, and correlation propagation did not exist.
- `RabbitAccessEventPublisherCorrelationTest.sendsHeaderWhenRowHasCorrelationId` failed when the publisher header was temporarily absent (`expected req-access, got null`).
Verification:
- `mvn -B -ntp -pl shared/common-observability,services/assessment-service,services/community-service,services/access-service,services/learning-service,services/notification-service -am verify` → BUILD FAILURE at `learning-service`; 1,347 tests reached, 1 failure, 0 errors, 3 skipped. Breakdown: common-events 19/0/0/0; common-security 9/0/0/0; common-storage 60/0/0/3; common-observability 22/0/0/0; access-service 99/0/0/0; learning-service 1,138/1/0/0. Assessment, notification, and community were skipped after Maven stopped.
- The sole full-suite failure is `BusinessControllerRoleConventionTest.businessEndpointsDoNotGrantSystemAdministratorAccess`, which rejects the pre-existing `DeadLetterAdminController` role annotation. It is the same baseline failure recorded for B1; no old test was disabled or weakened.
- Focused publisher verification (`RabbitAccessEventPublisherCorrelationTest`, `RabbitPointsGrantPublisherTest`, `RabbitOutboxEventPublisherCorrelationTest`, and `CommunityOutboxRelayIntegrationTest`) → BUILD SUCCESS; 15 tests, 0 failures/errors/skips. The Learning migration/persistence checks (`RewardGrantCorrelationPersistenceTest` and `LearningServiceApplicationTests`) → BUILD SUCCESS; 3 tests, 0 failures/errors/skips.
Deviations from skeleton: Updated the existing `LearningServiceApplicationTests` migration version expectation because the required V41 migration makes 40 stale; recorded as an existing-test change. Corrected the new persistence fixture to use the schema's accepted `BADGE` reason. Added `@AutoConfigureObservability` to the new actuator tests because Spring Boot disables observability in test contexts by default.
Concerns: Full reactor verification remains blocked by the unrelated pre-existing Learning controller role convention failure. Assessment, Notification, and Community were not reached by the final full command. Claude's requested manual stack check and approval remain outstanding.
