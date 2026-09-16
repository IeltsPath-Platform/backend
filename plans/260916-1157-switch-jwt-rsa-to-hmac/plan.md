---
title: "Đổi JWT RSA sang HMAC"
description: "Thay RS256/RSA key bằng HS256/HMAC secret cho external và internal JWT, giữ nguyên kiến trúc Gateway-signed internal JWT."
status: completed
priority: P2
branch: "main"
tags: [auth, security, api-gateway, user-service, hmac]
blockedBy: [260916-1054-gateway-signed-internal-jwt]
blocks: []
created: "2026-09-16T04:58:57.513Z"
createdBy: "ck:plan"
source: skill
---

# Đổi JWT RSA sang HMAC

## Overview

Plan này đổi phần ký/verify JWT từ RSA sang HMAC vì team quen vận hành HMAC hơn.
Không đổi trust boundary đã thiết kế trước đó:

```text
Client external JWT -> API Gateway verify external HMAC JWT
                    -> Gateway strip client identity headers
                    -> Gateway sign internal HMAC JWT
                    -> user-service verify internal HMAC JWT
```

Quyết định chính:

- Vẫn giữ hai token domain riêng:
  - External token: client cầm, `aud=api-gateway`, `type=access`.
  - Internal token: gateway ký xuống downstream, `aud=user-service`,
    `type=gateway-internal`.
- Dùng hai secret khác nhau, không dùng lại một `JWT_SECRET` chung:
  - `EXTERNAL_JWT_SECRET`: user-service ký external access token, gateway verify.
  - `GATEWAY_INTERNAL_JWT_SECRET`: gateway ký internal JWT, user-service verify.
- Dùng `HS256` trước vì đơn giản và quen thuộc. Secret phải đủ mạnh:
  tối thiểu 32 bytes random, lưu dạng base64/env secret, không commit.
- Không tạo file test mới theo yêu cầu. Verify chính bằng compile/build và grep
  cấu hình cũ; chỉ sửa test hiện có nếu implementation sau này bắt buộc để
  code compile trong pipeline.

Trade-off bảo mật cần chấp nhận: HMAC là symmetric key. Service nào có secret để
verify về mặt kỹ thuật cũng có khả năng ký token nếu secret bị lộ. Vì vậy phải
tách external/internal secret và không phân phát `GATEWAY_INTERNAL_JWT_SECRET`
cho service không cần verify internal token.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Hợp đồng HMAC](./phase-01-h-p-ng-hmac.md) | Completed |
| 2 | [Đổi code Gateway và User Service](./phase-02-i-code-gateway-v-user-service.md) | Completed |
| 3 | [Cập nhật config docs và verify](./phase-03-c-p-nh-t-config-docs-v-verify.md) | Completed |

## Dependencies

- Dựa trên plan `260916-1054-gateway-signed-internal-jwt`: giữ gateway-signed
  internal JWT, chỉ đổi vật liệu ký từ RSA sang HMAC.

## Acceptance Criteria

- [x] Không còn main code dùng `RsaKeyLoader`, `RSAKey`, `RS256`, `RSAPublicKey`,
  `RSAPrivateKey` cho JWT signing/verification.
- [x] Gateway verify external JWT bằng `EXTERNAL_JWT_SECRET` và `HS256`.
- [x] Gateway ký internal JWT bằng `GATEWAY_INTERNAL_JWT_SECRET` và `HS256`.
- [x] user-service verify internal JWT bằng `GATEWAY_INTERNAL_JWT_SECRET` và vẫn
  check issuer/audience/type/sub/roles như hiện tại.
- [x] user-service ký external access token bằng `EXTERNAL_JWT_SECRET`.
- [x] Config/compose/README không còn yêu cầu RSA public/private key env.
- [x] Không tạo file test mới.
- [x] Verify tối thiểu: `mvn -pl services/user-service,infra/api-gateway -am -DskipTests compile`
  pass và grep không còn RSA config cũ trong main/config.
- [x] Existing module tests pass for gateway + user-service; Docker/Testcontainers
  integration test is skipped because Docker is not available in this environment.

## Implementation Status — 2026-09-16

- Runtime JWT signing/verification đổi từ RSA/RS256 sang HMAC/HS256.
- Vẫn giữ hai secret domain riêng: `EXTERNAL_JWT_SECRET` và
  `GATEWAY_INTERNAL_JWT_SECRET`.
- Verification command:
  `mvn "-Dmaven.repo.local=C:\Users\pduy8\.m2\repository" -f "C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\pom.xml" -pl services/user-service,infra/api-gateway -am -DskipTests compile`
- Result: `BUILD SUCCESS`.
- Test command:
  `mvn "-Dmaven.repo.local=C:\Users\pduy8\.m2\repository" -f "C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\pom.xml" -pl services/user-service,infra/api-gateway -am -q test`
- Test result: 26 tests discovered, 25 passed, 1 skipped
  (`RefreshTokenPostgresIntegrationTest` skipped because no Docker/Testcontainers
  environment was available), 0 failures, 0 errors.
- Static grep: không còn RSA key config cũ trong main/config/docs; chỉ còn HMAC
  secret names mới có chủ đích.
