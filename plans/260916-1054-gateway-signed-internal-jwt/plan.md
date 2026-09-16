---
title: "Gateway-signed internal JWT"
description: "Chuyển trust boundary sang API Gateway: gateway verify external access token, ký JWT nội bộ ngắn hạn cho downstream, downstream chỉ tin internal JWT đó."
status: in-progress
priority: P1
branch: "main"
tags: [auth, security, api-gateway, user-service, infra]
blockedBy: [260916-1829-simplify-jwt-claims, 260917-remove-x-user-header-cleanup]
blocks: [260916-1157-switch-jwt-rsa-to-hmac]
created: "2026-09-16T03:55:22.518Z"
createdBy: "ck:plan"
source: skill
---

# Gateway-signed internal JWT

> Supersession note: `260917-remove-x-user-header-cleanup` updates the old
> `X-User-*` strip invariant. Current direction is JWT-only identity with no
> runtime `X-User-*` support; gateway still replaces client `Authorization`
> with a gateway-signed internal JWT for protected downstream routes.

## Overview

Plan này thay mô hình hiện tại `user-service` tự verify access token HMAC chung
bằng mô hình gateway-issued internal JWT.

Luồng đích:

```text
Client external JWT -> API Gateway verify issuer/audience/type/role
                    -> Gateway strip client identity headers
                    -> Gateway sign internal JWT, aud=user-service, ttl ~60s
                    -> user-service verify gateway public key + internal claims
```

Quyết định chính:

- External access token dành cho client có `aud=api-gateway`, `type=access`.
- Internal JWT có `iss=urn:code-base:api-gateway`, `aud=<service>`,
  `type=gateway-internal`, `sub=<user UUID>`, `roles=[ADMIN|LEARNER]`,
  `exp` rất ngắn và `jti`.
- Dùng asymmetric key, ưu tiên `RS256`/`PS256`; gateway giữ private key,
  downstream chỉ có public JWKS/current+previous key.
- Downstream không tin `X-User-*` làm identity nữa. Header có thể bị strip/log,
  nhưng nguồn auth duy nhất là JWT đã verify.
- Không mở signer cho mọi service discovery route. Gateway phải có route/audience
  mapping rõ ràng cho từng downstream.

Quan hệ với plan role `LEARNER/ADMIN`: plan này giả định baseline role mới đã
được áp dụng trong source. Không đặt `blockedBy` vì phần còn lại của plan role
là test/Docker có thể chạy song song, nhưng implementation plan này sẽ thay thế
quyết định cũ "user-service verify external JWT trực tiếp".

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Hợp đồng token và key](./phase-01-h-p-ng-token-v-key.md) | Completed |
| 2 | [Gateway xác minh external và ký internal](./phase-02-gateway-x-c-minh-external-v-k-internal.md) | Completed |
| 3 | [Downstream xác minh internal và đóng trust header](./phase-03-downstream-x-c-minh-internal-v-ng-trust-header.md) | Completed |
| 4 | [Kiểm thử rollout và vận hành](./phase-04-ki-m-th-rollout-v-v-n-h-nh.md) | In progress |

## Dependencies

- Không có dependency formal.
- Cần đọc lại plan `260916-0938-user-role-learner-admin` trước khi cook để bảo
  đảm không phục hồi lại `PATIENT`/`DOCTOR` hoặc cơ chế header trust cũ.

## Acceptance Criteria

- [x] Gateway reject external token sai issuer/audience/type/sub/role và chỉ ký
  internal JWT sau khi external token hợp lệ.
- [x] Gateway luôn strip `Authorization`/`X-User-*` từ client trước khi forward; với
  protected route thì thay bằng `Authorization: Bearer <internal-jwt>`.
- [x] `user-service` reject external access token, shared HMAC token, raw headers,
  expired internal token, wrong audience, wrong issuer, wrong type và role ngoài
  `ADMIN`/`LEARNER`.
- [x] `common-security` không còn tạo identity runtime từ unsigned headers cho
  `user-service`.
- [x] Docker/config local không expose trực tiếp `user-service` ra host port và
  không share cùng một `JWT_SECRET` giữa gateway và user-service.

## Implementation Status — 2026-09-16

- Runtime trust-boundary change completed: external JWT is verified at gateway,
  gateway signs short-lived internal JWT, and user-service verifies gateway
  internal JWT instead of raw identity headers.
- Verification passed with
  `mvn "-Dmaven.repo.local=C:\Users\pduy8\.m2\repository" -f "C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\pom.xml" -pl services/user-service,infra/api-gateway -am test`.
- Result: 23 tests, 0 failures, 0 errors, 1 skipped
  (`RefreshTokenPostgresIntegrationTest` skipped because Docker/Testcontainers
  has no valid Docker environment in this session).
- Follow-up left for production hardening: fuller gateway filter abuse tests,
  rate limiting for public auth endpoints, and a current/previous/retired key
  rotation runbook/config shape.

## Red Team Review

### Session — 2026-09-16

**Findings:** 3 accepted, 0 rejected.

| # | Finding | Severity | Disposition | Applied To |
|---|---|---|---|---|
| 1 | Gateway package paths were wrong; actual code uses `com/group01/apigateway` (`infra/api-gateway/src/main/java/com/group01/apigateway/security/SecurityConfig.java:47`). | Medium | Accept | Phase 1, Phase 2 |
| 2 | Broad discovery locator exists (`infra/config-server/config-repo/api-gateway.yaml:9`), so signer must not mint internal JWTs for arbitrary discovered routes. | High | Accept | Phase 2 |
| 3 | user-service imports common-security auto-config (`services/user-service/src/main/java/com/group01/user/UserServiceApplication.java:3`) and common-security registers `CurrentUserHeaderFilter` (`shared/common-security/src/main/java/com/group01/commonsecurity/config/CommonSecurityAutoConfiguration.java:15`), so header identity removal must be explicit. | High | Accept | Phase 3 |

### Whole-Plan Consistency Sweep

- Files reread: `plan.md`, `phase-01-*`, `phase-02-*`, `phase-03-*`,
  `phase-04-*`.
- Decision deltas checked: gateway package path, discovery locator allowlist,
  common-security auto-config removal.
- Reconciled stale references: 3.
- Unresolved contradictions: 0.
