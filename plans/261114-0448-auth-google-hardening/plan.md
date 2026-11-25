---
title: "Nâng cấp xác thực (đăng nhập mật khẩu, phiên, token) và đăng nhập Google"
description: "Phát hiện dùng lại refresh token, thu hồi access token, quản lý phiên, chính sách và băm mật khẩu, audit; chống phát lại ID token Google, xác thực lại trước khi gỡ liên kết, đặt mật khẩu cho tài khoản chỉ có Google; kèm phần frontend web"
status: in_progress
priority: P1
branch: "fix/login-gg"
tags: [auth, security, google, session, user-service, api-gateway, frontend]
blockedBy: []
blocks: []
created: "2026-11-14T04:48:00.000Z"
updated: "2026-11-25T00:00:00.000Z"
---

# Nâng cấp xác thực và đăng nhập Google

Người dùng chốt ngày 2026-11-14: làm hai nhóm hạng mục (A: xác thực chung, G: Google sign-in). **Đã loại khỏi phạm vi:**
xác thực hai lớp (MFA) và đổi cơ chế ký JWT (HS256 dùng chung, `kid`, RS256/ES256). Lô đầu gồm phase 1, 4 và 6 đã
được triển khai; các phase còn lại vẫn theo trạng thái trong bảng bên dưới.

## Quyết định đã chốt (2026-11-25)

| Quyết định | Nội dung |
| --- | --- |
| Phạm vi | **Backend và frontend web** (repo `frontend/`). Ứng dụng mobile làm sau, ngoài plan này |
| Contract | **Duyệt tất cả** mục trong bảng "Đã duyệt" bên dưới |
| Danh sách chặn mật khẩu | **Danh sách nhỏ tự soạn** (vài trăm mục), không tải danh sách 10 000 mục |
| Kiểm tra mật khẩu bị lộ (HIBP) | **Bỏ**. Không làm, không để cờ |
| CAPTCHA (A7, cũ là phase 9) | **Bỏ**. Chuyển vào backlog; giới hạn tốc độ hiện có (Gateway theo IP, `RedisLoginAttemptLimiter` theo email+IP) giữ nguyên |
| Đặt mật khẩu lần đầu thành công | **Xóa `password_cleared_at`** (cột mới ở `main`, migration V16) |
| Thông báo lỗi | Mọi thông báo lỗi trả cho client là **tiếng Việt có dấu**, `code` ổn định, dịch ngay tại nơi ném lỗi (AGENTS §3.3). Bảng ở mục "Thông báo lỗi tiếng Việt" |

## Bước 0 (bắt buộc, làm trước mọi phase backend)

Nhánh `fix/login-gg` tụt sau `origin/main` khoảng 111 commit (đã kiểm 2026-11-25). `main` đã sửa các file mà plan này sẽ
sửa: `ForgotPasswordUseCase`, `User`, `UserJpaEntity`, `UserJpaRepository`, `RabbitUserEventPublisher`,
`GlobalExceptionHandler`, `ClientRateLimitGatewayFilterFactory`; thêm `VietnameseErrorMessages`,
`ValidationMessages.properties`, callback dữ liệu demo và migration `V16__users_password_cleared_at.sql`.

1. `git fetch origin` rồi `git merge origin/main` vào `fix/login-gg` (merge, không rebase vì nhánh đã đẩy). Giải quyết xung
   đột, chạy `mvn -q -pl services/user-service -am test` và `mvn -q -pl infra/api-gateway test` để biết nền xanh trước khi
   thêm gì.
2. Đọc lại `AGENTS.md` (§3.3 thông báo tiếng Việt, §3.9 pattern bắt buộc) và `VietnameseErrorMessages` trên `main`: lớp này
   là lưới an toàn, nhưng quy ước là dịch tại nguồn.
3. Đối chiếu số migration (xem "Quy ước chung"): user-service sau merge kết thúc ở V16, nên phase 2 dùng **V17**, phase 8
   dùng **V18**.

## Hiện trạng (đã đọc code, một phần đã chạy thử)

