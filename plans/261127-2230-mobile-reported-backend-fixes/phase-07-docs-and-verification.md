# Phase 7: tài liệu và kiểm tra trên hệ thống chạy thật

## Tài liệu

- `docs/system-architecture.md`: content và game nhận `user.deleted.v1` qua RabbitMQ; sự kiện và queue mới của phase 6.
- `AGENTS.md` §3: luật "service thêm AMQP phải có config-repo, Compose và `BROKER` của `render-env.py`" (có
  `RabbitMqWiringTest` kiểm).
- `docs/deployment.md`: đã bổ sung RabbitMQ cho content, game, library; không cần sửa env tay.
- Ghi rõ: tài khoản không có dòng trong `user_email_verification_state` thì không kích hoạt key được.
- Report 4 (repo `report`): thêm cột `session_id` của `notification_devices`.

## Kiểm tra trên stack chạy thật

CI không bắt được lỗi cấu hình, nên phải chạy `docker compose up -d --build` và làm đủ:

| Bước | Kết quả mong đợi |
|---|---|
| `docker compose ps` | Mọi service `healthy` |
| Đăng nhập, mở danh sách phiên | Có thiết bị và IP |
| `learner@ielts.demo` nhập key demo | Thành công |
| `GET` vào API chỉ nhận `POST` ở access, content, game, library, user | 405 tiếng Việt |
| `GET /api/content/practice-tests/formats` không có `skill` | 400 tiếng Việt |
| 12 lần đăng nhập liên tiếp cùng IP, email khác nhau | Không có 429 |
| Lịch sử Speaking | Có `task`, `promptPreview` |
| Máy B thu hồi phiên máy A, gửi một thông báo | Máy A không nhận |
| Xoá một tài khoản test | Không có message nằm lại trong dead-letter |

Bước push cần thiết bị Android có FCM; nếu không có thì kiểm tra bằng dòng `active = false` trong `notification_devices`
và ghi rõ trong PR là chưa thử trên thiết bị.

Cuối cùng: `mvn -B -ntp verify` và CI của PR qua.
