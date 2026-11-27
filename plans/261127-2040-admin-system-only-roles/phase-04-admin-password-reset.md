# Phase 4: user-service, Admin đặt lại mật khẩu

## Bối cảnh

- Report 3 có UC-96 "Reset User Password": Admin đặt mật khẩu mới cho người dùng không đăng nhập được, mở từ menu của từng dòng ở trang người dùng.
- Server và web chưa có chức năng này. Test case ADM-08 (Report 5.3) đang không đạt.
- Gốc: `services/user-service/src/main/java/com/ieltspath/user`. Đã có `ChangePasswordUseCase`, `ResetPasswordUseCase`, `PasswordPolicyService`, `UserManagementController` (`/api/users`, ADMIN).

## API

`PUT /api/users/{id}/password`, quyền ADMIN, body `{ "newPassword": "..." }`, trả 204.

## Quy tắc

- Mật khẩu mới qua `PasswordPolicyService` và giới hạn byte như các luồng đổi mật khẩu khác.
- Không dùng cho chính mình: Admin đổi mật khẩu của mình bằng luồng Change Password (phải nhập mật khẩu hiện tại).
- Tài khoản không tồn tại hoặc đã xoá: từ chối.
- Tài khoản chưa có mật khẩu (chỉ đăng nhập Google): từ chối, vì người dùng vẫn vào được bằng Google.
- Trạng thái tài khoản (Active, Inactive, Locked) giữ nguyên.
- Không đánh dấu e-mail đã xác minh (khác với đặt lại bằng mã gửi qua e-mail).

## Khi thành công

Làm trong một transaction, có `userLock` như `ChangePasswordUseCase`:

1. Mã hoá và lưu mật khẩu mới.
2. Thu hồi mọi refresh token (lý do `PASSWORD_RESET`) và access token của người đó.
3. Xoá bộ đếm đổi mật khẩu sai và huỷ các mã đặt lại mật khẩu chưa dùng.
4. Ghi outbox `user.password-changed.v1`, để người dùng nhận e-mail báo mật khẩu đã đổi.
5. Ghi sự kiện kiểm toán `PASSWORD_RESET` với `method = ADMIN` và id của Admin. `AuthAuditSupport.SAFE_DETAIL_KEYS` cần thêm khoá cho id này, nếu không nó bị lọc mất.

Không ghi mật khẩu vào log, audit hay outbox.

## File

- Thêm `application/usecase/AdminResetPasswordUseCase.java`, command và request DTO.
- Sửa `api/controller/UserManagementController.java`, `application/usecase/AuthAuditSupport.java` (khoá chi tiết).
- Thông báo lỗi trả về client viết bằng tiếng Việt có dấu.

## Test

- Thành công: đăng nhập được bằng mật khẩu mới, mật khẩu cũ bị từ chối, token cũ bị thu hồi, đúng một outbox.
- Từ chối: mật khẩu yếu, chính mình, tài khoản chỉ có Google, tài khoản đã xoá, người gọi không phải ADMIN (403).

## Rủi ro

- Admin đặt được mật khẩu nghĩa là Admin có thể đăng nhập thay người dùng. E-mail báo đổi mật khẩu và bản ghi kiểm toán là hai thứ để phát hiện, nên không được bỏ.
- Admin được đặt lại mật khẩu cho Admin khác (đúng phạm vi quản lý người dùng). Nếu nhóm muốn chặn thì thêm một quy tắc ở use case.