| Dữ kiện | Nơi | Hệ quả |
| --- | --- | --- |
| Refresh token dùng một lần: `consumeActiveToken` đặt `revoked_at`; token đã dùng chỉ bị từ chối, không có phản ứng thêm | `RefreshTokenUseCase`, `RefreshTokenJpaRepository` | A1: cần biết token đã dùng do xoay vòng hay do đăng xuất để phát hiện dùng lại |
| Bảng `refresh_tokens` chỉ có `id, user_id, token_hash, expires_at, revoked_at, created_at` | `V1__create_user_tables.sql` | A1, A3: thiếu `family_id`, lý do thu hồi, IP, user agent, `last_used_at` |
| Access token (external) sống 3600 giây, **không có `jti`, không có `iat`** (`JwtTokenService` xóa `iat`) | `JwtTokenService`, `user-service.yaml` | A2: không thu hồi được từng token; cần thêm claim |
| Gateway xác thực JWT bằng `ReactiveJwtDecoder` và đã có Redis reactive (rate limit) | `SecurityConfig`, `api-gateway/pom.xml` | A2: kiểm tra thu hồi đặt tại Gateway; service chỉ nhận internal JWT sống 60 giây nên không cần kiểm thêm |
| Mật khẩu: `MIN_LENGTH = 6`, tối đa 72 byte, không có danh sách chặn | `PasswordPolicy` | A4 |
| `new BCryptPasswordEncoder()` mặc định (cost 10); tài khoản demo và dữ liệu cũ là `$2a$10$` | `AuthTokenConfig` | A5: Argon2 cần thư viện mới nên **không** chọn; dùng BCrypt cost 12 và băm lại khi đăng nhập |
| `LoginUseCase` chỉ chạy BCrypt khi có hash → email không tồn tại, tài khoản chỉ có Google trả lời nhanh hơn | `LoginUseCase` | A6 |
| `login_history` có `ip_address`, `user_agent`; chưa có API cho người dùng | `V12__create_login_history.sql` | A3, A8 |
| Google: không kiểm `nonce`, `iat`; kiểm `iss`, `aud`, `azp`, `exp`, `email_verified`, `hd` | `NimbusGoogleIdTokenVerifier` | G1 |
| `isEmailAuthoritative` chạy **trước** khi tra theo `sub` | `LinkGoogleAccountUseCase` | G6 |
| `DELETE /api/users/me/oauth/{provider}` chỉ cần access token | `OAuthIdentityController`, `UnlinkOAuthIdentityUseCase` | G3 |
| `ChangePasswordUseCase` ném `PasswordNotConfiguredException` (409) cho tài khoản chưa có mật khẩu | `ChangePasswordUseCase` | G5 |
| Contract nói chỉ phát `user.google-account-linked.v1` khi liên kết xóa mật khẩu; code phát cho mọi lần liên kết vào tài khoản đã có | `google-sign-in-v1.md`, `user-google-account-linked-v1.md`, `LinkGoogleAccountUseCase` | G4: sửa tài liệu cho khớp code |
| Notification đã có `GoogleAccountLinkedListener` và `MailTemplate` | `notification-service` | G3: email báo gỡ liên kết nhân bản mẫu này |
| Cookie auth: `APP_AUTH_COOKIE_SECURE` mặc định `false`, Compose không đặt; không có profile production được định nghĩa | `AuthCookieProperties`, `user-service.yaml` | G2: dùng cờ riêng, không dựa vào profile |
| Một số exception Google và giới hạn đăng nhập đang trả thông báo **tiếng Anh** | `Google*Exception`, `LoginRateLimitedException`, `LoginLimiterUnavailableException`, `NimbusGoogleIdTokenVerifier` | Vi phạm AGENTS §3.3: dịch ở phase 1 |
| Chưa có endpoint tự đổi email | `UserController` (chỉ `fullName`, `phoneNumber`) | A9 để backlog |
| Frontend web còn tối thiểu mật khẩu 6, kiểm độ mạnh `pass.length >= 6`, gỡ Google không xác thực lại, Google sign-in không gửi nonce | `frontend/src/features/auth/**`, `features/profile/components/SecuritySection.tsx` | Phase 10 |

## Phạm vi

