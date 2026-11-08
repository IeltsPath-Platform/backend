---
title: "Android FCM push notifications"
description: "Kế hoạch bổ sung đăng ký thiết bị Android và gửi FCM bền vững, giữ nguyên REST inbox và WebSocket."
status: in_progress
priority: P1
branch: "feat/fcm-token-mobile"
tags: [notification, android, fcm, reliability]
blockedBy: []
blocks: []
created: "2026-11-08T10:30:00.267Z"
createdBy: "ck:plan"
source: skill
---

# Android FCM push notifications

## Overview

Triển khai backend Android + Firebase Cloud Messaging trong `notification-service` trên nhánh `be`. Phần backend đã được hiện thực; regression verification còn một phần, còn Android/Firebase E2E và rollout.

Nguồn yêu cầu: `backend-fcm-push-requirements.md` do người dùng cung cấp, ngày 2026-11-07. Các quyết định bổ sung bên dưới là thiết kế đề xuất để đáp ứng tài liệu đó, không phải hành vi đã có.

Phạm vi: hai API đăng ký/gỡ thiết bị, migration, hàng đợi giao push trong PostgreSQL, Firebase sender, retry/parking, kiểm thử và bàn giao mobile. Không thêm iOS/APNs, Expo Push Service, Firebase Database, API `/send`, UI quản lý thiết bị hoặc sự kiện nghiệp vụ mới.

Luồng đích:

```text
Event RabbitMQ hiện có
  -> use case áp dụng chính sách hiện có
  -> transaction: notification + các push delivery cho thiết bị đang active
  -> commit -> ACK và WebSocket như hiện tại

Worker -> claim delivery trong transaction ngắn -> commit
       -> kiểm tra owner/version -> gọi FCM ngoài transaction
       -> lưu kết quả với claim ID; retry hoặc parking nếu cần
```

## Quyết định thiết kế

- Dùng PostgreSQL delivery table làm local outbox; không cần thêm exchange/event RabbitMQ cho FCM. Ghi notification và công việc gửi trong cùng transaction, không dựa vào callback sau commit hay executor trong bộ nhớ.
- Mỗi delivery gắn `(notification_id, device_id, registration_version)`. Token refresh, logout hoặc đổi chủ làm delivery cũ không còn hợp lệ; PUT lặp cùng token và owner không tăng version.
- `NoticePushBridge` vẫn chỉ giao WebSocket qua Redis. Không gửi FCM trong `onMessage`, tránh mỗi instance cùng gửi một push.
- Preferences cộng đồng giữ nguyên. Các thông báo grading/subscription giữ chính sách hiện tại; không gọi preference service chỉ hỗ trợ community cho chúng.
- Firebase Admin Java SDK, credentials qua ADC/workload identity hoặc secret được mount. Pin phiên bản tương thích lúc implementation; không ghi credential vào repo/image.
- Đề xuất hai cờ độc lập `notification.push.enqueue-enabled=false` và `notification.push.worker-enabled=false`. Phải triển khai ghi delivery trước khi bật enqueue; worker tắt thì giữ delivery, không đánh dấu SENT. Không backfill inbox lịch sử khi bật cờ.
- Gửi thành công nghĩa là **FCM đã chấp nhận**, không chứng minh thiết bị đã hiển thị. Timeout có thể gây nhận lặp; không cam kết exactly-once hoặc thu hồi push đã gửi khi logout.

Chi tiết API và schema: [phase 1](phase-01-api-and-device-storage.md); atomicity và flags: [phase 2](phase-02-durable-dispatch.md); worker: [phase 3](phase-03-fcm-delivery-worker.md).

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [API and device storage](./phase-01-api-and-device-storage.md) | Complete |
| 2 | [Durable dispatch](./phase-02-durable-dispatch.md) | Complete |
| 3 | [FCM delivery worker](./phase-03-fcm-delivery-worker.md) | Complete |
| 4 | [Regression verification](./phase-04-regression-verification.md) | In progress |
| 5 | [Rollout and mobile handoff](./phase-05-rollout-and-mobile-handoff.md) | Pending |

## Dependencies

Thứ tự: **1 → 2 → 3 → 4 → 5**. Test của từng phase phải được viết/chạy trong phase đó; phase 4 là kiểm chứng tích hợp và hồi quy.

Các plan reliability/common-events đã có cung cấp nền tảng; không mở rộng chúng hoặc thay đổi event đã phát. Plan Loki/Tempo và AI Speaking không chặn FCM; không thêm sự kiện Speaking chỉ để phục vụ plan này. Chỉ phối hợp nếu có người đồng thời sửa các listener Notification.

## Tiêu chí nghiệm thu

- [ ] PUT/DELETE xác thực, idempotent, không tác động thiết bị của user khác; chuyển tài khoản/token không gửi trùng hoặc gửi sai owner.
- [ ] Cả sáu nguồn notification hiện có được bao phủ; lỗi FCM không làm mất inbox, đổi unread hoặc chặn WebSocket.
- [ ] Notification và delivery commit/rollback cùng nhau; redelivery không tạo trùng.
- [ ] Worker nhiều instance, lease recovery, fencing, retry, expiry, invalid-token cleanup được kiểm chứng trên PostgreSQL thật trong test.
- [ ] Không lộ token/credentials trong response, logs, validation errors hoặc metrics; automated test không gọi Firebase thật.
- [ ] Android build thật nhận/tap đúng luồng; rollout/rollback có hướng dẫn và hai cờ mặc định tắt.

## Điều kiện môi trường và điểm cần chốt

Firebase project/package ID đúng môi trường, credentials có quyền tối thiểu, Android development build và Docker cho integration test là điều kiện kiểm chứng/deploy, không cần để viết code với fake sender.

Các giá trị token cap, batch, retry, TTL, retention trong phase là **đề xuất có cấu hình**, cần ghi lại giá trị chốt trong contract/runbook khi implementation. Chính sách mã hóa token ở DB, thu hồi khi xóa tài khoản và rủi ro logout offline phải được thống nhất trước production; xem phase 1 và 5.

## Bằng chứng và kiểm chứng plan

- [Khảo sát code và nguồn tham chiếu](reports/codebase-findings.md).
- [Checklist tự rà soát plan](reports/plan-review.md).
- [Báo cáo implementation và kiểm chứng](reports/verification.md).
- Chưa chạy migration trên môi trường dùng chung, chưa gửi push thật, chưa triển khai service; Android/Firebase E2E còn chờ app Android và Firebase credentials của môi trường.
