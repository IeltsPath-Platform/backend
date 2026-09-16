---
phase: 3
title: "Bảo vệ token và dữ liệu"
status: pending
priority: P1
dependencies: [1, 2]
---

# Phase 3: Bảo vệ token và dữ liệu

## Overview

Làm refresh-token rotation atomic, khôi phục data invariants và đảm bảo database
là authority cuối cho uniqueness thay vì chỉ dựa vào pre-check trong use case.

## Related Code Files

- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\application\usecase\RefreshTokenUseCase.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\domain\repository\RefreshTokenRepository.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\infrastructure\adapter\RefreshTokenRepositoryAdapter.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\infrastructure\persistence\entity\RefreshTokenJpaEntity.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\infrastructure\persistence\repository\RefreshTokenJpaRepository.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\domain\aggregate\User.java`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\java\com\group01\user\infrastructure\persistence\entity\UserJpaEntity.java`
- Create: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\resources\db\migration\V7__enforce_user_and_refresh_token_invariants.sql`
- Modify/Create tests under: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\test\java\com\group01\user\`

## Implementation Steps

1. Chốt cách xử lý dữ liệu legacy trước `NOT NULL`: backfill password hash,
   ép reset password, hoặc tách account external. Không chạy migration khi còn
   row không giải quyết được.
2. Thay lookup/revoke/save rời rạc bằng port `consumeActiveToken` có semantics
   atomic. Adapter lock row (`PESSIMISTIC_WRITE`) hoặc conditional update trả
   affected-row count; chọn một cơ chế, không dùng cả hai.
3. Chỉ tạo refresh token mới sau khi consume cũ thành công. Request thứ hai
   cùng refresh token phải trả unauthenticated, không mint token mới.
4. Migration V7 thêm/khôi phục `NOT NULL password_hash`, version/lock support,
   index cần cho active refresh lookup và cập nhật `updated_at` nếu mapping cần.
5. Không để aggregate lộ mutable role set; dùng defensive copy. Bảo đảm mutation
   cập nhật timestamp và DB conflict được translate sang 409 domain-safe.
6. Giữ pre-check email/phone để UX tốt nhưng catch unique-constraint tại adapter
   để race không thành 500.

## Todo List

- [ ] Hai refresh song song chỉ có đúng một request thành công.
- [ ] Duplicate email/phone đồng thời nhận conflict có kiểm soát.
- [ ] Không còn user local-auth với password hash null sau migration policy.
- [ ] Aggregate không lộ `passwordHash` qua response contract.

## Success Criteria

- [ ] Testcontainers PostgreSQL chạy Flyway V1–V7 trên database rỗng và upgrade path.
- [ ] Concurrency integration test dùng hai transaction/thread thật; không dùng
  H2 làm bằng chứng lock Postgres.
- [ ] Unit tests cover token expired/revoked/replayed và user role mutation.

## Risk Assessment

- Pessimistic lock có thể làm refresh chậm khi replay cao; lock chỉ bao phủ one
  token row, monitor lock wait.
- `NOT NULL` migration không rollback được an toàn; backup/preflight bắt buộc.
