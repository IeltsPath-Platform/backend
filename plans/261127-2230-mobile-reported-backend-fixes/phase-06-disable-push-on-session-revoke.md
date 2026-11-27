# Phase 6: tắt thiết bị push khi phiên bị thu hồi (P1)

Phần lớn nhất của plan. Máy bị đăng xuất từ xa vẫn nhận thông báo của tài khoản, vì thiết bị push không gắn với phiên.

Internal JWT đã mang `sid` và `CurrentUserProvider.currentSessionId()` đã có, nên **client không phải sửa**.

## notification-service

- Migration `V18__notification_device_session.sql`: thêm `notification_devices.session_id UUID` (nullable) và index
  `(user_id, session_id) WHERE active`.
- Đăng ký thiết bị lưu `currentSessionId()` (`NotificationDeviceController` → `RegisterNotificationDeviceUseCase` →
  repository).
- Consumer mới cho sự kiện bên dưới, có retry và dead-letter như các consumer hiện có (đăng ký cả `DeadLetterRoute`).
  Tắt thiết bị `active` của người đó với `last_registered_at <= occurredAt` và:
  - `allSessions = true`: mọi thiết bị có `session_id` khác `keptSessionId` (gồm cả thiết bị chưa có `session_id`);
  - ngược lại: thiết bị có `session_id` nằm trong `sessionIds`.

  Điều kiện thời gian để sự kiện đến muộn không tắt thiết bị vừa đăng nhập lại. Câu lệnh tự idempotent.

## Sự kiện `user.sessions-revoked.v1`

- `shared/common-events`: record `UserSessionsRevokedV1(eventId, eventType, occurredAt, userId, sessionIds,
  allSessions, keptSessionId)` và topology, theo mẫu `PasswordChanged`.
- Contract mới `docs/contracts/user-sessions-revoked-v1.md`.

## user-service

Ghi sự kiện vào outbox (`UserNoticeOutbox`) trong cùng transaction với việc thu hồi. Gom vào một lớp dùng chung để không
sót đường nào:

| Use case | Gửi gì |
|---|---|
| `LogoutUseCase`, `RevokeMySessionUseCase` | một `sessionId` |
| `RefreshTokenUseCase` (phát hiện dùng lại token) | `sessionId` của họ token |
| `RevokeAllMySessionsUseCase` | `allSessions`, kèm `keptSessionId` khi giữ phiên hiện tại |
| `ChangePasswordUseCase`, `SetInitialPasswordUseCase`, `ResetPasswordUseCase`, `LinkGoogleAccountUseCase` | `allSessions` |
| `AssignRoleUseCase`, `ChangeUserStatusUseCase`, `DeleteUserUseCase` | `allSessions` |

`DeleteMyAccountUseCase` không cần: notification đã dọn thiết bị theo `user.deleted.v1`.

Kiểm tra publisher outbox của user-service gửi được event type mới mà không cần nhánh riêng.

## Test

- user-service: mỗi đường trong bảng tạo đúng một dòng outbox; rollback thì không có.
- notification-service (Testcontainers): tắt đúng thiết bị cho từng trường hợp; giữ `keptSessionId`; thiết bị đăng ký lại
  sau `occurredAt` không bị tắt; nhận trùng sự kiện không lỗi; thiết bị đã tắt không được gửi push.

## Giới hạn đã biết

Phiên tự hết hạn không phát sự kiện (app mobile đã tự huỷ push token). Thiết bị đăng ký trước khi deploy chưa có
`session_id`, chỉ bị tắt bởi các trường hợp `allSessions`.
