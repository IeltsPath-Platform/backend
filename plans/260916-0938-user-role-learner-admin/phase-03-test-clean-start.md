---
phase: 3
title: "Kiểm thử và khởi tạo môi trường sạch"
status: pending
priority: P1
dependencies: [1, 2]
---

# Phase 3: Kiểm thử và khởi tạo môi trường sạch

## Overview

Khẳng định V1 tự đủ và contract mới không còn trace doctor/patient/profile.

> Hoãn theo yêu cầu ngày 2026-09-16: không tạo/sửa/chạy test và không chạy
> Docker/Compose trong lượt triển khai này. README đã được cập nhật; các kiểm
> chứng còn lại vẫn pending.

## Related Code Files

- Modify: `services/user-service/src/test/java/com/group01/user/application/usecase/RegisterUseCaseTest.java`
- Modify: `services/user-service/src/test/java/com/group01/user/application/usecase/CreateUserUseCaseTest.java`
- Modify: `services/user-service/src/test/java/com/group01/user/application/usecase/AssignRoleUseCaseTest.java`
- Modify: `services/user-service/src/test/java/com/group01/user/api/controller/UserControllerSecurityTest.java`
- Create: `services/user-service/src/test/java/com/group01/user/api/controller/AuthControllerContractTest.java`
- Delete: `services/user-service/src/test/java/com/group01/user/infrastructure/profile/outbox/ProfileProvisioningOutboxWorkerTest.java`
- Create: `services/user-service/src/test/java/com/group01/user/infrastructure/persistence/CleanSchemaIntegrationTest.java`
- Create/modify: `infra/api-gateway/src/test/java/com/group01/apigateway/security/SecurityConfigRoleClaimTest.java`
- Create/modify: `infra/api-gateway/src/test/java/com/group01/apigateway/security/JwtUserHeaderGatewayFilterTest.java`
- Modify: `README.md` (completed)

## Implementation Steps

1. Update unit tests: default learner, valid role assignment, reject doctor/
   patient/admin public registration, and no profile outbox side effect.
2. Update MVC tests: learner self access, admin management, legacy/unknown JWT
   rejected, response `/api/users/*` and `/auth/me` không có `patientId`.
3. Add clean PostgreSQL/Flyway integration test from an empty DB. Verify V1
   creates `refresh_tokens` with `token_hash`/`expires_at`, no profile table,
   correct role seed, and JPA validation passes.
4. Add gateway tests proving only canonical claim turns into authority/header.
5. Reset a local DB/volume, run `mvn -pl services/user-service test`, gateway
   tests, full Maven build and Compose config/build. Document Docker-dependent
   tests as skipped if daemon is unavailable.

## Success Criteria

- [ ] Fresh database runs V1 then user-service starts without missing-table errors.
- [ ] Tests cover canonical roles and reject all legacy roles/claims.
- [x] README documents destructive reset and changed API/JWT contract.
