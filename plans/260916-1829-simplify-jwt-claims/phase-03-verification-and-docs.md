---
phase: 3
title: "Verification and docs"
status: pending
priority: P1
dependencies: [1, 2]
---

# Phase 3: Verification and docs

## Overview

Align tests and documentation with the simplified JWT contract, then verify the gateway/user-service auth flow still works.

## Requirements

- Functional: Tests must assert the new minimal claims.
- Functional: Old audience/type/key-id expectations must be removed.
- Non-functional: Keep verification focused on affected modules first.

## Architecture

Test coverage should protect these flows:

```text
login token -> gateway accepted
external token -> user-service rejected
gateway internal token -> user-service accepted
wrong issuer -> rejected
bad subject -> rejected
unsupported role -> rejected
expired token -> rejected
```

Audience/type-specific rejection tests should be removed or rewritten because those claims are no longer part of the contract.

## Related Code Files

- Modify: `services/user-service/src/test/java/com/group01/user/api/controller/UserControllerSecurityTest.java`
- Modify: `services/user-service/src/test/java/com/group01/user/config/AuthTokenPropertiesTest.java`
- Modify: `infra/api-gateway/src/test/java/com/group01/apigateway/security/InternalJwtServiceTest.java`
- Modify: `infra/api-gateway/src/test/java/com/group01/apigateway/ApiGatewayApplicationTests.java`
- Modify: `README.md`

## Implementation Steps

1. Update test token builders:
   - Remove audience arguments.
   - Remove key-id arguments.
   - Remove type arguments.
   - Stop setting `iat`, `jti`, and `email` unless Spring requires issued-at in a specific test helper. If required by helper APIs, do not assert them as contract fields.
2. Replace tests named around wrong audience/type:
   - Keep wrong issuer tests.
   - Keep malformed/missing subject tests.
   - Add or keep expired token tests when practical.
   - Keep unsupported role tests.
3. Update gateway tests:
   - Assert internal token contains no `aud`.
   - Assert header contains no `kid`.
   - Assert route prefixes still trigger internal token signing.
4. Update README:
   - Show minimal external/internal JWT payloads.
   - Remove env vars for audience and key IDs.
   - Add note that this is a simplified contract and should revisit `aud` when more downstream services need strict token recipient binding.
5. Run focused tests.

## Success Criteria

- [ ] User-service tests pass.
- [ ] API gateway tests pass.
- [ ] README matches runtime token behavior.
- [ ] Grep finds no production references to removed config fields.
- [ ] Grep finds no test assertion requiring `aud`, `type`, or `kid`.

## Risk Assessment

Tests may currently encode old security expectations. Do not simply delete coverage; preserve equivalent protection through issuer, signature, expiration, subject, and role tests.
