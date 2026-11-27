# Phase 2: tài khoản demo kích hoạt được key (P1)

## Nguyên nhân

- `ActivateKeyUseCase` chỉ cho kích hoạt khi `user_email_verification_state` (access_db) có dòng `verified = TRUE`.
- Bảng đó chỉ được ghi từ sự kiện của user-service. Tài khoản demo được chèn thẳng bằng SQL nên không có sự kiện nào.
- Ngoài ra `learner@ielts.demo` và `admin@ielts.demo` được chèn không có `email_verified_at`.

## File sửa

- `services/access-service/src/main/resources/db/callback/afterMigrate__demo_data.sql`: thêm cho ba tài khoản
  `…0001`, `…0002`, `…0003` (và `${demoUserId}`):
  ```sql
  INSERT INTO user_email_verification_state (user_id, verified, email, display_name, occurred_at)
  SELECT ... , TIMESTAMPTZ 'epoch'
  WHERE '${demoData}' = 'true'
  ON CONFLICT (user_id) DO UPDATE SET verified = TRUE;
  ```
  `occurred_at = epoch` để mọi sự kiện thật sau đó đều ghi đè được. Callback chạy mỗi lần khởi động nên database đã có
  sẵn cũng được cập nhật.
- `services/user-service/src/main/resources/db/callback/afterMigrate__demo_data.sql`: đặt `email_verified_at = now()`
  cho `learner@ielts.demo` và `admin@ielts.demo` khi đang `NULL` (đúng cả id lẫn email demo, chỉ khi `demoData`).

Không sửa `V10__user_email_verification_state.sql` (đã áp dụng, đổi sẽ lệch checksum). Chú thích trong file đó nói
"không có dòng thì được phép" là sai so với code; ghi điều đúng vào tài liệu ở phase 7.

## Tài khoản thật tạo trước khi có xác minh email

Cũng không có dòng nên không kích hoạt được. Nếu môi trường nào có tài khoản như vậy, chạy backfill sẵn có một lần
(`user.backfill.verification-state.enabled=true` ở user-service) rồi tắt lại. Không cần sửa code.

## Test

- access-service: chạy migration + callback với `demoData=true`, khẳng định `isVerified` đúng cho ba tài khoản; chạy lại
  lần hai không lỗi; `demoData=false` không tạo dòng nào.
- Chạy thật: đăng nhập `learner@ielts.demo`, nhập một key demo `IP-DEV5-KEYS-…`, ví điểm tăng.
