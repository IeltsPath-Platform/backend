---
phase: 1
title: "Reset schema V1"
status: in-progress
priority: P1
dependencies: []
---

# Phase 1: Reset schema V1

## Overview

Biến migration thành một snapshot schema sạch, đúng với entity hiện dùng. Xóa
tất cả migration V2–V8 thay vì viết Flyway migration chuyển đổi.

## Related Code Files

- Modify: `services/user-service/src/main/resources/db/migration/V1__create_user_tables.sql`
- Delete: `services/user-service/src/main/resources/db/migration/V2__create_refresh_tokens_table.sql`
- Delete: `services/user-service/src/main/resources/db/migration/V3__add_keycloak_user_mapping.sql`
- Delete: `services/user-service/src/main/resources/db/migration/V4__restore_local_auth.sql`
- Delete: `services/user-service/src/main/resources/db/migration/V5__seed_doctor_users.sql`
- Delete: `services/user-service/src/main/resources/db/migration/V6__seed_doctor_users_v2.sql`
- Delete: `services/user-service/src/main/resources/db/migration/V7__enforce_user_and_refresh_token_invariants.sql`
- Delete: `services/user-service/src/main/resources/db/migration/V8__create_profile_provisioning_outbox.sql`

## Implementation Steps

1. Gộp final local-auth schema của V4/V7 vào V1: bảng `refresh_tokens` với
   `token_hash`, `expires_at`, `revoked_at`, foreign key user, indexes token/user/
   expiry và active token hash. Không dùng schema `token`/`expired_at` lỗi thời của V2.
2. Giữ `password_hash NOT NULL`; V1 không có `keycloak_user_id`, không tạo
   `profile_provisioning_outbox`, và status chỉ là `ACTIVE`, `INACTIVE`,
   `LOCKED`, `PENDING_VERIFY`.
3. Seed `roles` chỉ `ADMIN`, `LEARNER`; thêm DB check role names nếu schema
   PostgreSQL hiện tại hỗ trợ constraint đó một cách gọn gàng.
4. Xóa V2–V8. Xác nhận migration directory còn đúng một file V1 và JPA
   `RefreshTokenJpaEntity`/`UserJpaEntity` validate được với schema mới.
5. Ghi rõ ở README/runbook: muốn chạy lại phải reset DB/volume trước, không
   chạy against `flyway_schema_history` cũ.

## Success Criteria

- [x] Một `V1` tạo toàn bộ tables/indexes hiện được entity local auth dùng.
- [x] Không còn migration Keycloak, doctor seed, patient seed hay profile outbox.
- [ ] Database sạch chạy Flyway thành công và không cần migration sau V1.

## Ghi chú triển khai

Phần migration đã hoàn thành. Việc chạy Flyway trên database sạch được hoãn theo
yêu cầu không khởi tạo môi trường/Docker ở lượt triển khai này.

## Risk Assessment

- Xóa migration history làm database đã apply V1–V8 không còn compatible.
  Scope đã chốt là reset database, không deploy production có dữ liệu.
