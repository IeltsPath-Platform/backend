# Phase 2 (Deploy sau Consumers): Producer `user-service` & `api-gateway`

## 1. API `DELETE /api/users/me` (`user-service`)

### Request & Xác thực
- Endpoint: `DELETE /api/users/me` trong `UserController` (lấy `userId` qua `CurrentUserProvider.requireUserId()`).
- Request body (`DeleteAccountRequest`):
  - `currentPassword` (String, nullable)
  - `googleIdToken` (String, nullable)
  - `nonce` (không bắt buộc trong body vì Google ID token đã chứa claim `nonce`, giống `ReauthRequest`).
- Luật xác thực (tái dùng `GoogleIdTokenVerifier` / `NimbusGoogleIdTokenVerifier`, `GoogleNonceStore`, `OAuthIdentityRepository`, `PasswordChangeAttemptRepository` tương tự `ReauthUseCase`):
  - Tài khoản có `passwordHash != null` (kể cả vừa có mật khẩu vừa liên kết Google): bắt buộc gửi `currentPassword`, kiểm tra giới hạn sai mật khẩu (`PasswordChangeAttempts` — khóa 15 phút nếu sai quá ngưỡng), khớp bằng `PasswordEncoder.matches`.
  - Tài khoản chỉ có Google (`passwordHash == null`): bắt buộc gửi `googleIdToken`, xác minh bằng `GoogleIdTokenVerifier.verify(googleIdToken)` + kiểm tra `nonce` qua `GoogleNonceStore` (theo `GoogleAuthProperties.isRequireNonce()`), và đối chiếu `google.subject()` với `oauth_identities` (`OAuthProvider.GOOGLE`) của chính `userId`.
  - Chặn tự xóa nếu là `ADMIN` đang hoạt động cuối cùng (`UserManagementRuleViolationException` `LAST_ADMIN`, tương tự `DeleteUserUseCase`).
- **Idempotency:** Nếu `user.getStatus() == UserStatus.DELETED`: trả `204 No Content` (không ném 500, không ghi trùng outbox). Lưu ý: nếu access token đã bị thu hồi ở Redis, Gateway trả `401 SESSION_REVOKED` trước khi vào `user-service`.

### Lỗi tiếng Việt & `code` ổn định
Theo AGENTS.md §3.3 và convention `GlobalExceptionHandler` của `user-service`:
- Sai mật khẩu hiện tại → `401 Unauthorized` (hoặc `400` theo `PasswordChangeException` hiện có; khuyến nghị `401` cho xóa tài khoản), `code = "INVALID_CURRENT_PASSWORD"`, message: `"Mật khẩu hiện tại không chính xác."`
- Khóa tạm do nhập sai mật khẩu nhiều lần → `429 Too Many Requests`, `code = "PASSWORD_ATTEMPTS_LIMITED"`, message: `"Đã nhập sai mật khẩu quá nhiều lần. Vui lòng thử lại sau 15 phút."`
- `googleIdToken` không hợp lệ / hết hạn / sai nonce → `401 Unauthorized`, `code = "GOOGLE_ID_TOKEN_INVALID"`, message: `"Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn. Vui lòng thử lại."`
- Tài khoản Google không khớp tài khoản đang đăng nhập → `403 Forbidden`, `code = "ACCOUNT_MISMATCH"`, message: `"Tài khoản Google không khớp với tài khoản đã liên kết."`
- Thiếu phương thức xác nhận phù hợp (gửi trống hoặc gửi mật khẩu cho tài khoản chỉ có Google) → `400 Bad Request`, `code = "ACCOUNT_DELETION_CONFIRMATION_REQUIRED"`, message: `"Vui lòng xác nhận bằng mật khẩu hiện tại hoặc tài khoản Google để xóa tài khoản."`
- Admin cuối cùng → `409 Conflict`, `code = "LAST_ADMIN"`, message: `"Không thể xóa quản trị viên đang hoạt động cuối cùng."`

## 2. Xử lý dữ liệu & Outbox trong `user-service`

- **Migration mới `V20__account_deletion.sql`** (kiểm tra `origin/main` trước khi đặt số):
  - `ALTER TABLE users ADD COLUMN deleted_at TIMESTAMPTZ;`
  - Cập nhật `chk_users_status` thêm `'DELETED'`.
