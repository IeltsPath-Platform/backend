---
title: "Sửa các lỗi backend do kiểm thử mobile phát hiện"
description: "Khai báo RabbitMQ còn thiếu, cho tài khoản demo kích hoạt key, lưu thiết bị đăng nhập, sửa mã lỗi và câu lỗi, giới hạn đăng nhập, lịch sử Speaking, tắt thiết bị push khi phiên bị thu hồi."
status: pending
priority: P0
branch: "fix/mobile-backend-requests"
tags: [infra, rabbitmq, user-service, access-service, notification-service, learning-service, api-gateway, error-handling]
created: "2026-11-27T22:30:00+07:00"
updated: "2026-11-27T22:30:00+07:00"
---

# Plan: sửa các lỗi backend do kiểm thử mobile phát hiện

Nguồn: tài liệu của bên mobile `backend-requests-2026-11-27.md` (cùng các bản 25/11, 26/11 trong `mobile/docs/`).
Đã đối chiếu với `main` tại commit `beecdf57` bằng cách đọc code; **chưa chạy toàn bộ hệ thống để tái hiện**.

## Các lỗi sẽ sửa

| Mức | Lỗi | Phase |
|---|---|---|
| P0 | content-service và game-service không có cấu hình RabbitMQ, stack local không lên | 1 |
| P0 | Production: script tạo env không cấp RabbitMQ cho content, game và library | 1 |
| P1 | Tài khoản demo không kích hoạt được key | 2 |
| P1 | Đăng nhập mới không lưu thiết bị và IP | 3 |
| P1 | Gọi sai method trả 500; thiếu hoặc sai kiểu tham số trả 500 | 4 |
| P2 | Câu lỗi còn tiếng Anh hoặc kèm UUID (game, learning, assessment) | 4 |
| P2 | `/auth/login` dùng chung giới hạn chặt với các API auth khác | 5 |
| P2 | Người chưa đặt tên hiển thị là "Learner" | 5 |
| P2 | Lịch sử Speaking không có tên đề | 5 |
| P1 | Thu hồi phiên không tắt thiết bị nhận thông báo | 6 |

## Phases

| # | Nội dung | Phụ thuộc |
|---|---|---|
| 1 | [RabbitMQ cho content, game, library](phase-01-rabbitmq-wiring.md) | |
| 2 | [Tài khoản demo kích hoạt được key](phase-02-demo-account-verification.md) | |
| 3 | [Lưu thiết bị và IP khi đăng nhập](phase-03-login-device-and-ip.md) | |
| 4 | [Mã lỗi và câu lỗi](phase-04-error-responses.md) | |
| 5 | [Giới hạn đăng nhập, tên mặc định, lịch sử Speaking](phase-05-login-limit-name-speaking-history.md) | |
| 6 | [Tắt thiết bị push khi phiên bị thu hồi](phase-06-disable-push-on-session-revoke.md) | |
| 7 | [Tài liệu và kiểm tra trên hệ thống chạy thật](phase-07-docs-and-verification.md) | 1–6 |

Một nhánh, mỗi phase một commit. Phase 1 chặn việc chạy `main`, nên làm trước và có thể mở PR riêng ngay nếu cần.

**Không deploy production trước khi phase 1 được merge.**

## Trùng file với plan khác

`plans/261127-2040-admin-system-only-roles` cũng sửa `api-gateway.yaml`, xoá `AdminLearningGoalController` (đang được
`StudyDataExceptionHandler` liệt kê) và thêm Admin đặt lại mật khẩu. Merge nhánh nào sau thì rebase nhánh đó; chức năng
đặt lại mật khẩu cũng phải phát sự kiện thu hồi phiên của phase 6.

## Tiêu chí nghiệm thu

- [ ] `docker compose up -d --build` trên máy sạch: mọi service `healthy`, không còn `Connection refused` tới RabbitMQ.
- [ ] Env production sinh ra có `RABBITMQ_*` cho mọi service dùng RabbitMQ; CI fail nếu thêm service dùng AMQP mà thiếu cấu hình.
- [ ] `learner@ielts.demo` và `h2@gmail.com` nhập được key demo.
- [ ] Phiên đăng nhập mới có `userAgent` và `ipAddress` của client.
- [ ] Sai method trả 405, thiếu hoặc sai kiểu tham số trả 400, câu lỗi tiếng Việt, ở mọi service.
- [ ] 12 lần đăng nhập liên tiếp từ cùng IP không bị gateway trả 429.
- [ ] Lịch sử Speaking trả `task` và `promptPreview`.
- [ ] Thu hồi phiên của máy A từ máy B, hoặc đổi mật khẩu: máy A không còn nhận push.
- [ ] CI qua.

## Không làm trong plan này

- Chức năng báo cáo kết quả chấm AI (bỏ theo yêu cầu ngày 27/11/2026).
- Thêm `targetBand` vào hồ sơ công khai: band mục tiêu nằm ở learning-service, user-service gọi sang sẽ tạo vòng phụ thuộc
  (learning đã gọi user), và đó là dữ liệu riêng của học viên. Mobile đã bỏ chỗ hiển thị.
- Band tổng cho bài Speaking luyện một phần: để trống là cố ý, band tổng chỉ có khi làm đủ 3 phần.
- Cấu hình gửi mail trên máy local (nằm trong `.env`, không phải lỗi code).

## Câu hỏi còn mở

- Có cần hiện `targetBand` công khai không? Nếu có thì làm riêng, đi qua community-service.
- Mobile thấy bài Speaking **đủ 3 phần** mà không có band không? Nếu có thì đó là lỗi khác, cần thêm mã bài nộp để điều tra.
