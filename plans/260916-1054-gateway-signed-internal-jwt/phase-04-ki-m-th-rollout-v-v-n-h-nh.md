---
phase: 4
title: "Kiểm thử rollout và vận hành"
status: in-progress
priority: P1
dependencies: [1, 2, 3]
---

# Phase 4: Kiểm thử rollout và vận hành

## Overview

Phủ test cho contract auth mới, định nghĩa rollout/rollback và key rotation để
không biến thay đổi security thành một lần deploy mù.

## Requirements

- Functional: test chứng minh gateway là nơi verify external token và
  downstream chỉ verify internal token.
- Non-functional: rollout có giai đoạn tương thích có thời hạn, quan sát được
  lỗi 401/403 và không ghi lộ token/key.

## Architecture

Rollout khuyến nghị:

1. Deploy config/public keys và decoders mới.
2. Gateway có thể tạm accept external legacy token trong thời gian rất ngắn nếu
   còn client token cũ, nhưng downstream không trust raw headers.
3. Gateway bắt đầu forward internal JWT.
4. user-service chuyển strict internal-only.
5. Remove legacy shared secret path và direct service port.

## Related Code Files

- Modify/Create: `infra/api-gateway/src/test/java/com/group01/apigateway/security/*`
- Modify/Create: `services/user-service/src/test/java/com/group01/user/config/*`
- Modify/Create: `services/user-service/src/test/java/com/group01/user/api/controller/*`
- Modify: `shared/common-security/src/test/java/com/group01/commonsecurity/*` if header identity is removed
- Modify: `README.md` or `docs/*` if project docs exist for security/runtime setup
- Modify: CI/build scripts only if existing project pattern requires it

## Implementation Steps

1. Gateway unit/slice tests:
   external token validation, role normalization, wrong audience/type rejection,
   internal JWT claim shape, header stripping, and public route passthrough.
2. User-service security tests:
   accept valid gateway internal JWT, reject external JWT, wrong audience,
   wrong issuer, wrong type, expired token, invalid key, and raw headers.
3. Contract/integration test for one protected endpoint through gateway:
   client token in, internal token forwarded, downstream authorizes by role.
4. Add key rotation test: service accepts current+previous public keys and
   rejects retired key after config refresh/restart.
5. Add rollout notes for environment variables, secret mounting, JWKS endpoint
   availability, and rollback limits.
6. Update operational docs: token contracts, route audience mapping, local dev
   key generation, and how to rotate gateway signer.

## Success Criteria

- [x] Focused gateway and user-service tests cover positive path and rejection matrix.
- [x] Build/test command for touched modules is documented in the phase outcome.
- [ ] Rollout notes explain temporary compatibility windows and when to delete them.
- [ ] Key rotation runbook has current/previous/retired key steps.
- [x] Docs mention that downstream still has security config because it verifies
  gateway-signed internal JWTs.

## Outcome

- Verification command:
  `mvn "-Dmaven.repo.local=C:\Users\pduy8\.m2\repository" -f "C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\pom.xml" -pl services/user-service,infra/api-gateway -am test`
- Result: 23 tests, 0 failures, 0 errors, 1 skipped because Docker/Testcontainers
  could not find a valid Docker environment.
- Follow-up: add detailed key-rotation runbook and optional deeper gateway
  end-to-end filter tests before production rollout.

## Risk Assessment

Main risk is a long-lived compatibility mode that keeps the old shared secret
alive. Mitigation: set an explicit removal step tied to token max lifetime plus
clock skew and fail deployment if legacy secret config remains after cutoff.
