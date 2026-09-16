---
phase: 2
title: "Implementation updates"
status: pending
priority: P1
dependencies: [1]
---

# Phase 2: Implementation updates

## Overview

Update JWT creation and validation paths so external and internal tokens only carry `iss`, `sub`, `exp`, and `roles`.

## Requirements

- Functional: Login returns simplified external access token.
- Functional: Gateway signs simplified internal token.
- Functional: Gateway and user-service still reject invalid signatures, wrong issuer, invalid subject, expired token, and unsupported roles.
- Non-functional: Keep current HMAC HS256 setup and current refresh-token rotation.

## Architecture

Runtime flow after this phase:

```text
user-service login
  -> external JWT: iss/sub/exp/roles
  -> client
  -> gateway verifies external secret + issuer + sub + roles + exp
  -> gateway signs internal JWT: iss/sub/exp/roles
  -> user-service verifies internal secret + issuer + sub + roles + exp
```

## Related Code Files

- Modify: `services/user-service/src/main/java/com/group01/user/application/usecase/JwtTokenService.java`
- Modify: `services/user-service/src/main/java/com/group01/user/config/SecurityConfig.java`
- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/SecurityConfig.java`
- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/InternalJwtService.java`
- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/InternalJwtGatewayFilter.java`

## Implementation Steps

1. In `JwtTokenService.createAccessToken()`:
   - Keep `issuer`.
   - Keep `subject`.
   - Keep `expiresAt`.
   - Keep `roles`.
   - Remove `.audience(...)`.
   - Remove `.issuedAt(...)`.
   - Remove `.id(...)`.
   - Remove `.claim("type", "access")`.
   - Remove `.claim("email", ...)`.
   - Remove `.keyId(...)` from the `JwsHeader`.
2. In gateway `InternalJwtService.createToken()`:
   - Remove the `audience` parameter if it is only used as JWT `aud`.
   - Keep `issuer`.
   - Keep `subject`.
   - Keep `expiresAt`.
   - Keep `roles`.
   - Remove `.audience(...)`.
   - Remove `.issuedAt(...)`.
   - Remove `.id(...)`.
   - Remove `.claim("type", "gateway-internal")`.
   - Remove optional `email` forwarding.
   - Remove `.keyId(...)` from the `JwsHeader`.
3. In gateway `SecurityConfig.jwtDecoder()`:
   - Keep default expiration validation.
   - Keep issuer validation.
   - Remove `audienceValidator(...)`.
   - Remove `tokenTypeValidator(...)`.
   - Keep `subjectValidator()`.
   - Keep `canonicalRolesValidator()`.
4. In user-service `SecurityConfig.jwtDecoder()`:
   - Keep default expiration validation.
   - Keep issuer validation.
   - Remove `audienceValidator(...)`.
   - Remove `tokenTypeValidator(...)`.
   - Keep `subjectValidator()`.
   - Keep `canonicalRolesValidator()`.
5. Delete now-unused private validator methods and imports.
6. Keep authority conversion from `roles` to `ROLE_*` unchanged.

## Success Criteria

- [ ] Newly issued external JWT decodes to only `iss`, `sub`, `exp`, `roles`.
- [ ] Newly issued internal JWT decodes to only `iss`, `sub`, `exp`, `roles`.
- [ ] Gateway still rejects unsupported roles and malformed subjects.
- [ ] User-service still rejects unsupported roles and malformed subjects.
- [ ] External token cannot authenticate directly to user-service because it uses the wrong secret/issuer.

## Risk Assessment

Removing `type` shifts token-domain separation to secret and issuer. This is acceptable only if external and internal secrets remain different and are never collapsed into one shared `JWT_SECRET`.
