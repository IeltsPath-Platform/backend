---
title: "Simplify JWT claims"
description: "Reduce external and internal JWT payloads to issuer, subject, expiration, and roles only; remove audience, key id, token type, email, iat, and jti claims."
status: complete
priority: P1
branch: "main"
tags: [auth, security, api-gateway, user-service, hmac]
blockedBy: [260916-1157-switch-jwt-rsa-to-hmac]
blocks: [260916-1054-gateway-signed-internal-jwt]
created: "2026-09-16T11:29:57.398Z"
createdBy: "ck:plan"
source: skill
---

# Simplify JWT claims

## Overview

Implement the user's simplified JWT contract:

```json
{
  "iss": "urn:code-base:auth",
  "sub": "user-id",
  "exp": 1760003600,
  "roles": ["LEARNER"]
}
```

The system keeps the current trust boundary:

```text
Client external JWT -> API Gateway verifies external HMAC JWT
                    -> Gateway strips client identity headers
                    -> Gateway signs short-lived internal HMAC JWT
                    -> user-service verifies internal HMAC JWT
```

But both token types become claim-minimal:

- External token: `iss=urn:code-base:auth`, `sub=<user UUID>`, `exp`, `roles`.
- Internal token: `iss=urn:code-base:api-gateway`, `sub=<user UUID>`, `exp`, `roles`.
- No `aud`, no `kid`, no `type`, no `email`, no `iat`, no `jti`.

External and internal tokens stay separated by different HMAC secrets and different issuers.

## Scope Challenge

- Existing code: token issuing, gateway verification, internal signing, downstream verification, refresh-token rotation, and role validators already exist.
- Minimum changes: remove audience/key-id/type/email/issued-at/jti from token creation, validators, properties, env config, tests, and README.
- Complexity: touches auth config in `user-service`, `api-gateway`, tests, compose, and docs. No new service/class is needed.
- Selected mode: scope reduction / fast execution plan, because the desired token contract is intentionally simpler.

## Design Decisions

1. Keep separate secrets:
   - `EXTERNAL_JWT_SECRET`: user-service signs external access tokens; gateway verifies them.
   - `GATEWAY_INTERNAL_JWT_SECRET`: gateway signs internal tokens; user-service verifies them.
2. Keep issuer validation:
   - Gateway accepts only `iss=urn:code-base:auth`.
   - User-service accepts only `iss=urn:code-base:api-gateway`.
3. Remove audience validation completely.
4. Remove token type validation completely.
5. Remove JWT key IDs. Since HS256 uses one configured secret per token domain, `kid` is not useful until key rotation exists.
6. Gateway still needs to know which route prefixes require internal token signing. Replace `internalAudiences` with a route-prefix list such as `internalJwtPaths` or `internalProtectedPaths`.
7. Do not change refresh-token behavior. Refresh tokens remain opaque random strings stored as hashes.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Contract cleanup](./phase-01-contract-cleanup.md) | Complete |
| 2 | [Implementation updates](./phase-02-implementation-updates.md) | Complete |
| 3 | [Verification and docs](./phase-03-verification-and-docs.md) | Complete |

## Dependencies

| Relationship | Plan | Reason |
|---|---|---|
| Blocked by | [260916-1157-switch-jwt-rsa-to-hmac](../260916-1157-switch-jwt-rsa-to-hmac/plan.md) | This plan assumes the current HMAC setup is the baseline. |
| Blocks / supersedes part of | [260916-1054-gateway-signed-internal-jwt](../260916-1054-gateway-signed-internal-jwt/plan.md) | That plan still documents `aud`, `type`, and `jti`; this plan replaces that token-claim contract. |

## Acceptance Criteria

- [x] External access token payload contains only `iss`, `sub`, `exp`, and `roles`.
- [x] Internal gateway token payload contains only `iss`, `sub`, `exp`, and `roles`.
- [x] JWT headers no longer set `kid`; headers rely on default `alg=HS256`.
- [x] Config no longer requires `externalJwtAudience`, `externalKeyId`, `internalJwtAudience`, or `internalKeyId`.
- [x] Gateway no longer validates `aud` or `type` on external tokens.
- [x] User-service no longer validates `aud` or `type` on internal tokens.
- [x] Gateway route-to-internal-token behavior still works without audience values.
- [x] Role validation still rejects empty/unsupported roles.
- [x] Subject validation still requires UUID.
- [x] Tests and README match the simplified contract.

## Not In Scope

- No OAuth Google login.
- No key rotation.
- No asymmetric JWT keys.
- No refresh-token redesign.
- No new authorization model beyond `ADMIN` and `LEARNER`.

## Verification Commands

Start narrow:

```powershell
mvn -q -pl services/user-service,infra/api-gateway -am test
```

If that is too slow during implementation, use focused module tests first:

```powershell
mvn -q -pl services/user-service test
mvn -q -pl infra/api-gateway test
```

## Risks

- Removing `aud` means a future downstream service cannot reject a gateway token based on intended recipient. Mitigation: keep internal secret distribution tight and revisit `aud` if more services are added.
- Removing `type` means token domains rely on separate secrets plus issuer validation. Mitigation: never reuse external and internal secrets.
- Removing `kid` makes key rotation harder. Mitigation: acceptable for the current one-secret-per-domain setup; add `kid` back only when rotation is implemented.
