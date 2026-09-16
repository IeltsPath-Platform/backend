---
phase: 3
title: "Downstream xác minh internal và đóng trust header"
status: completed
priority: P1
dependencies: [1, 2]
---

# Phase 3: Downstream xác minh internal và đóng trust header

## Overview

`user-service` chuyển từ verify external token sang verify internal JWT do
gateway ký. Common header identity cũ bị loại khỏi trust boundary.

## Requirements

- Functional: API protected của user-service chỉ accept internal JWT có
  `aud=user-service` và `type=gateway-internal`.
- Non-functional: service không còn dựa vào unsigned `X-User-*`; direct access
  tới service không được dùng như đường bypass gateway.

## Architecture

`user-service` vẫn cần security config vì mỗi service phải tự verify chữ ký
gateway, nhưng config này verify internal assertion thay vì external client
token. Gateway là ingress, downstream là verifier.

## Related Code Files

- Modify: `services/user-service/src/main/java/com/group01/user/config/SecurityConfig.java`
- Modify: `services/user-service/src/main/java/com/group01/user/UserServiceApplication.java`
- Modify: `services/user-service/pom.xml`
- Modify: `shared/common-security/src/main/java/com/group01/commonsecurity/*`
- Modify: `infra/config-server/config-repo/user-service.yaml`
- Modify: `docker-compose.yml`
- Search/update callers of `CurrentUserHolder`, `CurrentUserHeaderFilter`, and
  `SecurityHeaders.X_USER_*`.

## Implementation Steps

1. Change user-service resource server decoder to gateway internal JWT verifier:
   issuer `urn:code-base:api-gateway`, audience `user-service`,
   `type=gateway-internal`, UUID subject, canonical roles only.
2. Keep auth endpoints public where needed: login, refresh, logout/register as
   currently defined. Protected user APIs require internal JWT.
3. Remove `common-security` identity header filter from user-service runtime:
   delete the user-service dependency/import, or split correlation-only support
   so `CommonSecurityAutoConfiguration` cannot register `CurrentUserHeaderFilter`
   for user-service.
4. Delete or deprecate `CurrentUserHeaderFilter`/`CurrentUserHolder` if no
   runtime consumer remains. If another service still needs it later, require a
   signed internal token adapter instead of raw headers.
5. Remove host port exposure for `user-service` in local compose unless it is
   explicitly a debug profile. Production equivalent is network policy/security
   group: only gateway can call downstream HTTP.
6. Update downstream controllers/services to read identity from verified
   `JwtAuthenticationToken`/authorities, not from headers.

## Success Criteria

- [x] user-service rejects external access JWT even if it is otherwise valid.
- [x] user-service rejects unsigned headers without a valid internal JWT.
- [x] user-service rejects internal JWT with wrong issuer, audience, type,
  expiry, key id, algorithm, subject shape, or role.
- [x] No main runtime path in user-service reads `X-User-*` as identity.
- [x] user-service no longer imports `CommonSecurityAutoConfiguration` that can
  auto-register unsigned header identity.
- [x] Local config no longer advertises user-service as a normal public port.

## Outcome

- `user-service` no longer depends on `common-security` for runtime identity.
- Protected user APIs verify only gateway-signed internal JWTs.
- Local compose no longer publishes `user-service` directly to the host.

## Risk Assessment

Main risk is thinking "security belongs only in gateway" and removing verifier
logic from downstream. Mitigation: downstream must still verify gateway's
signature so a direct network call cannot impersonate a user by setting headers.
