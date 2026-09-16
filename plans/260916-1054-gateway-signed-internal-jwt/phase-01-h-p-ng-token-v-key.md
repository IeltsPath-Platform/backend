---
phase: 1
title: "Hợp đồng token và key"
status: completed
priority: P1
dependencies: []
---

# Phase 1: Hợp đồng token và key

## Overview

Tách rõ hai trust domain: external token cho client và internal token cho
downstream. Phase này tạo contract cấu hình/key trước khi đổi behavior runtime.

## Requirements

- Functional: external token chỉ dùng ở API Gateway; internal token chỉ dùng
  giữa gateway và downstream.
- Non-functional: không lưu private key trong Git/config repo; hỗ trợ `kid` và
  current+previous public keys để rotate không downtime.

## Architecture

External access token do auth/user-service phát hành:

```text
iss=urn:code-base:auth
aud=api-gateway
type=access
sub=<UUID>
roles=[ADMIN|LEARNER]
alg=RS256 or PS256
```

Internal JWT do API Gateway ký:

```text
iss=urn:code-base:api-gateway
aud=<downstream service id, ví dụ user-service>
type=gateway-internal
sub=<UUID>
roles=[ADMIN|LEARNER]
iat, exp<=60s, jti, kid
```

JWKS/public key phải có nguồn tin cậy. Ưu tiên cấu hình secret-mounted/key-store
hoặc internal JWKS endpoint chỉ chứa public keys. Config Server không giữ
private key.

## Related Code Files

- Modify: `services/user-service/src/main/java/com/group01/user/config/AuthTokenProperties.java`
- Modify: `services/user-service/src/main/java/com/group01/user/security/JwtTokenService.java`
- Modify: `services/user-service/src/main/java/com/group01/user/config/SecurityConfig.java`
- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/AuthProperties.java`
- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/SecurityConfig.java`
- Modify: `infra/config-server/config-repo/api-gateway.yaml`
- Modify: `infra/config-server/config-repo/user-service.yaml`
- Modify: `docker-compose.yml`
- Create: key/JWKS property classes as needed, following existing config style.

## Implementation Steps

1. Split config names so external issuer/audience and internal issuer/audience
   cannot accidentally reuse the same secret.
2. Replace shared HMAC `jwt-secret` contract with asymmetric key material:
   private signer config only where token is minted, public verification config
   where token is consumed.
3. Set external access token `aud=api-gateway`; keep canonical roles
   `ADMIN`/`LEARNER` only.
4. Define internal JWT TTL default around 60 seconds and max allowed TTL no more
   than 5 minutes.
5. Add `kid` support and public key rotation shape: current and previous keys
   accepted by verifiers, only current key signs new tokens.
6. Remove duplicated `JWT_SECRET` injection between gateway and user-service in
   config/compose. Replace with explicit external/internal key env names.

## Success Criteria

- [x] Config cannot boot with a missing signing private key where signing is required.
- [x] Gateway has public verification material for external access tokens.
- [x] User-service has public verification material for gateway internal JWTs.
- [x] External and internal token claim contracts are documented in properties/tests.
- [x] No runtime config still implies one shared `JWT_SECRET` for all services.

## Outcome

- External access tokens now use gateway audience and asymmetric key config.
- Gateway internal JWT config is separate from external client JWT config.
- `JWT_SECRET` sharing was removed from config repo and compose.

## Risk Assessment

Main risk is breaking all auth at once. Mitigation: introduce properties and
decoders first, keep temporary compatibility only at Gateway for existing
external client tokens, and never add compatibility that lets downstream trust
raw user headers.
