---
phase: 4
title: "Chính sách mật khẩu (A4) và băm mật khẩu (A5)"
status: done
priority: P1
dependencies: []
---

# Phase 4: Mật khẩu: chính sách và băm

Độc lập với các phase khác; có thể làm song song.

## A4. Chính sách mật khẩu

**Hiện trạng:** `PasswordPolicy` chỉ kiểm tối thiểu 6 ký tự và tối đa 72 byte (giới hạn của BCrypt); không có danh sách
chặn, không kiểm trùng email/tên.

**Thiết kế** (theo hướng dẫn NIST SP 800-63B: ưu tiên độ dài và danh sách chặn, không bắt buộc ký tự đặc biệt):
- Tối thiểu **8** ký tự (đã duyệt 2026-11-25); tối đa 72 byte giữ nguyên. Cấu hình
  `app.auth.password.min-length` (mặc định 8).
- Từ chối khi: nằm trong danh sách mật khẩu phổ biến; trùng (không phân biệt hoa thường) với phần trước `@` của email, họ
  tên hoặc số điện thoại; toàn một ký tự lặp (`aaaaaaaa`) hoặc chuỗi tăng/giảm đơn giản (`12345678`, `abcdefgh`).
- Danh sách phổ biến: tài nguyên `password-blocklist.txt` **tự soạn, vài trăm mục** (đã chốt 2026-11-25: không tải danh
  sách 10 000 mục, nên không có vấn đề nguồn/giấy phép). Gồm: các mật khẩu thông dụng nhất (`password`, `123456789`,
  `qwerty123`, `iloveyou`, `admin123`, `letmein`, `welcome1`...), biến thể Việt hóa thường gặp (`matkhau`, `matkhau123`,
  `vietnam123`, `anhyeuem`, `emyeuanh`, `20042004`...), tên sản phẩm (`ielts`, `ielts123`, `ielts2026`...). Mỗi dòng một mục,
  chữ thường, không dòng trống; dòng bắt đầu bằng `#` là chú thích. Nạp một lần vào `Set` lúc khởi động, so khớp không phân
  biệt hoa thường. Thêm mục mới = thêm dòng vào tệp. Không thêm dependency mới.
- Lỗi trả `400`, `code=WEAK_PASSWORD`, `details.reasons` ∈ `TOO_SHORT, TOO_COMMON, CONTAINS_ACCOUNT_INFO,
  TOO_SIMPLE`. Thông báo tiếng Việt, không nhắc lại mật khẩu (xem bảng trong `plan.md`).
- Chỉ áp cho mật khẩu **mới** (đăng ký, đổi, đặt lại, phase 7 đặt lần đầu). Người dùng cũ không bị buộc đổi; không
  kiểm khi đăng nhập.
- **Không** kiểm mật khẩu đã lộ qua dịch vụ ngoài (đã bỏ khỏi phạm vi; không có cờ, không gọi mạng).

**Điểm gọi:** `RegisterUseCase`, `CreateUserUseCase`, `ChangePasswordUseCase`, `ResetPasswordUseCase`, và use case đặt mật
khẩu lần đầu ở phase 7. Một `application/usecase/PasswordPolicyService.check(password, email, fullName, phone)` dùng chung;
`PasswordLengthValidator` (Bean Validation) chỉ kiểm độ dài theo cấu hình để giữ thông báo 400 cũ cho request thiếu hợp lệ.

**Files:** `domain/vo/PasswordPolicy.java` (hàm thuần, độ dài tối thiểu truyền vào), `application/usecase/
PasswordPolicyService.java`, `application/port/PasswordBlocklist.java`, `infrastructure/security/
ResourcePasswordBlocklist.java`, `resources/password-blocklist.txt`, `domain/exception/WeakPasswordException.java`,
`api/exception/GlobalExceptionHandler.java`, `api/validation/PasswordLengthValidator.java`, cấu hình trong
`config-repo/user-service.yaml`, `docs/contracts/user-management-v1.md` (mã `WEAK_PASSWORD`).