| Mã | Hạng mục | Phase |
| --- | --- | --- |
| A6 | Chống dò email qua thời gian phản hồi | 1 |
| G2 | Cookie `Secure`: bắt buộc ở production | 1 |
| G4 | Sửa contract lệch với code | 1 |
| G8 | Metric Google sign-in, `.env.example`, cảnh báo thiếu cấu hình | 1 |
| i18n | Dịch thông báo lỗi tiếng Anh còn sót (xem bảng thông báo) | 1 |
| A1 | Phát hiện dùng lại refresh token | 2 |
| A3 | Quản lý phiên và thiết bị | 2 |
| A2 | Thu hồi access token, đăng xuất thật sự | 3 |
| A4 | Chính sách mật khẩu (danh sách chặn nhỏ) | 4 |
| A5 | Băm mật khẩu cost 12 và băm lại | 4 |
| G1 | Chống phát lại ID token (`iat`, `nonce`) | 5 |
| G6 | Cho `sub` đã liên kết đăng nhập dù miền email đổi | 6 |
| G3 | Xác thực lại trước khi gỡ liên kết, email báo gỡ | 7 |
| G5 | Đặt mật khẩu lần đầu cho tài khoản chỉ có Google | 7 |
| A8 | Audit sự kiện nhạy cảm, metric, cảnh báo | 8 |
| FE | Frontend web cho tất cả mục trên | 10 |
| A7 | CAPTCHA | **Bỏ** (backlog) |
| A9, G7 | Đổi email an toàn; chuẩn hóa email Gmail | Backlog |
| Mobile | App di động dùng nonce, phiên, reauth | Sau, ngoài plan |

## Đã duyệt (AGENTS.md §5: đổi contract, JWT claim, route công khai) — 2026-11-25

| Việc | Ghi chú giảm thiểu |
| --- | --- |
| Thêm claim `jti`, `iat`, `sid` vào external JWT và `sid` vào internal JWT (phase 3) | Chỉ thêm, không đổi claim cũ; Gateway và service cũ vẫn chấp nhận token có thêm claim; ghi `docs/contracts/auth-token-revocation-v1.md`; `shared/common-security` chỉ thêm `currentSessionId()` |
| Endpoint mới `/api/users/me/sessions` (phase 2), `/api/users/me/reauth`, `/api/users/me/password/initial` (phase 7), `/api/users/me/security-events` (phase 8), `GET /api/users/{id}/auth-events` (ADMIN, phase 8) | Tất cả cần JWT, có contract; chỉ thêm, không đổi endpoint cũ |
| Đổi `MIN_LENGTH` 6 → 8 (phase 4) | Chỉ áp cho mật khẩu mới; người dùng cũ không bị buộc đổi; có cờ cấu hình `app.auth.password.min-length` |
| `DELETE /api/users/me/oauth/{provider}` yêu cầu `X-Reauth-Token` (phase 7) | Cờ `app.auth.reauth-required-for-unlink` mặc định `false` trong code; chủ dự án bật trong môi trường **sau khi** frontend web phase 10 đã triển khai |
| Route công khai `POST /auth/google/nonce` (phase 5) và route có JWT `POST /api/users/me/reauth` (phase 7) | Gán tier `auth`, cập nhật `AuthRouteConsistencyTest` (AGENTS §3.9) |
| Hạ `ACCESS_TOKEN_MAX_AGE_SECONDS` 3600 → 900 (phase 3) | Chỉ là biến môi trường; **chủ dự án** tự đổi sau khi xác nhận frontend refresh khi gặp 401 (xem "Việc chủ dự án tự làm") |

**Không còn mục nào cần duyệt thêm.** CAPTCHA và kiểm tra mật khẩu bị lộ đã bỏ nên không có dịch vụ ngoài mới.

## Phases

| Phase | Việc | Phụ thuộc | Status |
| --- | --- | --- | --- |
| 1 | [Nền tảng: A6, G2, G4, G8, dịch thông báo](./phase-01-foundation-quick-fixes.md) | Bước 0 | Done |
| 2 | [Refresh token: A1, A3](./phase-02-refresh-token-families-and-sessions.md) | 1 | Pending |
| 3 | [Thu hồi access token: A2](./phase-03-access-token-revocation.md) | 2 | Pending |
| 4 | [Mật khẩu: A4, A5](./phase-04-password-policy-and-hashing.md) | Bước 0 | Done |
| 5 | [ID token Google: G1](./phase-05-google-id-token-replay.md) | 1 | Pending |
| 6 | [Đủ điều kiện đăng nhập Google: G6](./phase-06-google-eligibility-by-subject.md) | Bước 0 | Done |
| 7 | [Xác thực lại, gỡ liên kết, đặt mật khẩu: G3, G5](./phase-07-reauth-unlink-set-password.md) | 2, 3, 4 | Pending |
| 8 | [Audit và quan sát: A8](./phase-08-audit-and-observability.md) | 2, 7 | Pending |
| 9 | [CAPTCHA có điều kiện: A7](./phase-09-optional-captcha.md) | - | **Dropped** |
| 10 | [Frontend web](./phase-10-frontend-web.md) | từng nhóm theo backend (xem file) | Pending |

