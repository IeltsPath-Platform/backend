---
phase: 4
title: "Chính sách mật khẩu (A4) và băm mật khẩu (A5)"
status: pending
priority: P1
dependencies: []
---

# Phase 4: Mật khẩu: chính sách và băm

Độc lập với các phase khác; có thể làm song song.

## A4. Chính sách mật khẩu

**Hiện trạng:** `PasswordPolicy` chỉ kiểm tối thiểu 6 ký tự và tối đa 72 byte (giới hạn của BCrypt); không có danh sách
chặn, không kiểm trùng email/tên.

**Thiết kế** (theo hướng dẫn NIST SP 800-63B: ưu tiên độ dài và danh sách chặn, không bắt buộc ký tự đặc biệt):
- Tối thiểu **8** ký tự (cần duyệt, xem `plan.md`); tối đa 72 byte giữ nguyên. Cấu hình
  `app.auth.password.min-length` (mặc định 8).
- Từ chối khi: nằm trong danh sách mật khẩu phổ biến; trùng (không phân biệt hoa thường) với phần trước `@` của email, họ
  tên hoặc số điện thoại; toàn một ký tự lặp (`aaaaaaaa`) hoặc chuỗi tăng/giảm đơn giản (`12345678`, `abcdefgh`).
- Danh sách phổ biến: tài nguyên `password-blocklist.txt` (khoảng 10 000 mục, nguồn công khai giấy phép tự do, ghi nguồn
  và giấy phép trong tệp README cạnh tài nguyên), nạp một lần vào `Set` lúc khởi động, so khớp không phân biệt hoa thường.
  Không thêm dependency mới.
- Lỗi trả `400` mã `WEAK_PASSWORD`, `details.reasons` ∈ `TOO_SHORT, TOO_COMMON, CONTAINS_ACCOUNT_INFO, TOO_SIMPLE`.
  Thông báo không nhắc lại mật khẩu.
- Chỉ áp cho mật khẩu **mới** (đăng ký, đổi, đặt lại, phase 7 đặt lần đầu). Người dùng cũ không bị buộc đổi; không
  kiểm khi đăng nhập.
- **Tùy chọn, tắt mặc định, cần duyệt:** kiểm mật khẩu đã lộ qua API k-anonymity của Have I Been Pwned (chỉ gửi 5 ký tự
  đầu của SHA-1), qua `ResilientCall` (`target=hibp`, `CallKind.READ`), lỗi thì cho qua; cờ `app.auth.password.breach-check`.

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
`ResourcePasswordBlocklistTest` nạp được tài nguyên và tra đúng.

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
- Dummy hash của phase 1 tự theo cost mới vì dùng cùng encoder.
- Counter `auth.password.rehash`. Ghi lại thời gian BCrypt trên máy đích trong `docs/deployment.md`; chọn 12 nếu một lần
  băm dưới khoảng 350 ms, hạ 11 nếu VPS yếu (chỉ đổi biến môi trường).

**Files:** `config/AuthTokenConfig.java`, `application/usecase/LoginUseCase.java`, `domain/repository/UserRepository.java`,
`infrastructure/adapter/UserRepositoryAdapter` và JPA repository tương ứng, `config-repo/user-service.yaml`.

**Tests:** `LoginUseCaseTest` (hash cost 10 → được băm lại cost 12 và đăng nhập lại vẫn đúng; hash cost 12 không băm lại;
sai mật khẩu không băm lại); Testcontainers: hai request đăng nhập đồng thời không ghi đè một lần đổi mật khẩu xen giữa.

## Success Criteria
- [ ] Đăng ký/đổi/đặt lại với `12345678`, `password`, email của chính mình bị từ chối `WEAK_PASSWORD`; mật khẩu 12 ký tự
  ngẫu nhiên được chấp nhận.
- [ ] Sau một lần đăng nhập, hash của tài khoản demo chuyển từ `$2a$10$` sang `$2a$12$`, đăng nhập lại vẫn đúng.
- [ ] Không có cột hay bảng mới; không dependency mới.
- [ ] `PasswordPolicyTest`, `LoginUseCaseTest` và các test đăng ký/đặt lại xanh; `ArchitectureTest` xanh.
