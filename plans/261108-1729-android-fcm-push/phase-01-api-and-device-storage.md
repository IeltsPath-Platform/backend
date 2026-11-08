---
phase: 1
title: "API and device storage"
status: complete
effort: ""
---

# Phase 1: API and device storage

## Overview

Thêm hợp đồng thiết bị Android, bảng thiết bị và hai use case đăng ký/gỡ. Context: [plan](plan.md), [khảo sát](reports/codebase-findings.md), `AGENTS.md` §3.1/§3.9, `docs/contracts/community-events-v1.md`.

## Requirements

| API | Request | Success |
| --- | --- | --- |
| `PUT /api/notifications/devices/{installationId}` | UUID; JSON `provider: FCM`, `platform: ANDROID`, `token`, `appVersion?` | `204 No Content` |
| `DELETE /api/notifications/devices/{installationId}` | UUID, không body | `204 No Content` |

- Lấy user từ `CurrentUserProvider.requireCurrentUser()`, theo internal JWT đã được Gateway xác thực; không nhận userId từ client.
- PUT bắt buộc token không blank; đề xuất cap 4096 ký tự, appVersion 128 ký tự; không trim/truncate token âm thầm. Provider/platform chỉ hỗ trợ hai giá trị trên. Body/schema lỗi trả `400` theo ErrorResponse hiện có; không echo token. Chưa xác thực `401`, không đủ quyền theo security hiện có `403`.
- DELETE unknown/nonowned cũng `204`, không sửa record của người khác và không tiết lộ owner. Không gọi Firebase để đăng ký/gỡ.
- Một tài khoản có nhiều thiết bị. Cùng installation đổi tài khoản: đăng ký thành công sau cùng sở hữu mapping; không giữ active mapping cho owner cũ.
- Đăng ký giống hệt chỉ refresh metadata/time. Token, owner hoặc trạng thái active thay đổi thì tăng `registration_version`.

## Architecture / schema đề xuất

Migration mới `V5__create_notification_devices.sql` nếu V5 còn trống tại thời điểm triển khai; không sửa V1–V4 đã áp dụng.

`notification_devices`:

| Field | Kiểu / ý nghĩa |
| --- | --- |
| `id` | UUID PK, ID record ổn định |
| `user_id`, `installation_id` | UUID NOT NULL |
| `provider`, `platform` | varchar NOT NULL, CHECK hỗ trợ FCM/ANDROID |
| `token`, `token_hash` | token recoverable để gửi; hash SHA-256 để lookup, không trả client |
| `app_version` | varchar(128) nullable |
| `active`, `registration_version` | boolean, bigint dương |
| `created_at`, `updated_at`, `last_registered_at`, `disabled_at` | timestamptz |

Ràng buộc:

- UNIQUE `(provider, installation_id)` trên tất cả record để account switch đổi đúng một owner.
- Partial UNIQUE `(provider, token_hash) WHERE active` để một token chỉ có một mapping active.
- Index `(user_id, active)` phục vụ snapshot và worker; hash không đưa vào logs/metrics.
- Nếu token đang active ở installation khác: transaction vô hiệu hóa record cũ/tăng version rồi upsert installation đích; không tạo hai active token. Dùng locking/unique constraint và bounded retry toàn transaction, thứ tự khóa xác định, không chỉ check-then-insert.
- DELETE UPDATE có điều kiện `user_id` và installation; owner cũ không thể disable mapping sau khi đã chuyển tài khoản.
- Không có FK chéo sang user_db. Không lưu token vào delivery table.

Token FCM là địa chỉ định tuyến nhạy cảm, không phải access token đăng nhập. Đề xuất schema token TEXT với quyền DB tối thiểu; encrypted disk/DB và retention là điều kiện production. Field encryption cần cơ chế quản lý khóa đã chốt nếu hạ tầng yêu cầu; không dùng hash thay token gửi, không tự thêm crypto framework hoặc đặt khóa trong DB. Nếu chọn field encryption, đổi storage sang ciphertext + key ID qua migration trước khi có dữ liệu, kiểm thử rotation; không dùng plaintext fallback âm thầm.

## Related Code Files

Create dưới `services/notification-service/src/main/java/com/ieltspath/notification/`:

- `api/NotificationDeviceController.java`, request DTO có validation, không dùng entity làm response.
- `application/RegisterNotificationDeviceUseCase.java`, `DisableNotificationDeviceUseCase.java` (giữ convention application phẳng của service).
- `domain/vo/NotificationDevice.java`, `domain/repository/NotificationDeviceRepository.java`.
- `infrastructure/persistence/NotificationDeviceRepositoryAdapter.java` dùng JDBC như repo hiện tại.

Create migration + `docs/contracts/notification-push-v1.md`. Modify `api/exception/GlobalExceptionHandler.java` chỉ khi cần mapping lỗi mới an toàn. Gateway đã có `/api/notifications/**`; không sửa route/shared-security nếu test chứng minh không cần.

## Implementation Steps

1. Chốt contract dự thảo trong PR: status, validation, idempotency, ownership transfer, caps và logout semantics.
2. Viết test controller/auth và persistence trước: insert, repeat, refresh, transfer, revoke.
3. Thêm migration/index và JDBC adapter; dùng thời gian server/Clock để test được.
4. Thêm use cases; validation invariant trong application/domain ngoài DTO.
5. Thêm controller; redact cả record `toString`, lỗi validation, tracing request body; chỉ log device record ID.
6. Test hai PUT đồng thời cùng installation và cùng token trên hai installation; kiểm tra unique constraint và owner cuối cùng nhất quán. Test DELETE owner cũ đua với PUT owner mới.
7. Xác minh hai endpoint đi qua route/rate-limit tier default hiện có. Không mở endpoint public.

## Verification

Mockito use case + WebMvc security/validation + Testcontainers PostgreSQL migration/constraint/races. Nếu thiếu Docker, ghi rõ persistence tests bị skip, chưa đạt gate. Không cần credentials Firebase ở phase này.

## Success Criteria

- [ ] PUT/DELETE đúng auth/status, idempotent, validate và không lộ token.
- [ ] Refresh vô hiệu hóa token cũ; repeat giống hệt không tăng version.
- [ ] Transfer/token collision/race không tạo mapping active trùng hoặc cho owner cũ xóa owner mới.
- [ ] Migration upgrade từ V4 chạy được; các notification/preference hiện tại còn nguyên.

## Risk Assessment / rollback

Token/address không chứng minh việc sở hữu một tài khoản; auth luôn dựa trên JWT. `installationId` ngẫu nhiên phải ổn định theo lần cài app, không đưa vào analytics công khai. Không thêm device listing trong phase này.

Migration chỉ additive. Rollback code/API bằng release cũ hoặc cờ ingress phù hợp; không drop bảng hay xóa dữ liệu đang dùng. Dữ liệu goal/profile hoặc Firebase Database không liên quan.
