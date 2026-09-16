---
phase: 1
title: "Contract cleanup"
status: pending
priority: P1
dependencies: []
---

# Phase 1: Contract cleanup

## Overview

Define the simplified JWT contract in code-level terms before changing runtime behavior. This phase removes old assumptions from property shapes and token claim expectations.

## Requirements

- Functional: JWT payloads must be limited to `iss`, `sub`, `exp`, and `roles`.
- Functional: External/internal token separation must still exist via secret and issuer.
- Non-functional: Avoid new abstractions; update existing config records and validators.

## Architecture

Current contract:

```text
external: iss, sub, aud, iat, exp, jti, type, email, roles + kid
internal: iss, sub, aud, iat, exp, jti, type, email, roles + kid
```

Target contract:

```text
external: iss, sub, exp, roles
internal: iss, sub, exp, roles
```

Gateway still needs protected route prefixes for deciding when to issue internal JWTs, but those prefixes should not be called "audiences" anymore.

## Related Code Files

- Modify: `services/user-service/src/main/java/com/group01/user/config/AuthTokenProperties.java`
- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/AuthProperties.java`
- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/InternalJwtGatewayFilter.java`
- Modify: `docker-compose.yml`
- Modify: `README.md`

## Implementation Steps

1. Remove these user-service properties:
   - `externalJwtAudience`
   - `externalKeyId`
   - `internalJwtAudience`
2. Remove these gateway properties:
   - `externalJwtAudience`
   - `internalKeyId`
3. Replace gateway `internalAudiences: Map<String, String>` with a route-prefix list, for example:

   ```java
   List<String> internalJwtPaths
   ```

   Default values can remain:

   ```text
   /api/users
   /auth/me
   ```

4. Update env/config names in `docker-compose.yml`:
   - Remove `EXTERNAL_JWT_AUDIENCE`
   - Remove `EXTERNAL_JWT_KEY_ID`
   - Remove `INTERNAL_JWT_AUDIENCE`
   - Remove `GATEWAY_INTERNAL_KEY_ID`
5. Update README token examples and secret list.

## Success Criteria

- [ ] No production config class requires audience or key-id values.
- [ ] Gateway route matching no longer depends on audience values.
- [ ] README no longer documents `aud`, `type`, `email`, `iat`, `jti`, or `kid` as required token fields.

## Risk Assessment

The route-prefix list replaces audience mapping. If implemented carelessly, gateway may stop signing internal JWTs for protected downstream routes. Keep existing route prefixes and add focused tests around `/api/users` and `/auth/me`.