**Tests:** `PasswordPolicyServiceTest` (từng lý do; ranh giới 7/8 ký tự; 72 byte; chữ có dấu tính nhiều byte; không phân
biệt hoa thường); `PasswordPolicyTest` cũ cập nhật; `@WebMvcTest` cho đăng ký và đặt lại mật khẩu trả `WEAK_PASSWORD`;
`ResourcePasswordBlocklistTest` nạp được tài nguyên, bỏ qua dòng `#` và dòng trống, tra đúng không phân biệt hoa thường.
Đặt cả `ValidationMessages`/`PasswordLengthValidator` theo độ dài tối thiểu mới và thông báo tiếng Việt "từ 8 đến 72 ký tự".

## A5. Băm mật khẩu cost 12 và băm lại khi đăng nhập

**Hiện trạng:** `new BCryptPasswordEncoder()` (cost 10); dữ liệu cũ và tài khoản demo là `$2a$10$`.
Argon2id cần thư viện mới (Bouncy Castle) nên **không** chọn trong plan này.

**Thiết kế:**
- `AuthTokenConfig` tạo `new BCryptPasswordEncoder(cost)`, cost lấy từ `app.auth.password.bcrypt-cost` (mặc định 12, giới
  hạn 10–14 khi khởi động).
- `LoginUseCase` (đã `@Transactional`): sau khi `matches` đúng, nếu `passwordEncoder.upgradeEncoding(hash)` thì băm lại bằng
  encoder hiện tại và cập nhật **có điều kiện** `UPDATE users SET password_hash = :new WHERE id = :id AND password_hash =
  :old` (không ghi đè nếu mật khẩu vừa bị đổi song song). Thêm `UserRepository.updatePasswordHashIfUnchanged`.
- Hash cũ cost 10 vẫn xác thực được; mỗi người dùng được nâng cấp ở lần đăng nhập kế tiếp, không chạy hàng loạt.
- Hash giả của kiểm tra thời gian đăng nhập dùng cost hiện tại cùng cost 10 để cân bằng các tài khoản cũ chưa nâng cấp.
- Counter `auth.password.rehash`. Codex thêm mục "Chi phí BCrypt" vào `docs/deployment.md` (cách đo, ngưỡng, biến
  `APP_AUTH_PASSWORD_BCRYPT_COST`). **Chủ dự án** đo trên máy đích (không phải Codex): chọn 12 nếu một lần băm dưới khoảng
  350 ms, hạ 11 nếu VPS yếu (chỉ đổi biến môi trường).

**Files:** `config/AuthTokenConfig.java`, `application/usecase/LoginUseCase.java`, `domain/repository/UserRepository.java`,
`infrastructure/adapter/UserRepositoryAdapter` và JPA repository tương ứng, `config-repo/user-service.yaml`.

**Tests:** `LoginUseCaseTest` (hash cost 10 được nâng lên cost cấu hình; hash ở cost hiện tại không băm lại; sai mật khẩu
không băm lại); kiểm thử BCrypt thật xác minh tài khoản hash cost 10 đăng nhập được sau khi nâng và đăng nhập lại được;
Testcontainers: cập nhật có điều kiện không ghi đè một lần đổi mật khẩu xen giữa.

## Success Criteria
- [ ] Đăng ký/đổi/đặt lại với `12345678`, `password`, email của chính mình bị từ chối `WEAK_PASSWORD`; mật khẩu 12 ký tự
  ngẫu nhiên được chấp nhận.
- [ ] Sau một lần đăng nhập, hash của tài khoản demo chuyển từ `$2a$10$` sang `$2a$12$`, đăng nhập lại vẫn đúng.
- [ ] Không có cột hay bảng mới; không dependency mới; không gọi dịch vụ ngoài.
- [ ] `PasswordPolicyTest`, `LoginUseCaseTest` và các test đăng ký/đặt lại xanh; `ArchitectureTest` xanh.
