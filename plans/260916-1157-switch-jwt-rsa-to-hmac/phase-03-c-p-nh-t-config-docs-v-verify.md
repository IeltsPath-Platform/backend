---
phase: 3
title: "Cập nhật config docs và verify"
status: completed
priority: P2
dependencies: [1, 2]
effort: "small"
---

# Phase 3: Cập nhật config docs và verify

## Overview

Đổi config/docs cho HMAC và verify bằng compile/static grep. Phase này cố ý
không tạo test file mới theo yêu cầu hiện tại.

## Requirements

- Functional: local config có đủ secret để gateway/user-service boot.
- Non-functional: không để secret thật trong repo; không mở lại direct
  user-service public port; không làm Docker/test file mới.

## Architecture

Runtime config sau khi đổi:

```text
config-server/user-service.yaml
  app.auth.external-jwt-secret=${EXTERNAL_JWT_SECRET}
  app.auth.internal-jwt-secret=${GATEWAY_INTERNAL_JWT_SECRET}

config-server/api-gateway.yaml
  app.auth.external-jwt-secret=${EXTERNAL_JWT_SECRET}
  app.auth.internal-jwt-secret=${GATEWAY_INTERNAL_JWT_SECRET}
```

Compose chỉ truyền env placeholder/secret name, không hard-code secret thật.

## Related Code Files

- Modify: `infra/config-server/config-repo/user-service.yaml`
- Modify: `infra/config-server/config-repo/api-gateway.yaml`
- Modify: `docker-compose.yml`
- Modify: `README.md`
- Optional modify existing tests only if compile/test task later requires it:
  - `services/user-service/src/test/java/...`
  - `infra/api-gateway/src/test/java/...`

## Implementation Steps

1. Config server:
   - Replace RSA key properties with:
     - `external-jwt-secret`
     - `internal-jwt-secret`
   - Keep issuer/audience/key-id where still useful.

2. Docker compose:
   - Remove:
     - `EXTERNAL_JWT_PRIVATE_KEY`
     - `EXTERNAL_JWT_PUBLIC_KEY`
     - `GATEWAY_INTERNAL_PRIVATE_KEY`
     - `GATEWAY_INTERNAL_PUBLIC_KEY`
   - Add:
     - `EXTERNAL_JWT_SECRET`
     - `GATEWAY_INTERNAL_JWT_SECRET`
   - Keep user-service without host port.

3. README:
   - Replace RSA/public-private-key explanation with HMAC explanation.
   - Emphasize two separate secrets, not one shared `JWT_SECRET`.
   - Keep warning that downstream still needs security config to verify gateway
     internal JWT.

4. Static grep checks:

   ```powershell
   rg -n "RsaKeyLoader|RS256|RSAKey|RSAPublicKey|RSAPrivateKey|external-public-key|external-private-key|internal-public-key|internal-private-key" infra services/user-service/src/main docker-compose.yml README.md
   rg -n "JWT_SECRET|jwt-secret" infra services/user-service/src/main docker-compose.yml README.md
   ```

   Expected: no old RSA key config; no generic shared `JWT_SECRET`.

5. Compile verification, no new tests:

   ```powershell
   mvn -f pom.xml -pl services/user-service,infra/api-gateway -am -DskipTests compile
   ```

6. If compile fails only because existing tests reference old RSA helper, do not
   add new test files. Either leave tests untouched and report compile scope, or
   update existing test fixtures in a separate follow-up if user later wants full
   test run.

## Success Criteria

- [x] Config/docs mention HMAC secrets instead of RSA keys.
- [x] No direct user-service host port is reintroduced.
- [x] No new files under `src/test`.
- [x] Compile with `-DskipTests` passes for gateway + user-service modules.
- [x] Existing gateway + user-service tests pass; Docker/Testcontainers
  integration test is skipped because Docker is not available in this
  environment.
- [x] Static grep shows no old RSA JWT config in main/config/docs.

## Outcome

- README, config server YAML, and compose now document/use HMAC secrets.
- Verification passed:
  `mvn "-Dmaven.repo.local=C:\Users\pduy8\.m2\repository" -f "C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\pom.xml" -pl services/user-service,infra/api-gateway -am -DskipTests compile`
- Result: `BUILD SUCCESS`.
- Existing module tests also passed:
  `mvn "-Dmaven.repo.local=C:\Users\pduy8\.m2\repository" -f "C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\pom.xml" -pl services/user-service,infra/api-gateway -am -q test`
- Test result: 26 tests discovered, 25 passed, 1 skipped
  (`RefreshTokenPostgresIntegrationTest` skipped because no Docker/Testcontainers
  environment was available), 0 failures, 0 errors.

## Risk Assessment

Không tạo test file mới theo yêu cầu. Các test hiện có đã được cập nhật fixture
RSA sang HMAC khi cần để pipeline compile/run được. Phần chưa verify được hoàn
toàn là integration test phụ thuộc Docker/Testcontainers vì môi trường hiện tại
không có Docker runtime hợp lệ.
