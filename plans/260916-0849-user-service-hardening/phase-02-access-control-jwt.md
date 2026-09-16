---
phase: 2
title: "Khóa truy cập và JWT"
status: pending
priority: P1
dependencies: [1]
---

# Phase 2: Khóa truy cập và JWT

## Overview

Thay `permitAll` bằng authorization rõ ràng, loại fallback JWT secret, và tách
public registration khỏi thao tác tạo user có đặc quyền.

## Architecture

```text
Client access JWT -> Security filter validates issuer/signature/expiry
                  -> controller extracts authenticated subject
                  -> use case receives subject explicitly

Anonymous only: login, refresh, logout-by-refresh-token, public registration
ADMIN only: user listing/creation, role/status changes, deactivate
Authenticated owner: /me and self-update
```

## Related Code Files

- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\config\SecurityConfig.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\config\AuthTokenProperties.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\application\usecase\JwtTokenService.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\api\controller\AuthController.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\api\controller\UserController.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\application\usecase\RegisterUseCase.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\application\usecase\GetMyProfileUseCase.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\api\filter\UserServiceLoggingFilter.java`
- Modify/Create tests under: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\test\java\com\group01\user\api\`

## Implementation Steps

1. Chốt policy role với product owner. Plan mặc định: `/register` bỏ role do
   client gửi hoặc reject role khác `PATIENT`; `POST /api/users` giữ cho ADMIN.
2. Bind properties JWT bằng `@Validated`; secret/issuer/audience bắt buộc từ
   environment. Xóa mọi default secret và fail startup nếu secret trống/yếu.
3. Cấu hình resource server decoder khớp issuer/algorithm/claim do
   `JwtTokenService` phát hành; validate signature, expiry, issuer và audience.
4. Thay `anyRequest().permitAll()` bằng matcher chính xác. Quy định ownership
   cho self endpoints và `hasRole("ADMIN")` cho user management.
5. Đổi `GetMyProfileUseCase` nhận `UUID subjectId`; controller lấy subject từ
   JWT đã xác thực. Không dùng `CurrentUserHolder`/`X-User-*` như trust boundary.
6. Giảm log: không ghi email/role/token và không ghi query string nguyên vẹn.
   Chuẩn hóa 401/403, malformed UUID/JSON, validation error mà không lộ PII.
7. Bổ sung MockMvc/Spring Security test trước khi refactor endpoint.

## Todo List

- [ ] Public registration không thể tạo ADMIN/DOCTOR.
- [ ] Thiếu JWT secret làm application context fail.
- [ ] Header giả không thể truy cập endpoint protected.
- [ ] API protected trả đúng 401, 403 hoặc 404 ownership.

## Success Criteria

- [ ] Test anonymous/admin/owner/foreign-user cover mọi nhóm endpoint.
- [ ] JWT giả, hết hạn, sai issuer/audience đều bị từ chối.
- [ ] Log assertion chứng minh không ghi password, JWT, email hay query PII.

## Risk Assessment

- Thay đổi access policy là breaking change. Phát hành API matrix cho consumer
  trước rollout và canary 401/403 metrics.
- Nếu gateway và service dùng secret/claims khác nhau, request hợp lệ sẽ 401;
  kiểm tra shared secret injection trong Phase 5.
