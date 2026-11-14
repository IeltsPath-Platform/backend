---
phase: 2
title: "Refresh token: phát hiện dùng lại (A1) và quản lý phiên (A3)"
status: pending
priority: P1
dependencies: [1]
---

# Phase 2: Họ refresh token, phát hiện dùng lại, API phiên

## Vấn đề
- Mỗi lần refresh, token cũ bị đặt `revoked_at` nhưng không ghi lý do; không biết token đó đã bị xoay vòng hay đăng xuất.
  Kẻ trộm và chủ thật dùng cùng một token, người đến sau chỉ nhận 401, phiên của kẻ trộm vẫn sống.
- Người dùng không xem hay thu hồi được từng thiết bị. `login_history` có IP và user agent nhưng không có API.

## Thiết kế dữ liệu: `V16__refresh_token_sessions.sql`

```sql
ALTER TABLE refresh_tokens
    ADD COLUMN family_id        UUID        NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN session_started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN revoked_reason   VARCHAR(20),
    ADD COLUMN replaced_by_id   UUID,
    ADD COLUMN last_used_at     TIMESTAMP,
    ADD COLUMN ip_address       VARCHAR(45),
    ADD COLUMN user_agent       VARCHAR(255),
    ADD CONSTRAINT chk_refresh_tokens_revoked_reason CHECK (revoked_reason IS NULL OR revoked_reason IN
        ('ROTATED','LOGOUT','PASSWORD_CHANGE','PASSWORD_RESET','GOOGLE_LINK','SESSION_REVOKED','REUSE_DETECTED','ADMIN'));
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens (user_id, expires_at) WHERE revoked_at IS NULL;
```

- Mặc định `gen_random_uuid()` cho `family_id` để code cũ (đang chạy lúc triển khai) vẫn insert được; token đã có mỗi cái
  thành một họ riêng. Token cũ không có `revoked_reason` được coi là không do xoay vòng (không kích hoạt thu hồi họ).
- Không xóa hay sửa cột cũ; thay đổi tương thích ngược theo từng bước triển khai.

## Phát hiện dùng lại (A1)

`RefreshTokenUseCase.execute(token, LoginContext)` (thêm `LoginContext`; controller truyền `LoginContextResolver.from(request)`):

1. Tra theo hash, **kể cả token đã thu hồi**. Không thấy: 401.
2. Khóa user (`userLock.lock`) như hiện tại.
3. Token đã bị thu hồi:
   - lý do `ROTATED` và đã quá `app.auth.refresh-reuse-grace-seconds` (mặc định 10) kể từ `revoked_at`: **dùng lại** →
     thu hồi mọi token đang hoạt động của họ (`family_id`) với lý do `REUSE_DETECTED`, ghi metric `auth.refresh{result=reuse}`
     và audit (phase 8), rồi 401;
   - `ROTATED` trong cửa sổ ân hạn (hai tab refresh gần như cùng lúc): chỉ 401, không thu hồi;
   - lý do khác hoặc rỗng: chỉ 401.
4. Token còn hoạt động: `consumeActiveToken` như hiện tại (atomic), đặt lý do `ROTATED`, cấp token mới **cùng `family_id`
   và `session_started_at`**, ghi `replaced_by_id`, `last_used_at`, IP, user agent của lần dùng.
5. Phiên quá `app.auth.session-max-age-seconds` (mặc định 0 = không giới hạn; đề xuất 30 ngày khi bật) kể từ
   `session_started_at`: 401, thu hồi họ với lý do `SESSION_REVOKED`.

`AuthTokenIssuer.issue(user)` giữ chữ ký cũ cho đăng nhập mới (tạo họ mới); thêm
`issue(user, familyId, sessionStartedAt, context)` cho refresh. Mọi nơi gọi `revokeAllActiveForUser` truyền thêm lý do:
`ChangePasswordUseCase` (`PASSWORD_CHANGE`), `ResetPasswordUseCase` (`PASSWORD_RESET`), `LinkGoogleAccountUseCase`
(`GOOGLE_LINK`), `LogoutUseCase` (`LOGOUT`), `UserManagementController` đổi trạng thái/role (`ADMIN`).

## API phiên (A3)

