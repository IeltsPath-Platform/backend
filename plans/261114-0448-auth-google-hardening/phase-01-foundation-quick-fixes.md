---
phase: 1
title: "Nền tảng: A6, G2, G4, G8, dịch thông báo lỗi"
status: done
priority: P1
dependencies: []
---

# Phase 1: Các sửa nhỏ, rủi ro thấp

Năm việc độc lập, không đổi API, không cần migration. Làm đầu tiên (sau Bước 0 trong `plan.md`) để có metric đo các phase
sau. Các bước "đo tay" thuộc chủ dự án, không phải Codex (xem `plan.md`, mục "Việc chủ dự án tự làm").

## I18N. Dịch thông báo lỗi tiếng Anh còn sót (AGENTS §3.3)

**Vấn đề:** các exception sau trả chuỗi tiếng Anh ra client: `GoogleAccountConflictException`,
`GoogleAccountNotActiveException`, `GoogleEmailNotSupportedException`, `GoogleSignInNotAllowedException`,
`GoogleSignInUnavailableException`, `LoginLimiterUnavailableException`, `LoginRateLimitedException`, và chuỗi
"Google ID token is invalid" trong `NimbusGoogleIdTokenVerifier`.

**Việc:** dịch tại nguồn sang tiếng Việt có dấu, giữ nguyên HTTP status và `code` đang dùng; đọc `GlobalExceptionHandler`
và `VietnameseErrorMessages` (đã có trên `main`) để chọn câu và không trùng lặp với lưới dịch. Câu gợi ý (chỉnh cho khớp
ngữ cảnh khi viết):
- Xung đột tài khoản Google: "Tài khoản Google này đã được liên kết với một tài khoản khác."
- Tài khoản không hoạt động: "Tài khoản không còn hoạt động nên không thể đăng nhập bằng Google."
- Email Google không được hỗ trợ: "Địa chỉ email Google này chưa được hỗ trợ để đăng nhập."
- Vai trò không được phép đăng nhập Google: "Loại tài khoản này không được đăng nhập bằng Google."
- Dịch vụ đăng nhập Google tạm thời không dùng được: "Đăng nhập Google tạm thời không khả dụng. Vui lòng thử lại sau."
- ID token không hợp lệ: "Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn. Vui lòng thử lại."
- Quá nhiều lần đăng nhập sai: "Bạn đã thử đăng nhập sai quá nhiều lần. Vui lòng thử lại sau." (giữ thông tin thời gian chờ nếu code đang trả).
- Bộ giới hạn đăng nhập không dùng được: "Hệ thống tạm thời không thể xử lý đăng nhập. Vui lòng thử lại sau."

**Test:** mỗi exception có một test (`@WebMvcTest` hoặc test handler) chứng minh response chứa chuỗi tiếng Việt và đúng
`code`; cập nhật test/contract đang assert chuỗi tiếng Anh. Không đổi hành vi nào khác.

**Success:** không còn exception nào trong danh sách trả chuỗi tiếng Anh; test xanh.

## A6. Chống dò email qua thời gian phản hồi

**Vấn đề:** `LoginUseCase.execute` chỉ gọi `passwordEncoder.matches` khi user tồn tại, ACTIVE và có `passwordHash`. Email
không tồn tại, tài khoản bị khóa hay tài khoản chỉ có Google trả lời nhanh hơn rõ rệt so với tài khoản có mật khẩu.

**Thiết kế:** mọi nhánh thất bại đều làm lượng công việc BCrypt tương đương cho hash cost 10 cũ và cost hiện tại.
- Trong `LoginUseCase` tạo hash giả ngẫu nhiên một lần khi khởi động bằng encoder cost hiện tại và encoder legacy cost 10.
- Email sai, `status != ACTIVE`, `passwordHash == null`: so khớp với cả hai hash giả rồi mới ném
  `AuthenticationFailedException("Email hoặc mật khẩu không đúng")`.
- Mật khẩu sai trên tài khoản có hash: so khớp hash đã lưu và một hash giả bù theo cost; hash dưới cost hiện tại được bù
  bằng hash giả cost hiện tại, hash ở cost hiện tại được bù bằng hash giả cost 10. Việc này giữ lượng công việc tương đương
  giữa tài khoản cũ cost 10 và tài khoản mới cost hiện tại, trong khi vẫn cho phép nâng hash cũ khi đăng nhập thành công.
- Không dùng `Thread.sleep`.

**File:** `services/user-service/.../application/usecase/LoginUseCase.java`; test `LoginUseCaseTest`.

**Test (Codex):** Mockito xác minh các nhánh email không tồn tại, không ACTIVE, hash null và mật khẩu sai đều chạy so khớp
BCrypt với hash giả phù hợp.

**Đo tay (chủ dự án, không phải Codex):** 50 lần đăng nhập cho mỗi loại trên stack local, hai nhóm lệch nhau dưới 20% trung vị.

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
- [ ] Bốn nhánh thất bại của `LoginUseCase` đều chạy công việc BCrypt bù cho sai khác cost giữa hash cũ và hiện tại.
- [ ] Bật `enforce-secure` với `secure=false` làm service không khởi động; hai tổ hợp còn lại chạy bình thường.
- [ ] Hai contract Google khớp với code, có test chứng minh.
- [ ] Counter `auth.google.signin` tăng đúng tag cho từng nhánh (test đơn vị).
- [ ] Mọi exception trong danh sách I18N trả thông báo tiếng Việt có dấu, có test.
- [ ] `mvn -q -pl services/user-service -am test` xanh; `ArchitectureTest` không có vi phạm mới.
- [ ] Chủ dự án: đo tay thời gian đăng nhập và kiểm `auth_google_signin_total` trên Prometheus (ngoài phạm vi Codex).
