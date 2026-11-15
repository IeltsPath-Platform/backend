---
title: "Nâng cấp xác thực (đăng nhập mật khẩu, phiên, token) và đăng nhập Google"
description: "Phát hiện dùng lại refresh token, thu hồi access token, quản lý phiên, chính sách và băm mật khẩu, audit; chống phát lại ID token Google, xác thực lại trước khi gỡ liên kết, đặt mật khẩu cho tài khoản chỉ có Google"
status: pending
priority: P1
branch: "fix/login-gg"
tags: [auth, security, google, session, user-service, api-gateway]
blockedBy: []
blocks: []
created: "2026-11-14T04:48:00.000Z"
---

# Nâng cấp xác thực và đăng nhập Google

Người dùng chốt ngày 2026-11-14: làm hai nhóm hạng mục (A: xác thực chung, G: Google sign-in). **Đã loại khỏi phạm vi:**
xác thực hai lớp (MFA) và đổi cơ chế ký JWT (HS256 dùng chung, `kid`, RS256/ES256). Chưa có code nào được viết; plan này
chỉ chốt thiết kế và thứ tự.

## Hiện trạng (đã đọc code, một phần đã chạy thử)

| Dữ kiện | Nơi | Hệ quả |
| --- | --- | --- |
| Refresh token dùng một lần: `consumeActiveToken` đặt `revoked_at`; token đã dùng chỉ bị từ chối, không có phản ứng thêm | `RefreshTokenUseCase`, `RefreshTokenJpaRepository` | A1: cần biết token đã dùng do xoay vòng hay do đăng xuất để phát hiện dùng lại |
| Bảng `refresh_tokens` chỉ có `id, user_id, token_hash, expires_at, revoked_at, created_at` | `V1__create_user_tables.sql` | A1, A3: thiếu `family_id`, lý do thu hồi, IP, user agent, `last_used_at` |
| Access token (external) sống 3600 giây, **không có `jti`, không có `iat`** (`JwtTokenService` xóa `iat`) | `JwtTokenService`, `user-service.yaml` | A2: không thu hồi được từng token; cần thêm claim (đổi contract JWT, xem Cần duyệt) |
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
| Migration user-service mới nhất là V15 | `db/migration` | Migration mới bắt đầu từ V16 |
| Chưa có endpoint tự đổi email | `UserController` (chỉ `fullName`, `phoneNumber`) | A9 để backlog |

## Phạm vi

| Mã | Hạng mục | Phase |
| --- | --- | --- |
| A6 | Chống dò email qua thời gian phản hồi | 1 |
| G2 | Cookie `Secure`: bắt buộc ở production | 1 |
| G4 | Sửa contract lệch với code | 1 |
| G8 | Metric Google sign-in, `.env.example`, cảnh báo thiếu cấu hình | 1 |
| A1 | Phát hiện dùng lại refresh token | 2 |
| A3 | Quản lý phiên và thiết bị | 2 |
| A2 | Thu hồi access token, đăng xuất thật sự | 3 |
| A4 | Chính sách mật khẩu | 4 |
| A5 | Băm mật khẩu cost 12 và băm lại | 4 |
| G1 | Chống phát lại ID token (`iat`, `nonce`) | 5 |
| G6 | Cho `sub` đã liên kết đăng nhập dù miền email đổi | 6 |
| G3 | Xác thực lại trước khi gỡ liên kết, email báo gỡ | 7 |
| G5 | Đặt mật khẩu lần đầu cho tài khoản chỉ có Google | 7 |
| A8 | Audit sự kiện nhạy cảm, metric, cảnh báo | 8 |
| A7 | CAPTCHA có điều kiện (**cần duyệt**) | 9 (tùy chọn) |
| A9, G7 | Đổi email an toàn; chuẩn hóa email Gmail | Backlog |

## Cần duyệt (AGENTS.md §5: không tự đổi contract, JWT claim, route công khai)

| Việc | Vì sao cần duyệt | Giảm thiểu |
| --- | --- | --- |
| Thêm claim `jti`, `iat`, `sid` vào external JWT và `sid` vào internal JWT (phase 3) | Đổi JWT claim contract | Chỉ thêm, không đổi claim cũ; Gateway và service cũ vẫn chấp nhận token có thêm claim; ghi `docs/contracts/auth-token-revocation-v1.md`; `shared/common-security` chỉ thêm `currentSessionId()` |
| Thêm endpoint mới: `/api/users/me/sessions` (phase 2), `/api/users/me/reauth`, `/api/users/me/password/initial` (phase 7), `/api/users/me/security-events` (phase 8) | Mở rộng API công khai | Tất cả cần JWT, có contract; chỉ thêm, không đổi endpoint cũ |
| Đổi `MIN_LENGTH` 6 → 8 (phase 4) | Đổi hành vi API đăng ký/đổi/đặt lại mật khẩu | Chỉ áp cho mật khẩu mới; người dùng cũ không bị buộc đổi; có cờ cấu hình |
| `DELETE /api/users/me/oauth/{provider}` yêu cầu `X-Reauth-Token` (phase 7) | Đổi hành vi endpoint hiện có (thêm 403 `REAUTH_REQUIRED`) | Cờ `app.auth.reauth-required-for-unlink` (mặc định `false` đến khi frontend sẵn sàng); ghi contract |
| Route công khai mới `/auth/google/nonce` (phase 5), `POST /api/users/me/reauth` (phase 7) | Route và tier rate limit mới ở Gateway | Gán tier `auth`, cập nhật `AuthRouteConsistencyTest` (AGENTS §3.9) |
| CAPTCHA (phase 9) | Gọi dịch vụ ngoài mới | Tùy chọn, tắt mặc định, qua `ResilientCall`; chỉ làm khi được duyệt |
| Hạ `ACCESS_TOKEN_MAX_AGE_SECONDS` 3600 → 900 (phase 3) | Frontend phải tự refresh trước khi hết hạn | Chỉ là biến môi trường; đổi sau khi frontend xác nhận xử lý 401 → refresh |

