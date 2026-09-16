---
date: 2026-09-16T08:42:00+07:00
scope: services/user-service
mode: read-only-assessment
---

# User Service Assessment

## Summary

`user-service` has a sensible domain/application/infrastructure separation and
clear auth, user-management, token, and profile-provisioning flows. It is not
safe to expose or deploy as currently configured: authorization is effectively
disabled, a predictable JWT fallback exists, and the supplied runtime config
does not provision a usable datasource for this service.

No files were changed and no tests were run.

## Findings

### Critical

1. **Every endpoint is publicly accessible.** `SecurityConfig` ends with
   `anyRequest().permitAll()` and controllers add no authorization checks.
   Unauthenticated callers can read, update, deactivate users, assign roles,
   and set account status. See
   `services/user-service/src/main/java/com/group01/user/config/SecurityConfig.java:23`.

2. **The JWT signer has a repository-visible default HS256 secret.** A missing
   deployment secret therefore permits forged tokens. See
   `services/user-service/src/main/java/com/group01/user/config/AuthTokenProperties.java:16`.

3. **Public registration accepts `ADMIN`.** A request-supplied role flows from
   `RegisterUseCase` into account creation; `RoleName` permits `ADMIN`,
   `DOCTOR`, and `PATIENT`. See
   `services/user-service/src/main/java/com/group01/user/application/usecase/RegisterUseCase.java:21`
   and `services/user-service/src/main/java/com/group01/user/domain/vo/RoleName.java:3`.

### High

4. **Direct access can spoof identity headers.** `/auth/me` and `/api/users/me`
   trust `CurrentUserHolder`, while this service does not validate a bearer
   token. Header-based identity is safe only with gateway-only ingress and a
   gateway that strips client-supplied `X-User-*` headers. See
   `services/user-service/src/main/java/com/group01/user/api/controller/AuthController.java:54`
   and `services/user-service/src/main/java/com/group01/user/application/usecase/GetMyProfileUseCase.java:19`.

5. **Registration is not a consistent distributed transaction.** It saves a
   user then synchronously provisions a doctor/patient profile. Doctor failures
   roll back locally; patient failures are logged and ignored. A remote profile
   can be orphaned, or an account can be returned without a patient profile.
   See `services/user-service/src/main/java/com/group01/user/application/usecase/RegisterUseCase.java:21`.

6. **Refresh-token rotation can race.** Two requests can validate the same
   active token before either revokes it; neither the entity nor repository
   expresses optimistic/pessimistic locking or atomic consumption. See
   `services/user-service/src/main/java/com/group01/user/application/usecase/RefreshTokenUseCase.java:25`.

7. **The default service runtime is incomplete.** It imports config from port
   8889, but Compose exposes Config Server on 8888. No `user-service` config
   supplies datasource/auth/profile-client settings, and Compose does not run
   the service. See `services/user-service/src/main/resources/application.yml:8`
   and `docker-compose.yml:37`.

### Medium

8. **Outbound profile calls lack explicit timeout, retry, circuit breaker,
   service authentication, and correlation propagation.** Operational failures
   may be returned as a missing profile because non-404 exceptions become
   `Optional.empty()`. See
   `services/user-service/src/main/java/com/group01/user/infrastructure/profile/ProfileProvisioningRestClient.java:43`.

9. **Refresh-token migration history forces logout and weakens a password
   invariant.** V3 drops token storage; V4 recreates it. V3 also makes
   `password_hash` nullable, while V4 does not restore `NOT NULL`. See
   `services/user-service/src/main/resources/db/migration/V3__add_keycloak_user_mapping.sql:6`
   and `V4__restore_local_auth.sql:4`.

10. **Tests do not cover runtime integration.** The suite is primarily Mockito
    use-case tests plus a class-load test. There is no Flyway/Postgres,
    repository, concurrency, Config Server, HTTP-client, or Docker integration
    coverage.

## Positive Architecture Signals

- Domain repository interfaces are separated from JPA adapters.
- Commands are immutable records; use cases make user/auth flows readable.
- Schema and entity mappings largely align after V4; role loads use an entity
  graph to avoid mapper-triggered lazy-load failures.
- A structured exception handler and DTO validation cover several normal API
  paths.

## Recommendations

1. **Block deployment/exposure** until security is corrected: require JWT
   authentication, authorize operations by role/ownership, remove the fallback
   signing secret, and restrict direct ingress to the gateway.
2. **Split public and privileged account creation.** Public registration must
   assign only a safe default role; admin/staff creation needs an authorized
   endpoint/use case.
3. **Make profile provisioning reliable.** Choose an explicit outbox/saga or
   retry/compensation design; do not silently accept a failed patient profile.
4. **Make token consumption atomic.** Use optimistic locking, a conditional
   update, or a repository operation that consumes one active token exactly once.
5. **Make local startup real.** Provide user-service config (datasource, Config
   Server URI, profile-service addresses) and add a Compose service/health check.
6. **Add integration tests first** for security policy, role escalation,
   Flyway-to-JPA mapping, refresh concurrency, and profile-client failure modes.

## Unresolved Questions

- Is a network policy already preventing direct access to `user-service` outside
  the repository's Docker setup?
- Is self-service doctor registration intended, or should only staff create
  doctor accounts?
- Is an external identity provider migration intended, given the V3/V4 schema
  history?
