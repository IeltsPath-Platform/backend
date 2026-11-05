Phase: C (3a)
Status: DONE_WITH_CONCERNS
Commits:
- b3e74685 test(resilience): cover learning content breaker behavior
- b60cbe63 test(resilience): cover retry breaker and bulkhead rules
- ab844447 test(resilience): cover gateway timeout and AI bulkhead
- 599078f8 feat(resilience): protect learning content and LLM calls
- 2d60317f test(resilience): expect safe client retries
Files:
- `shared/common-resilience/` (module, auto-configuration, `ResilientCall`, `CallKind`, exception, README, tests)
- `pom.xml`
- `services/learning-service/pom.xml`
- `services/learning-service/src/main/java/com/ieltspath/learning/infrastructure/client/RestLearningContentClient.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/infrastructure/client/OpenAiCompatibleLlmClient.java`
- `services/learning-service/src/main/java/com/ieltspath/learning/api/exception/GlobalExceptionHandler.java`
- learning resilience and AI bulkhead tests, plus the existing `RestLearningContentClientTest`
- `infra/config-server/config-repo/application.yaml`
- `infra/config-server/config-repo/api-gateway.yaml`
- `infra/api-gateway/src/main/java/com/ieltspath/apigateway/error/GatewayErrorAttributes.java`
- `infra/api-gateway/src/test/java/com/ieltspath/apigateway/error/GatewayTimeoutTest.java`
- `docs/system-architecture.md`
Red tests (commit test): `RestLearningContentClientResilienceTest.repeatedServerErrorsOpenBreakerThenNoHttpRequestIsMade` initially failed because the 11th 500 response still reached the downstream; the breaker integration was not yet implemented. Gateway/ResilientCall tests were also added in test-only commits before implementation.
Verification:
- `mvn -B -ntp -pl shared/common-resilience -am test` → 14 tests, 0 failures, 0 errors, 0 skipped.
- `mvn -B -ntp -pl infra/api-gateway -am -Dtest=GatewayTimeoutTest -Dsurefire.failIfNoSpecifiedTests=false test` → 4 tests, 0 failures, 0 errors, 0 skipped.
- `mvn -B -ntp -pl services/learning-service -am -Dtest=RestLearningContentClientResilienceTest,OpenAiCompatibleLlmClientTest -Dsurefire.failIfNoSpecifiedTests=false test` → completed successfully (learning focused resilience/client tests).
- `mvn -B -ntp -pl infra/api-gateway,services/learning-service -am verify` → API Gateway 50 tests, 0 failures/errors/skips; learning 1,144 tests, 4 failures, 0 errors/skips. Three failures were old `RestLearningContentClientTest` expectations that allowed only one response for retried 5xx/transport errors. After adjusting those expectations, `mvn -B -ntp -pl services/learning-service -am -Dtest=RestLearningContentClientTest -Dsurefire.failIfNoSpecifiedTests=false test` → 35 tests, 0 failures/errors/skips. The remaining full-suite failure was `BusinessControllerRoleConventionTest.businessEndpointsDoNotGrantSystemAdministratorAccess` for `DeadLetterAdminController`, pre-existing and outside this package.
Deviations from skeleton: Updated the existing `RestLearningContentClientTest` retry-response expectations after the full suite showed they contradicted the specified retry-on-5xx/transport behavior; committed separately as `2d60317f`. No tests were disabled or removed.
Concerns: The full package verification has one unrelated existing ADMIN-role convention failure. The exact reactor command will be run again as part of the final whole-reactor verification.
