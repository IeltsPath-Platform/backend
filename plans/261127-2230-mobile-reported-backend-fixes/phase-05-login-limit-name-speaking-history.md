# Phase 5: giới hạn đăng nhập, tên mặc định, lịch sử Speaking (P2)

## Giới hạn đăng nhập

Cả lớp hoặc một nhà mạng 4G chung một IP; người thứ 6 đăng nhập trong vài giây bị 429. Chống dò mật khẩu theo email + IP
đã có ở user-service (`RedisLoginAttemptLimiter`).

- `infra/config-server/config-repo/api-gateway.yaml`: thêm tier `login: { replenish-rate: 1, burst-capacity: 20 }`;
  thêm route `user-login-limited` cho `/auth/login`, đặt trước `user-auth-limited`; bỏ `/auth/login` khỏi route cũ.
- `AuthRouteConsistencyTest`: cập nhật danh sách của `user-auth-limited`, thêm test cho route mới.

## Tên mặc định "Learner"

- user-service `domain/vo/PublicProfile.java` (2 chỗ) và `domain/aggregate/LearnerProfile.createDefault`: đổi "Learner"
  thành "Học viên", cùng chữ community-service đang dùng.
- Giữ nguyên luật riêng tư: người chưa chọn tên công khai không lộ tên thật.
- Không migrate dữ liệu cũ (đăng ký bắt buộc có họ tên nên gần như không có dòng lưu chữ "Learner").

## Lịch sử Speaking có tên đề

`JdbcSubmissionHistoryRepository`, cả nhánh `COURSE` lẫn `FREE` của Speaking:

- `task`: `FULL` khi `scope = 'FULL'`; làm một phần thì `PART_1`, `PART_2`, `PART_3`; nhiều phần nhưng không đủ thì `PARTS`.
- `stem` (trả ra `promptPreview`): `question_text` của câu đầu tiên (`part_no`, `question_no` nhỏ nhất) trong
  `speaking_recordings`; chuỗi rỗng trả `null` (bài cũ không lưu câu hỏi).
- Lấy bằng truy vấn con theo `submission_id` (là cột đầu của khoá chính), không gọi Content.
- `band` giữ nguyên.

Cập nhật `docs/contracts/submission-history-v1.md` (dòng `SPEAKING` thêm `task`, `promptPreview`) và
`JdbcSubmissionHistoryIntegrationTest`.

Web (repo frontend, PR riêng, không bắt buộc cho phase này): `speakingRow` trong
`src/features/practice/history/submissionHistoryView.ts` dùng `promptPreview` làm tiêu đề khi có. Mobile tự sửa màn lịch sử.