## Thứ tự thực thi (mỗi lô một PR, mỗi lô xanh trước khi sang lô sau)

| Lô | Phase | Lý do gom |
| --- | --- | --- |
| 1 | 1 + 4 + 6 | Độc lập, không migration, rủi ro thấp; có metric và chính sách mật khẩu ngay |
| 2 | 2 | Có migration V17; nền của phiên và thu hồi |
| 3 | 3 + 5 | Gateway + claim JWT; ID token Google (cùng chạm Redis và Gateway) |
| 4 | 7 + 8 | Reauth, gỡ liên kết, đặt mật khẩu, audit (audit ghi các sự kiện của phase 7) |
| FE | 10 | Chia 4 đợt bám sau lô backend tương ứng (xem phase 10); repo `frontend/`, nhánh riêng |

Phase 4 và 6 độc lập; trong lô 1 làm theo thứ tự 1 → 4 → 6.

## Việc chủ dự án tự làm (Codex không làm, không đánh dấu hoàn thành thay)

Các bước dưới cần mắt người, môi trường thật hoặc quyền triển khai. Codex chỉ ghi vào báo cáo cuối "chưa chạy, chờ chủ
dự án".

| Khi nào | Việc |
| --- | --- |
| Sau lô 1 | Đo tay thời gian đăng nhập: 50 lần cho mỗi loại (email không tồn tại, không ACTIVE, chỉ Google, sai mật khẩu); hai nhóm lệch dưới 20% trung vị (phase 1) |
| Sau lô 1 | Đo thời gian một lần băm BCrypt trên máy đích, ghi vào `docs/deployment.md`; chọn cost 12 nếu dưới khoảng 350 ms, không thì 11 qua biến môi trường (phase 4) |
| Sau lô 1 | Kiểm `auth_google_signin_total{result=...}` trên Prometheus sau một lần gọi `/auth/google` sai token (phase 1) |
| Sau lô 2, 3, 4 | Chạy bộ test bảo mật trên stack local: các case `TC-SEC-001..015`, `041`, `046..050` và case mới của phase; kịch bản tay: đăng nhập → đăng xuất → dùng lại access token cũ phải 401; đổi mật khẩu ở thiết bị A làm token thiết bị B mất hiệu lực |
| Sau FE đợt phiên | Xác nhận trên trình duyệt: access token hết hạn → tự refresh → request cũ chạy tiếp. **Chỉ sau đó** mới đặt `ACCESS_TOKEN_MAX_AGE_SECONDS=900` |
| Sau FE đợt reauth | Triển khai frontend rồi mới đặt `APP_AUTH_REAUTH_REQUIRED_FOR_UNLINK=true` |
| Khi mobile sẵn sàng | Khi metric đăng nhập Google không nonce về 0, đặt `APP_AUTH_GOOGLE_REQUIRE_NONCE=true` (không làm khi mobile chưa gửi nonce) |
| Trước production | Đặt `APP_AUTH_COOKIE_SECURE=true` và `APP_AUTH_COOKIE_ENFORCE_SECURE=true` trong env production |

## Thông báo lỗi tiếng Việt (áp cho mọi phase)

Quy tắc: thông báo có dấu, không nhắc lại mật khẩu, token, ID token, nonce hay dữ liệu của người khác; `code` là mã
ổn định cho frontend phân nhánh, không đổi khi đổi chữ. Mã nằm ở `code` **cấp cao nhất** của `ErrorResponse` (frontend
`HttpError.code` đọc đúng chỗ này). Một số handler cũ đặt mã trong `details.code` (xem `GlobalExceptionHandler`); mã mới
**không** làm vậy. `details` chỉ chứa lỗi theo từng trường (ví dụ `details.password`) và `details.reasons`. Trước khi thêm mã mới, Codex đọc `GlobalExceptionHandler` và
`VietnameseErrorMessages` trên `main` để dùng đúng kiểu `details` (nếu `details` là `Map<String,String>` thì `reasons` là
chuỗi phân cách bằng dấu phẩy, ví dụ `TOO_SHORT,TOO_COMMON`) và **giữ nguyên mã đang dùng** cho các lỗi đã có.

