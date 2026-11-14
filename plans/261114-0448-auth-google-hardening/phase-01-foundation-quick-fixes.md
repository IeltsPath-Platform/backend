---
phase: 1
title: "Nền tảng: A6, G2, G4, G8"
status: pending
priority: P1
dependencies: []
---

# Phase 1: Các sửa nhỏ, rủi ro thấp

Bốn việc độc lập, không đổi API, không cần migration. Làm đầu tiên để có metric đo các phase sau.

## A6. Chống dò email qua thời gian phản hồi

**Vấn đề:** `LoginUseCase.execute` chỉ gọi `passwordEncoder.matches` khi user tồn tại, ACTIVE và có `passwordHash`. Email
không tồn tại, tài khoản bị khóa hay tài khoản chỉ có Google trả lời nhanh hơn rõ rệt so với tài khoản có mật khẩu.

**Thiết kế:** mọi nhánh thất bại đều chạy đúng một lần `matches` với một hash giả cùng cost.
- Trong `LoginUseCase` thêm trường `dummyHash`, tạo một lần khi khởi động bằng `passwordEncoder.encode(<chuỗi ngẫu nhiên>)`
  (cùng encoder nên cùng cost; sau phase 4 tự theo cost mới).
- Nhánh email sai, `status != ACTIVE`, `passwordHash == null`: gọi `passwordEncoder.matches(password, dummyHash)` rồi mới ném
  `AuthenticationFailedException("Email hoặc mật khẩu không đúng")`. Thông báo giữ nguyên.
- Không dùng `Thread.sleep`.

**File:** `services/user-service/.../application/usecase/LoginUseCase.java`; test `LoginUseCaseTest`.

**Test:** Mockito `verify(passwordEncoder).matches(...)` ở bốn nhánh (email không tồn tại, không ACTIVE, hash null, mật khẩu
sai). Đo tay: 50 lần đăng nhập cho mỗi loại trên stack local, hai nhóm lệch nhau dưới 20% trung vị.

## G2. Cookie `Secure` bắt buộc ở production

**Vấn đề:** `APP_AUTH_COOKIE_SECURE` mặc định `false`, Compose không đặt; không có profile production để dựa vào.

**Thiết kế:** thêm cờ `app.auth.cookie.enforce-secure` (biến `APP_AUTH_COOKIE_ENFORCE_SECURE`, mặc định `false` để dev
local chạy HTTP). Một bean khởi động ném `IllegalStateException` nếu `enforce-secure=true` mà `secure=false`.
- Môi trường production đặt cả hai biến `true` (thêm vào file env/compose production của plan `261111-1640` và
  `docs/deployment.md`).
- Không dùng tiền tố `__Host-` vì cookie refresh có `Path=/auth` (`__Host-` đòi `Path=/`). Không đổi tên cookie.

**File:** `config/AuthCookieProperties.java` (thêm `enforceSecure`), `config/AuthCookieSecurityGuard.java` (mới),
`infra/config-server/config-repo/user-service.yaml`, `docs/deployment.md`, `.env.example` (thêm tên biến, giá trị rỗng).

**Test:** unit test ba tổ hợp (`enforce=false/secure=false` chạy; `true/true` chạy; `true/false` ném lỗi, thông báo không
chứa giá trị cấu hình nhạy cảm).

## G4. Contract lệch với code

**Vấn đề:** `user-google-account-linked-v1.md` nói sự kiện chỉ phát khi liên kết xóa mật khẩu; `LinkGoogleAccountUseCase`
phát cho mọi lần liên kết Google vào tài khoản đã tồn tại (`identity == null && !createdNow`). Phát cho mọi lần liên kết là
hành vi an toàn hơn cho chủ tài khoản nên **sửa tài liệu theo code**.

**Việc:**
- `docs/contracts/user-google-account-linked-v1.md`: sự kiện phát khi Google mới được liên kết vào tài khoản đã có, dù mật
  khẩu có bị xóa hay không; không phát khi tạo tài khoản mới bằng Google.
- `docs/contracts/google-sign-in-v1.md`: đoạn cuối mục "Account matching and sessions" nói đúng như trên.
- Kiểm tra `LinkGoogleAccountUseCaseTest` đã có case "tài khoản đã xác minh, không có mật khẩu bị xóa, vẫn phát sự kiện";
  nếu chưa thì thêm.

## G8. Quan sát và cấu hình Google sign-in

**Thiết kế:**
- Port `application/port/GoogleSignInMetrics` (`record(Result)`), adapter Micrometer ở `infrastructure`. Counter
  `auth.google.signin` tag `result` ∈ `success, invalid_token, email_not_supported, not_allowed, conflict, inactive,
  unavailable`. Ghi trong `GoogleSignInUseCase` (một chỗ duy nhất bắt các exception đã có).
- Gauge `auth.google.configured` = 1 khi `GOOGLE_CLIENT_IDS` không rỗng, ngược lại 0; log WARN một lần lúc khởi động khi
  rỗng ("Google sign-in tắt: chưa đặt GOOGLE_CLIENT_IDS"), không log giá trị.
- `.env.example`: thêm `GOOGLE_CLIENT_IDS=` (rỗng) kèm nhóm "Optional: Google sign-in".
- `docs/local-development.md`: một dòng cho biết lấy client ID ở đâu và cách bật.

**File:** `application/port/GoogleSignInMetrics.java`, `infrastructure/metrics/MicrometerGoogleSignInMetrics.java`,
`config/GoogleAuthProperties.java` (kiểm tra cấu hình), `.env.example`, `docs/local-development.md`.

**Test:** `GoogleSignInUseCaseTest` kiểm counter đúng tag cho từng nhánh; test cấu hình đo gauge 0/1.

## Success Criteria
- [ ] Bốn nhánh thất bại của `LoginUseCase` đều gọi `matches`; đo tay không còn chênh lệch rõ giữa các loại tài khoản.
- [ ] Bật `enforce-secure` với `secure=false` làm service không khởi động; hai tổ hợp còn lại chạy bình thường.
- [ ] Hai contract Google khớp với code, có test chứng minh.
- [ ] Prometheus thấy `auth_google_signin_total{result=...}` sau một lần gọi `/auth/google` sai token.
- [ ] `mvn -q -pl services/user-service -am test` xanh; `ArchitectureTest` không có vi phạm mới.
