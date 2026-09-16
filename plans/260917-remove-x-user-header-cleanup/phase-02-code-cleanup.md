---
phase: 2
title: "Code cleanup"
status: completed
priority: P1
dependencies: [1]
---

# Phase 2: Code cleanup

## Overview

Remove obsolete `X-User-*` identity header code while keeping the JWT-only
gateway-to-service flow unchanged.

## Requirements

- Functional: remove `headers.remove(SecurityHeaders.USER_ID/EMAIL/ROLES)` and
  all dead header-identity support classes/constants that are no longer used.
- Non-functional: do not weaken JWT validation; do not remove correlation id
  behavior.

## Architecture

After cleanup, the gateway request preparation should only remove/replace the
client `Authorization` header. Downstream services should continue to reject
requests without a valid gateway-signed internal JWT.

## Related Code Files

- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/filter/InternalJwtGatewayFilter.java`
- Modify: `shared/common-security/src/main/java/com/group01/commonsecurity/header/SecurityHeaders.java`
- Modify/delete: `shared/common-security/src/main/java/com/group01/commonsecurity/config/CommonSecurityAutoConfiguration.java`
- Delete: `shared/common-security/src/main/java/com/group01/commonsecurity/filter/CurrentUserHeaderFilter.java`
- Delete: `shared/common-security/src/main/java/com/group01/commonsecurity/currentuser/CurrentUser.java`
- Delete: `shared/common-security/src/main/java/com/group01/commonsecurity/currentuser/CurrentUserHolder.java`
- Modify: `services/user-service/src/test/java/com/group01/user/api/controller/UserControllerSecurityTest.java`

## Implementation Steps

1. In `InternalJwtGatewayFilter`, remove the `SecurityHeaders` import and the
   three `headers.remove(SecurityHeaders.USER_*)` lines.
2. Leave `headers.remove(HttpHeaders.AUTHORIZATION)` in place; gateway must
   still replace the client token with an internal JWT on protected paths.
3. Remove `USER_ID`, `USER_EMAIL`, and `USER_ROLES` from `SecurityHeaders`,
   keeping `CORRELATION_ID`.
4. Remove `CurrentUserHeaderFilter`, `CurrentUserHolder`, and `CurrentUser` if
   phase 1 confirms no runtime caller remains.
5. Update or delete `CommonSecurityAutoConfiguration` so it no longer registers
   a header-identity servlet filter. If it becomes empty, delete it and update
   any auto-configuration imports if present.
6. Update `UserControllerSecurityTest` so the "raw header cannot authenticate"
   scenario either uses literal legacy header strings or is replaced by a JWT
   absence test that expresses the same security invariant.
7. Run `rg` again to ensure no main runtime code references removed symbols.

## Success Criteria

- [x] `rg "SecurityHeaders\\.USER_|CurrentUserHeaderFilter|CurrentUserHolder|X-User-"` shows no main runtime references.
- [x] Gateway still creates internal JWT for configured internal paths.
- [ ] user-service still rejects unauthenticated requests and accepts valid
  internal JWTs.
- [x] No compile errors in affected modules.

## Risk Assessment

Risk: direct service access might appear "less sanitized" because gateway no
longer strips legacy headers. Mitigation: services must not read those headers
after cleanup; authentication remains JWT-only.
