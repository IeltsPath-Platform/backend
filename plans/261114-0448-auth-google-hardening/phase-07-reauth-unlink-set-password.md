---
phase: 7
title: "Xác thực lại, gỡ liên kết Google (G3) và đặt mật khẩu lần đầu (G5)"
status: pending
priority: P1
dependencies: [2, 3, 4]
---

# Phase 7: Xác thực lại cho thao tác nhạy cảm

## Vấn đề
- `DELETE /api/users/me/oauth/{provider}` chỉ cần access token. Ai có token (phiên bị chiếm) gỡ được liên kết Google mà
  không phải chứng minh lại là chủ tài khoản, và chủ tài khoản không được báo.
- Tài khoản tạo bằng Google không có mật khẩu. `ChangePasswordUseCase` ném `PasswordNotConfiguredException` (409) và chỉ
  còn đường quên mật khẩu qua email; vì vậy người dùng này không bao giờ gỡ được liên kết (quy tắc "không gỡ liên kết
  cuối cùng khi chưa có mật khẩu").

## Thiết kế

### Token xác thực lại (reauth), dùng một lần
`POST /api/users/me/reauth` (cần JWT; **cần duyệt** vì là endpoint mới), body đúng một trong hai:
- `{ "currentPassword": "..." }`: kiểm bằng `PasswordEncoder.matches`, dùng chung bộ đếm
  `PasswordChangeAttempts` của đổi mật khẩu (sai nhiều lần thì khóa 15 phút, trả 429), tài khoản phải có mật khẩu.
- `{ "googleIdToken": "..." }`: qua `GoogleIdTokenVerifier` với cùng luật `iat` (phase 5) và nonce (nếu bật); `sub` phải
  **trùng** định danh Google đã liên kết của chính user, không thì 403.

Trả `{ "reauthToken": "<32 byte base64url>", "expiresInSeconds": 300, "method": "PASSWORD|GOOGLE" }`.
Lưu Redis `auth:reauth:{SHA-256(token)}` = `userId|method|sid`, TTL 300 giây; token gắn với user và phiên hiện tại (`sid`,
phase 3), **dùng một lần** (`GETDEL` khi nơi tiêu thụ xác nhận). Chỉ lưu hash.

Gateway: gán `/api/users/me/reauth` vào tier `auth` (rate limit chặt) trong `config-repo/api-gateway.yaml`, cập nhật
`AuthRouteConsistencyTest` theo AGENTS §3.9.

### G3. Gỡ liên kết cần xác thực lại + email báo
- `DELETE /api/users/me/oauth/{provider}` đọc header `X-Reauth-Token`. Khi
  `app.auth.reauth-required-for-unlink=true` (mặc định `false` đến khi frontend sẵn sàng): thiếu hoặc sai token → `403
  REAUTH_REQUIRED`. Quy tắc "không gỡ liên kết cuối cùng khi chưa có mật khẩu" giữ nguyên.
- Gỡ thành công → `UserNoticeOutbox.enqueue` sự kiện mới `user.google-account-unlinked.v1` (payload như
  `user.google-account-linked.v1`: `eventId, eventType, occurredAt, userId, email, displayName`, không có thông tin xác thực).
- Notification: nhân bản `GoogleAccountLinkedListener` và `GoogleAccountLinkedMessagingConfig` (queue, retry, DLQ do
  Notification sở hữu), thêm mẫu thư trong `MailTemplate`, gọi `SendAccountNoticeMailUseCase`.
- `common-events`: thêm tên sự kiện và record `GoogleAccountUnlinkedV1` trong `UserMessaging`; contract
  `docs/contracts/user-google-account-unlinked-v1.md` cùng PR.

### G5. Đặt mật khẩu lần đầu cho tài khoản chỉ có Google
`POST /api/users/me/password/initial` (cần JWT), body `{ "newPassword": "..." }`, header `X-Reauth-Token` **bắt buộc** và
phải có `method=GOOGLE`:
- Chỉ áp khi `passwordHash` rỗng; đã có mật khẩu thì 409 (dùng đổi mật khẩu).
- Phải xác minh email (như đổi mật khẩu); qua `PasswordPolicyService` (phase 4).
- Ghi hash, thu hồi mọi refresh token (lý do `PASSWORD_CHANGE`, phase 2) và access token (`revokeUser`, phase 3), phát
  `PasswordChangedV1` sẵn có. Giống đổi mật khẩu: client đăng nhập lại sau khi đặt.
- Không đổi hành vi `POST /api/users/me/password` hiện có (vẫn 409 cho tài khoản chưa có mật khẩu).

## Files
- user-service: `api/controller/ReauthController.java` (hoặc trong `UserController`), `api/dto/request/ReauthRequest.java`,
  `api/dto/response/ReauthResponse.java`, `application/usecase/{Reauth,SetInitialPassword}UseCase.java`,
  `UnlinkOAuthIdentityUseCase.java`, `OAuthIdentityController.java`, `application/port/ReauthTokenStore.java`,
  `infrastructure/adapter/RedisReauthTokenStore.java`, `infrastructure/messaging/RabbitUserEventPublisher.java`
  (khóa định tuyến sự kiện mới), `config-repo/user-service.yaml`.
- common-events: `UserMessaging`, `GoogleAccountUnlinkedV1`.
- notification-service: `GoogleAccountUnlinkedListener`, `GoogleAccountUnlinkedMessagingConfig`, `MailTemplate`,
  `SendAccountNoticeMailUseCase`.
- api-gateway: `config-repo/api-gateway.yaml`, `AuthRouteConsistencyTest`.
- Tài liệu: `docs/contracts/auth-reauth-v1.md` (mới), `user-google-account-unlinked-v1.md` (mới), cập nhật
  `user-management-v1.md` và `google-sign-in-v1.md`.

## Tests
- `ReauthUseCaseTest`: mật khẩu đúng/sai/khóa; ID token Google đúng `sub`/khác `sub`/user chưa liên kết Google; token chỉ
  dùng một lần; token của user khác hoặc phiên khác bị từ chối; hết hạn sau 300 giây.
- `UnlinkOAuthIdentityUseCaseTest`: cờ tắt (hành vi cũ); cờ bật thiếu token → `REAUTH_REQUIRED`; có token → gỡ và phát
  sự kiện; liên kết cuối cùng chưa có mật khẩu vẫn bị chặn.
- `SetInitialPasswordUseCaseTest`: cần token `GOOGLE`; token `PASSWORD` bị từ chối; đã có mật khẩu → 409; mật khẩu yếu →
  `WEAK_PASSWORD`; thu hồi token và phát `PasswordChangedV1`.
- Notification: listener nhận sự kiện, gửi thư, retry/DLQ như listener liên kết hiện có.
- `@WebMvcTest` các endpoint mới (xác thực, mã lỗi); `AuthRouteConsistencyTest` xanh.
- Chạy lại bộ test bảo mật trên stack local: gỡ liên kết bằng access token trần bị `403 REAUTH_REQUIRED` khi cờ bật.

## Success Criteria
- [ ] Gỡ liên kết cần xác thực lại khi cờ bật và chủ tài khoản nhận email báo.
- [ ] Người dùng chỉ có Google đặt được mật khẩu khi đã đăng nhập, rồi gỡ được liên kết nếu muốn.
- [ ] Reauth token dùng một lần, gắn user và phiên, hết hạn sau 5 phút, không bị log.
- [ ] Contract mới và cập nhật có trong cùng PR; `mvn -q -pl services/user-service -am test`,
  `mvn -q -pl services/notification-service -am test`, `mvn -q -pl infra/api-gateway test` xanh.