### Lỗi mới

| Tình huống | HTTP | `code` | Thông báo |
| --- | --- | --- | --- |
| Mật khẩu mới yếu | 400 | `WEAK_PASSWORD` (+ `reasons`) | "Mật khẩu chưa đủ mạnh." Lý do hiển thị ở frontend (xem bên dưới) |
| Thao tác nhạy cảm thiếu hoặc sai token xác thực lại | 403 | `REAUTH_REQUIRED` | "Vui lòng xác thực lại để thực hiện thao tác này." |
| Xác thực lại bằng Google nhưng tài khoản Google không khớp tài khoản đã liên kết | 403 | `REAUTH_ACCOUNT_MISMATCH` | "Tài khoản Google không khớp với tài khoản Google đã liên kết." |
| Xác thực lại bằng mật khẩu cho tài khoản chưa có mật khẩu | 409 | dùng mã `PasswordNotConfigured` hiện có | tái dùng thông báo hiện có |
| Xác thực lại sai mật khẩu / bị khóa tạm | 401 / 429 | tái dùng mã và thông báo của đổi mật khẩu hiện có (`PasswordChangeAttempts`) | giữ nguyên |
| Phiên đăng nhập không tồn tại hoặc của người khác | 404 | `SESSION_NOT_FOUND` | "Không tìm thấy phiên đăng nhập." |
| Đặt mật khẩu lần đầu khi đã có mật khẩu | 409 | `PASSWORD_ALREADY_SET` | "Tài khoản đã có mật khẩu. Hãy dùng chức năng đổi mật khẩu." |
| Đặt mật khẩu lần đầu khi email chưa xác minh | 403 | tái dùng mã hiện có | "Vui lòng xác minh email trước khi thực hiện thao tác này" (giữ nguyên chuỗi đang có) |
| Gateway: access token đã bị thu hồi | 401 | `SESSION_REVOKED` | "Phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại." |
| Gateway: kho thu hồi lỗi và `fail-closed=true` | 503 | `AUTH_UNAVAILABLE` | "Hệ thống tạm thời không xác minh được phiên đăng nhập. Vui lòng thử lại sau." |
| Đăng nhập Google tạm thời không dùng được (Redis nonce lỗi khi bắt buộc nonce) | 503 | `GOOGLE_SIGNIN_UNAVAILABLE` | "Đăng nhập Google tạm thời không khả dụng. Vui lòng thử lại sau." |
| ID token Google cũ, nonce sai, nonce đã dùng | 401 | giữ mã `Google ID token invalid` hiện có | "Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn. Vui lòng thử lại." (một thông báo chung, không lộ lý do) |
| Refresh token bị dùng lại / phiên hết hạn tuyệt đối | 401 | giữ mã hiện có | "Refresh token không hợp lệ" (giữ nguyên chuỗi đang có) |

### Lý do của `WEAK_PASSWORD` (frontend ánh xạ `reasons` sang câu này)

| `reason` | Câu hiển thị dưới ô nhập |
| --- | --- |
| `TOO_SHORT` | "Mật khẩu phải có ít nhất 8 ký tự." |
| `TOO_COMMON` | "Mật khẩu này quá phổ biến, hãy chọn mật khẩu khác." |
| `CONTAINS_ACCOUNT_INFO` | "Mật khẩu không được chứa email, họ tên hoặc số điện thoại của bạn." |
| `TOO_SIMPLE` | "Mật khẩu quá đơn giản, tránh chuỗi lặp hoặc liên tiếp (như 12345678)." |

### Thông báo tiếng Anh hiện có phải dịch tại nguồn (phase 1)

`GoogleAccountConflictException`, `GoogleAccountNotActiveException`, `GoogleEmailNotSupportedException`,
`GoogleSignInNotAllowedException`, `GoogleSignInUnavailableException`, `LoginLimiterUnavailableException`,
`LoginRateLimitedException`, và chuỗi "Google ID token is invalid" trong `NimbusGoogleIdTokenVerifier`. Giữ nguyên
`code`/HTTP status; chỉ đổi chuỗi sang tiếng Việt có dấu, rồi cập nhật test đang assert chuỗi cũ và các contract
trong `docs/contracts/` nếu có trích thông báo. Mỗi thông báo mới cần một test chứng minh chuỗi tiếng Việt ra tới response.

