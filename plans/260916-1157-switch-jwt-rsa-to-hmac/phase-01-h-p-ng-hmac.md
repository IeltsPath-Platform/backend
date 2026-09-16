---
phase: 1
title: "Hợp đồng HMAC"
status: completed
priority: P1
dependencies: []
effort: "small"
---

# Phase 1: Hợp đồng HMAC

## Overview

Chốt contract HMAC trước khi sửa code để không vô tình quay lại một secret dùng
chung toàn hệ thống. Phase này đổi tên properties/env và xác định secret nào
được dùng ở service nào.

## Requirements

- Functional: external JWT và internal JWT đều dùng `HS256`.
- Non-functional: không commit secret; secret tối thiểu 32 bytes random; không
  dùng một `JWT_SECRET` chung cho mọi service.

## Architecture

Token contract giữ nguyên claim hiện tại, chỉ đổi algorithm:

```text
External JWT:
  alg=HS256
  secret=EXTERNAL_JWT_SECRET
  signer=user-service
  verifier=api-gateway
  aud=api-gateway
  type=access

Internal JWT:
  alg=HS256
  secret=GATEWAY_INTERNAL_JWT_SECRET
  signer=api-gateway
  verifier=user-service
  aud=user-service
  type=gateway-internal
```

Không đưa `GATEWAY_INTERNAL_JWT_SECRET` cho frontend hoặc service không cần
verify internal JWT.

## Related Code Files

- Modify: `services/user-service/src/main/java/com/group01/user/config/AuthTokenProperties.java`
- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/AuthProperties.java`
- Modify: `infra/config-server/config-repo/user-service.yaml`
- Modify: `infra/config-server/config-repo/api-gateway.yaml`
- Modify: `docker-compose.yml`
- Modify: `README.md`

## Implementation Steps

1. Trong `AuthTokenProperties`, thay:
   - `externalPrivateKey`, `externalPublicKey` -> `externalJwtSecret`
   - `internalPublicKey` -> `internalJwtSecret`
   - giữ `externalKeyId` nếu vẫn muốn gắn `kid` cho external token.
2. Trong gateway `AuthProperties`, thay:
   - `externalPublicKey` -> `externalJwtSecret`
   - `internalPrivateKey`, `internalPublicKey` -> `internalJwtSecret`
   - giữ `internalKeyId` nếu muốn gắn `kid` cho internal token.
3. Fail-fast nếu secret blank hoặc sau base64/raw decode không đủ mạnh.
4. Config server YAML đổi sang env:
   - `EXTERNAL_JWT_SECRET`
   - `GATEWAY_INTERNAL_JWT_SECRET`
5. Compose bỏ toàn bộ RSA key env, thay bằng hai secret trên.
6. README mô tả HMAC contract và cảnh báo không dùng lại cùng secret.

## Success Criteria

- [x] Property classes không còn yêu cầu RSA public/private key.
- [x] Config repo không còn `EXTERNAL_JWT_PUBLIC_KEY`, `EXTERNAL_JWT_PRIVATE_KEY`,
  `GATEWAY_INTERNAL_PRIVATE_KEY`, `GATEWAY_INTERNAL_PUBLIC_KEY`.
- [x] Có đúng hai secret domain: external và internal.
- [x] Plan implementation không tạo file test mới.

## Outcome

- `AuthTokenProperties` và gateway `AuthProperties` dùng HMAC secret thay cho
  RSA key material.
- Config repo và compose dùng `EXTERNAL_JWT_SECRET` cùng
  `GATEWAY_INTERNAL_JWT_SECRET`.

## Risk Assessment

HMAC dễ vận hành nhưng blast radius lớn hơn RSA: verifier có secret thì nếu bị
lộ có thể bị lợi dụng để ký token. Giảm rủi ro bằng cách tách secret external và
internal, secret đủ dài, không publish user-service ra host, và rotate khi nghi
ngờ lộ secret.
