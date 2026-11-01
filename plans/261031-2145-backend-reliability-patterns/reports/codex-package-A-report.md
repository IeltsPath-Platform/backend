Phase: Package A — Gateway auth routes and forwarded client IP
Status: DONE_WITH_CONCERNS
Commits:
- 76c62d40 test(gateway): cover auth routes and trusted proxy resolution
- 18d8ebc0 fix(gateway): trust forwarded client IP chain
Files:
- `infra/api-gateway/src/test/java/com/ieltspath/apigateway/ratelimit/ClientKeyResolverTest.java` (created)
- `infra/api-gateway/src/test/java/com/ieltspath/apigateway/ratelimit/AuthRouteConsistencyTest.java` (created)
- `infra/api-gateway/src/main/java/com/ieltspath/apigateway/ratelimit/ClientKeyResolver.java`
- `infra/api-gateway/src/main/java/com/ieltspath/apigateway/ratelimit/ClientRateLimitGatewayFilterFactory.java`
- `infra/api-gateway/src/test/java/com/ieltspath/apigateway/ratelimit/ClientRateLimitGatewayFilterFactoryTest.java`
- `infra/config-server/config-repo/api-gateway.yaml`
- `docs/system-architecture.md`
- `plans/261031-2145-backend-reliability-patterns/phase-03-api-reliability.md`
Red tests (test commit): `AuthRouteConsistencyTest` (2) failed because `/api/auth/*` remained and `/auth/reset-password` was missing; `ClientKeyResolverTest` (4) failed because forwarded IP resolution trusted the spoofable leftmost entry and did not fall back to the remote address when all hops were trusted. Test run: 6, failures: 6, errors: 0, skipped: 0.
Verification: `mvn -B -ntp -pl infra/api-gateway -am verify` → 55 tests (common-security 9, api-gateway 46), failures: 0, errors: 0, skipped: 0. Post-fix focused tests: 6, failures: 0, errors: 0, skipped: 0.
Deviations from skeleton: Updated the two existing gateway test request paths from `/api/auth/login` to the actual `/auth/login` endpoint, as required by the correction spec. Existing phase-3 deviations retained and recorded: `failsClosedWhenRedisIsDown` uses a mocked Redis connection failure; `sharesLimitAcrossTwoGatewayInstances` uses two limiters with real Redis Testcontainers. The current `checkAllowed` calls `getExpire` first, so the correction spec's conditional extra closed-port test was not required.
Concerns: The requested live-gateway reset-password check is reserved for Claude; no live stack or real Redis was used.
