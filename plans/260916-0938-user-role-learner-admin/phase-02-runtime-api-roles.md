---
phase: 2
title: "Rút gọn runtime và API"
status: in-progress
priority: P1
dependencies: [1]
---

# Phase 2: Rút gọn runtime và API

## Overview

Loại bỏ toàn bộ khái niệm doctor/patient/profile trong code, chỉ giữ user local
với two-role authorization và JWT claim canonical.

## Related Code Files

- Modify: `services/user-service/src/main/java/com/group01/user/domain/vo/RoleName.java`
- Modify: `services/user-service/src/main/java/com/group01/user/domain/vo/UserStatus.java`
- Modify: `services/user-service/src/main/java/com/group01/user/application/usecase/CreateUserUseCase.java`
- Modify: `services/user-service/src/main/java/com/group01/user/application/usecase/RegisterUseCase.java`
- Modify: `services/user-service/src/main/java/com/group01/user/application/usecase/AssignRoleUseCase.java`
- Modify: `services/user-service/src/main/java/com/group01/user/application/command/RegisterCommand.java`
- Modify: `services/user-service/src/main/java/com/group01/user/api/dto/request/RegisterRequest.java`
- Modify: `services/user-service/src/main/java/com/group01/user/api/dto/response/UserResponse.java`
- Modify: `services/user-service/src/main/java/com/group01/user/api/dto/response/CurrentUserResponse.java`
- Modify: `services/user-service/src/main/java/com/group01/user/api/controller/UserController.java`
- Modify: `services/user-service/src/main/java/com/group01/user/api/controller/AuthController.java`
- Modify: `services/user-service/src/main/java/com/group01/user/config/SecurityConfig.java`
- Modify: `services/user-service/src/main/java/com/group01/user/api/exception/GlobalExceptionHandler.java`
- Delete: `services/user-service/src/main/java/com/group01/user/application/exception/ProfileProvisioningException.java`
- Delete: `services/user-service/src/main/java/com/group01/user/application/usecase/ProfileLookupClient.java`
- Delete: `services/user-service/src/main/java/com/group01/user/application/usecase/ProfileProvisioningClient.java`
- Delete: `services/user-service/src/main/java/com/group01/user/application/usecase/ProfileProvisioningOutbox.java`
- Delete: `services/user-service/src/main/java/com/group01/user/application/usecase/ProfileProvisioningRequest.java`
- Delete: `services/user-service/src/main/java/com/group01/user/infrastructure/profile/`
- Modify: `infra/config-server/config-repo/user-service.yaml`
- Deferred: `docker-compose.yml` (theo yêu cầu hiện tại không làm Docker)
- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/SecurityConfig.java`
- Modify: `infra/api-gateway/src/main/java/com/group01/apigateway/security/JwtUserHeaderGatewayFilter.java`

## Implementation Steps

1. Rút enum còn `ADMIN`, `LEARNER`; đổi default create/register thành
   `LEARNER`, bỏ `PENDING_PROFILE` và để registration tạo user `ACTIVE`.
2. Rút request/command registration về identity fields (`email`, `password`,
   `fullName`, `phoneNumber`, optional role). Accept null/rỗng/`LEARNER`; reject
   `ADMIN` hoặc value không canonical bằng `400`.
3. Xóa `patientId` và profile lookup khỏi `UserController`/`AuthController`;
   giữ permission self-service cho learner và admin-only management.
4. Xóa exception, ports, REST client, outbox adapter/entity/repository/services/
   worker và mọi scheduled profile job. Xóa test riêng cho worker ở Phase 3.
5. Xóa doctor/patient/profile config. Gateway và user-service chỉ
   convert/forward claim `ADMIN` hoặc `LEARNER`; reject claim legacy/unknown.
   Docker environment variables được hoãn theo yêu cầu hiện tại.

## Success Criteria

- [x] Không còn bean, outbound request, scheduled worker, config hay DTO field mang semantics profile trong runtime source/config.
- [x] Public registration không thể tạo admin; create/assign chỉ nhận hai role.
- [x] JWT mới và gateway headers chỉ mang `ADMIN`/`LEARNER`.

## Ghi chú triển khai

Docker Compose không được sửa trong lượt này theo yêu cầu. Các thay đổi source
đã biên dịch thành công; test được theo dõi ở Phase 3 và đang hoãn.

## Risk Assessment

- Đây là breaking API có chủ đích. Không hỗ trợ payload `DOCTOR`/`PATIENT` hay
  response `patientId` theo scope reset codebase.