## Phases

| Phase | Việc | Phụ thuộc | Status |
| --- | --- | --- | --- |
| 1 | [Nền tảng: A6, G2, G4, G8](./phase-01-foundation-quick-fixes.md) | - | Pending |
| 2 | [Refresh token: A1, A3](./phase-02-refresh-token-families-and-sessions.md) | 1 | Pending |
| 3 | [Thu hồi access token: A2](./phase-03-access-token-revocation.md) | 2 | Pending |
| 4 | [Mật khẩu: A4, A5](./phase-04-password-policy-and-hashing.md) | - | Pending |
| 5 | [ID token Google: G1](./phase-05-google-id-token-replay.md) | 1 | Pending |
| 6 | [Đủ điều kiện đăng nhập Google: G6](./phase-06-google-eligibility-by-subject.md) | - | Pending |
| 7 | [Xác thực lại, gỡ liên kết, đặt mật khẩu: G3, G5](./phase-07-reauth-unlink-set-password.md) | 2, 3, 4 | Pending |
| 8 | [Audit và quan sát: A8](./phase-08-audit-and-observability.md) | 2, 7 | Pending |
| 9 | [CAPTCHA có điều kiện: A7](./phase-09-optional-captcha.md) | 8, được duyệt | Pending |

Phase 4 và 6 độc lập, có thể làm song song với các phase còn lại.

## Quy ước chung mọi phase

- **Pattern bắt buộc (AGENTS §3.9):** HTTP ra ngoài qua `ResilientCall`; sự kiện qua outbox (`UserNoticeOutbox`) với tên
  và payload ở `common-events`, contract ở `docs/contracts/`; `@Scheduled` mới an toàn nhiều instance (`FOR UPDATE SKIP
  LOCKED` hoặc thao tác lặp an toàn) và ghi vào bảng "Scheduler và nhiều instance" của `docs/system-architecture.md`;
  `X-Correlation-Id` giữ nguyên; không log token, mật khẩu, ID token, nonce.
- **Migration:** chỉ thêm V16 trở đi (V16 ở phase 2, V17 ở phase 8), không sửa migration cũ, thay đổi tương thích ngược
  (thêm cột nullable hoặc có default) để triển khai từng bước. Số V16/V17 chỉ là dự kiến: trước khi mở PR đối chiếu với
  `main` mới nhất và các nhánh đang mở để tránh trùng phiên bản (đã có sự cố va chạm số migration gần đây).
- **Kiểm chứng mỗi phase:** `mvn -q -pl services/user-service -am test`; phase chạm Gateway thêm
  `mvn -q -pl infra/api-gateway test`; phase chạm Notification thêm `mvn -q -pl services/notification-service -am test`;
  `ArchitectureTest` của từng service phải xanh (không tạo vi phạm mới). Test Testcontainers cần Docker; thiếu Docker thì
  nêu rõ là bị bỏ qua, không coi là đã chạy.
- **Test bảo mật chạy lại** trên stack local sau phase 2, 3, 5, 7: các case `TC-SEC-001..015`, `041`, `046..050` trong
  sheet SecurityTest (xem báo cáo test tích hợp) và các case mới của từng phase.
- Commit theo conventional commit; không nhắc AI/agent, số phase hay mã plan trong commit, comment, tên test, migration.
- Mỗi phase đổi HTTP hoặc event cập nhật `docs/contracts/` trong cùng PR.

## Rủi ro

| Rủi ro | Giảm thiểu |
| --- | --- |
| Hai tab cùng refresh một lúc bị coi là "dùng lại" và bị đăng xuất cả chuỗi | Cửa sổ ân hạn `app.auth.refresh-reuse-grace-seconds` (mặc định 10): dùng lại trong cửa sổ chỉ trả 401, không thu hồi chuỗi |
| Redis chết làm Gateway không kiểm được danh sách thu hồi | Mặc định mở (fail-open) kèm metric và log cảnh báo; cờ `gateway.auth.revocation.fail-closed` cho môi trường cần chặt |
| Hạ thời hạn access token làm frontend bị 401 bất ngờ | Làm sau cùng, chỉ khi frontend xác nhận; đổi bằng biến môi trường, quay lui không cần deploy code |
| Dùng lại nhiều thông tin phiên (IP, user agent) là dữ liệu cá nhân | Lưu tối thiểu như `login_history` hiện có; xóa theo hạn giữ; không đưa vào log |
| Tăng cost BCrypt làm đăng nhập chậm | Đo trước (cost 12 khoảng 200–300 ms); giữ rate limit hiện có; băm lại dần khi đăng nhập, không chạy hàng loạt |
| Đổi contract mà client chưa cập nhật | Mọi thay đổi hành vi có cờ cấu hình hoặc chỉ thêm trường tùy chọn; bật cờ sau khi client sẵn sàng |