| Method | Path | Ý nghĩa |
| --- | --- | --- |
| GET | `/api/users/me/sessions` | Danh sách phiên đang hoạt động (tối đa 50) |
| DELETE | `/api/users/me/sessions/{sessionId}` | Thu hồi một phiên (`sessionId` = `family_id`) |
| DELETE | `/api/users/me/sessions` | Thu hồi mọi phiên (tham số `exceptCurrent=true` có hiệu lực sau phase 3) |

- Quyền `CUSTOMER` và các role khác đều tự quản lý phiên của mình (`CurrentUserProvider`, không nhận id từ client).
- Mỗi phần tử: `sessionId`, `startedAt`, `lastUsedAt`, `ipAddress` (che octet cuối, ví dụ `203.0.113.x`), `userAgent`,
  `current` (`null` đến phase 3; sau đó so với claim `sid`).
- Phiên của người khác hoặc không tồn tại: **404** như nhau (không lộ sự tồn tại).
- Tier rate limit `default` (route `/api/users/**` đã có), không phải route công khai mới.
- Một token hoạt động duy nhất mỗi họ (xoay vòng thu hồi token trước) nên "phiên đang hoạt động" = token chưa thu hồi và
  chưa hết hạn; truy vấn có `LIMIT 50`, có chỉ mục `idx_refresh_tokens_user_active`.

## Dọn dữ liệu
`RefreshTokenCleanupJob` (`@Scheduled` hằng ngày) xóa token hết hạn hoặc thu hồi quá 30 ngày theo lô:
`DELETE ... WHERE id IN (SELECT id FROM refresh_tokens WHERE ... ORDER BY id LIMIT 1000 FOR UPDATE SKIP LOCKED)`.
An toàn nhiều instance nhờ `SKIP LOCKED`; ghi một dòng vào bảng "Scheduler và nhiều instance" của
`docs/system-architecture.md`. Giữ 30 ngày để vẫn phát hiện dùng lại token cũ.

## Files
- Mới: `V16__refresh_token_sessions.sql`, `api/controller/SessionController.java`, `api/dto/response/SessionResponse.java`,
  `application/usecase/{ListMySessions,RevokeMySession,RevokeAllMySessions}UseCase.java`,
  `infrastructure/scheduler/RefreshTokenCleanupJob.java`, `docs/contracts/auth-sessions-v1.md`.
- Sửa: `domain/aggregate/RefreshToken.java` (thêm trường và `revoke(reason)`), `RefreshTokenRepository` +
  `RefreshTokenRepositoryAdapter` + `RefreshTokenJpaRepository` + `RefreshTokenJpaEntity`, `RefreshTokenUseCase`,
  `AuthTokenIssuer`, `LogoutUseCase`, `AuthController.refresh`, các use case thu hồi nêu trên,
  `infra/config-server/config-repo/user-service.yaml` (`app.auth.refresh-reuse-grace-seconds`,
  `app.auth.session-max-age-seconds`).

## Tests
- `RefreshTokenUseCaseTest`: xoay vòng giữ `family_id`; dùng lại sau ân hạn thu hồi cả họ và trả 401; dùng lại trong ân
  hạn không thu hồi; token bị thu hồi do `LOGOUT` không kích hoạt thu hồi họ; user không ACTIVE; phiên quá hạn tuyệt đối.
- Integration (Testcontainers): `RefreshTokenFamilyIT` hai request refresh đồng thời cùng một token: đúng một thành công;
  migration V16 chạy trên dữ liệu cũ (token cũ thành họ riêng).
- `@WebMvcTest SessionController`: cần xác thực; danh sách che IP; thu hồi phiên của người khác trả 404; thu hồi phiên rồi
  refresh bằng token của phiên đó trả 401.
- Job dọn dữ liệu: hai instance chạy song song không xóa trùng, không xóa token còn hiệu lực.

## Success Criteria
- [ ] Gửi lại refresh token đã xoay vòng sau 10 giây làm mọi phiên cùng họ mất hiệu lực và có metric/audit.
- [ ] Hai tab refresh cùng lúc không bị đăng xuất.
- [ ] Người dùng liệt kê và thu hồi được từng phiên; phiên đã thu hồi không refresh được.
- [ ] Migration V16 chạy trên bản sao dữ liệu cũ không lỗi; code cũ và mới cùng chạy được trong lúc triển khai.
- [ ] `ArchitectureTest` xanh; `docs/contracts/auth-sessions-v1.md` đã viết cùng PR.