- **Domain (`UserStatus.DELETED`, `User.markDeleted(...)`):**
  - Đổi `status = UserStatus.DELETED`, đặt `deletedAt = now()`.
  - Ẩn danh PII (tùy chọn chốt ở câu hỏi 3):
    - Nếu cho đăng ký lại cùng email: `email = "deleted+" + id + "@deleted.local"`, `fullName = "Người dùng đã xóa"`, `phoneNumber = null`, `passwordHash = null`, `passwordClearedAt = null`, `emailVerifiedAt = null`.
    - Nếu không cho đăng ký lại: giữ hash/cột kiểm tra email đã xóa (hoặc giữ email gốc) — cập nhật theo quyết định người dùng.
- **Dữ liệu liên quan trong cùng transaction (`DeleteMyAccountUseCase`):**
  - Thu hồi toàn bộ refresh token: `refreshTokens.revokeAllActiveForUser(userId, now, "ACCOUNT_DELETED")`.
  - Xóa `oauth_identities` của `userId`, `account_action_tokens` của `userId`, `password_change_attempts` của `userId`, `login_history` của `userId` (hoặc ẩn danh).
  - Xóa/ẩn danh `learner_profiles` (`display_name = 'Người dùng đã xóa'`, `avatar_reference = null`, `bio = null`, `self_reported_band = null`).
  - Ghi audit `ACCOUNT_DELETED` vào `auth_audit_events` (không chứa PII/token).
  - Ghi event `UserDeletedV1` vào `outbox_events` **trong cùng `@Transactional`** (bảng `outbox_events` đã có từ V13 với `correlation_id` và `trace_context`; bổ sung `UserDeletedV1.EVENT_TYPE` vào `RabbitUserEventPublisher.SUPPORTED_EVENT_TYPES`).
- **Chặn đăng nhập / refresh / khôi phục:**
  - `LoginUseCase`, `RefreshTokenUseCase`, `GoogleSignInUseCase`, `ForgotPasswordUseCase`, `ResetPasswordUseCase`, `EmailVerificationUseCase` đều kiểm `status != UserStatus.ACTIVE` (đã chặn `DELETED` tự nhiên; cần thêm test xác nhận `DELETED` không login/refresh/Google sign-in được).
- **Xóa avatar trên Supabase Storage (`avatars` bucket):**
  - Đọc `avatarReference` trước khi xóa; gọi `AvatarStorage.delete(avatarReference)` **sau commit** (`TransactionSynchronization.afterCommit`) hoặc trong best-effort cleanup ngoài transaction/lock để không giữ DB lock khi gọi HTTP storage (AGENTS.md §5).
- **Thu hồi Access Token đang sống (1 giờ):**
  - `user-service` đã có `RedisAccessTokenRevocationStore.revokeUser(userId, Instant.now())` (ghi `auth:revoked:user:{userId}` sau commit).
  - `api-gateway` đã có `RevocationChecker` kiểm tra khóa này ở mọi request có JWT (`docs/contracts/auth-token-revocation-v1.md`) → access token đang sống bị chặn ngay với `401 SESSION_REVOKED`.
  - *Trade-off:* Mặc định Gateway chạy `fail-open` nếu Redis mất kết nối (tăng metric `gateway.auth.revocation.errors`), tối đa 1 giờ (thời hạn access token) nếu Redis sập hoàn toàn; bật `GATEWAY_AUTH_REVOCATION_FAIL_CLOSED=true` để chặn tuyệt đối (trả `503 AUTH_UNAVAILABLE`).

## 3. `api-gateway`
- Trong `infra/config-server/config-repo/api-gateway.yaml`: thêm route `user-delete-account-limited` đặt **trước** route `user-service`:
  - `predicates`: `Path=/api/users/me`, `Method=DELETE`
  - `filters`: `ClientRateLimit` với `tier: auth` (vì endpoint nhận mật khẩu / Google ID token, cần giới hạn chống brute-force).
- Cập nhật `AuthRouteConsistencyTest` kiểm tra `user-delete-account-limited` dùng `tier=auth` và `Method=DELETE`.

## Kiểm chứng
- `mvn -q -pl services/user-service -am test` (kèm `ArchitectureTest`)
- `mvn -q -pl infra/api-gateway test`