## Quy ước chung mọi phase

- **Pattern bắt buộc (AGENTS §3.9):** HTTP ra ngoài qua `ResilientCall`; sự kiện qua outbox (`UserNoticeOutbox`) với tên
  và payload ở `common-events`, contract ở `docs/contracts/`; `@Scheduled` mới an toàn nhiều instance (`FOR UPDATE SKIP
  LOCKED` hoặc thao tác lặp an toàn) và ghi vào bảng "Scheduler và nhiều instance" của `docs/system-architecture.md`;
  `X-Correlation-Id` giữ nguyên; không log token, mật khẩu, ID token, nonce.
- **Migration:** chỉ thêm, không sửa migration cũ, thay đổi tương thích ngược (thêm cột nullable hoặc có default) để triển
  khai từng bước. Số dự kiến: **V17** (phase 2, `refresh_token_sessions`) và **V18** (phase 8, `auth_audit_events`) vì
  `main` đã dùng V16. Trước khi mở mỗi PR chạy lại `git ls-tree origin/main services/user-service/src/main/resources/db/migration`
  và xem các nhánh đang mở để tránh trùng phiên bản (đã có sự cố va chạm số migration).
- **Kiểm chứng mỗi phase:** `mvn -q -pl services/user-service -am test`; phase chạm Gateway thêm
  `mvn -q -pl infra/api-gateway test`; phase chạm Notification thêm `mvn -q -pl services/notification-service -am test`;
  `ArchitectureTest` của từng service phải xanh (không tạo vi phạm mới). Test Testcontainers cần Docker; thiếu Docker thì
  nêu rõ là bị bỏ qua, không coi là đã chạy. Lỗi có sẵn trên `main` (ví dụ `EssayKnowledgeJudgeTest...` của learning-service)
  không thuộc plan này; báo riêng, không sửa lẫn.
- **Đo tay và kiểm thử trên stack thật** thuộc mục "Việc chủ dự án tự làm" ở trên, không phải việc của Codex.
- Commit theo conventional commit; không nhắc AI/agent, số phase hay mã plan trong commit, comment, tên test, migration.
- Mỗi phase đổi HTTP hoặc event cập nhật `docs/contracts/` trong cùng PR.
- Phase chạm frontend tuân theo quy ước giao diện của dự án (xem phase 10).

## Rủi ro

| Rủi ro | Giảm thiểu |
| --- | --- |
| Hai tab cùng refresh một lúc bị coi là "dùng lại" và bị đăng xuất cả chuỗi | Cửa sổ ân hạn `app.auth.refresh-reuse-grace-seconds` (mặc định 10): dùng lại trong cửa sổ chỉ trả 401, không thu hồi chuỗi |
| Redis chết làm Gateway không kiểm được danh sách thu hồi | Mặc định mở (fail-open) kèm metric và log cảnh báo; cờ `gateway.auth.revocation.fail-closed` cho môi trường cần chặt |
| Hạ thời hạn access token làm frontend bị 401 bất ngờ | Chủ dự án làm sau cùng, chỉ khi đã xác nhận trên trình duyệt; đổi bằng biến môi trường, quay lui không cần deploy code |
| Dùng lại nhiều thông tin phiên (IP, user agent) là dữ liệu cá nhân | Lưu tối thiểu như `login_history` hiện có; xóa theo hạn giữ; không đưa vào log |
| Tăng cost BCrypt làm đăng nhập chậm | Chủ dự án đo trước; giữ rate limit hiện có; băm lại dần khi đăng nhập, không chạy hàng loạt |
| Đổi contract mà client chưa cập nhật | Mọi thay đổi hành vi có cờ cấu hình hoặc chỉ thêm trường tùy chọn; bật cờ sau khi frontend (và mobile nếu liên quan) sẵn sàng |
| Danh sách chặn nhỏ bỏ sót mật khẩu yếu phổ biến | Chấp nhận có chủ đích (đã chốt); các luật độ dài, chuỗi đơn giản, trùng thông tin tài khoản vẫn chặn phần lớn; có thể mở rộng danh sách bằng cách thêm dòng vào tài nguyên |
| Merge `origin/main` vào `fix/login-gg` gây xung đột ở file auth | Làm Bước 0 riêng, một commit merge, test xanh trước khi thêm tính năng |
