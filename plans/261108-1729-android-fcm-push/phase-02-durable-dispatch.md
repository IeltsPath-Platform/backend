---
phase: 2
title: "Durable dispatch"
status: complete
effort: ""
---

# Phase 2: Durable dispatch

## Overview

Dependencies: phase 1. Tạo delivery bền vững cùng transaction với notification cho cả sáu nguồn hiện có; giữ nguyên ACK, dedupe, WebSocket và preferences. Context: [plan](plan.md), `NotificationRepositoryAdapter`, `Record*UseCase`, `NoticePushBridge`.

## Requirements / architecture

- Khi enqueue bật và insert notification mới thành công: đọc snapshot các thiết bị Android/FCM active của recipient, tạo delivery riêng cho mỗi device/version trong **cùng transaction PostgreSQL**.
- Không có thiết bị thì không tạo delivery. Thiết bị đăng ký sau không nhận lại inbox cũ.
- Insert delivery lỗi thì rollback cả notification mới; RabbitMQ chưa ACK và retry theo cơ chế hiện có. Không swallow lỗi DB rồi đánh dấu notification đã xử lý.
- `insertIfAbsent` trả false do event/comment dedupe: không tạo thêm delivery. Không thay nghĩa khóa dedupe hiện tại.
- Sau commit, listener tiếp tục WebSocket và ACK theo luồng đã có. Không đưa FCM/network call vào transaction, Redis subscriber hoặc nhánh retry lưu inbox.

Migration tiếp theo dự kiến `V6__create_notification_push_deliveries.sql`, kiểm tra lại số lúc implementation.

| Field trong `notification_push_deliveries` | Mục đích |
| --- | --- |
| `id`, `notification_id`, `recipient_id` | UUID; tham chiếu notification cùng DB |
| `device_id`, `registration_version` | Binding đã snapshot; FK device không cascade xóa lịch sử âm thầm |
| `status` | PENDING, IN_FLIGHT, RETRY, SENT, PARKED, CANCELLED |
| `attempt_count`, `next_attempt_at` | Bounded retry |
| `claim_id`, `lease_until` | Lease + fencing khi nhiều instance |
| `expires_at`, `created_at`, `updated_at`, `sent_at` | TTL, vận hành và cleanup |
| `provider_message_id`, `last_error_code` | Optional ID provider, mã lỗi đã chuẩn hóa, không raw exception |
| `correlation_id`, `trace_context` | Theo common-observability, không nhét vào payload nghiệp vụ |

- UNIQUE `(notification_id, device_id, registration_version)`.
- CHECK status/attempt/version; index due jobs `(next_attempt_at, id)` lọc PENDING/RETRY và index lease IN_FLIGHT. Foreign key/index phục vụ recipient/device lookup, không thêm FK liên service.
- Job chỉ giữ IDs/version và metadata cần thiết; payload tạo từ notification đã lưu, không lưu bản sao token hoặc nội dung bài/bình luận.
- Record mới + snapshot phải được ghi trong transaction có isolation/locking phù hợp; worker kiểm tra lại ownership/version trước gửi để xử lý registration đua với enqueue.

## Chính sách nguồn

| Entry point | Use case phải nối durable dispatch | Policy giữ nguyên |
| --- | --- | --- |
| `CommunityEventConsumer` | `RecordCommunityEventUseCase` | Community preference; disabled không lưu/không delivery/không WS |
| `GradingRequestedListener` | `RecordGradingRequestedUseCase` | Chính sách grading hiện có |
| `GradingCompletedListener` | `RecordGradingCompletedUseCase` | Chính sách grading hiện có |
| `GradingDueSoonListener` | `RecordGradingDueSoonUseCase` | Chính sách grading hiện có |
| `GradingOverdueListener` | `RecordGradingOverdueUseCase` | Chính sách grading hiện có |
| `SubscriptionGrantedListener` | `RecordSubscriptionGrantedUseCase` | Chính sách subscription hiện có |

Không gọi `NotificationPreferenceService.isEnabled` cho event type ngoài CommunityEventTypes; service hiện sẽ từ chối các loại đó. Mở rộng preference riêng theo kênh là ngoài scope.

## Cờ tính năng

| enqueue | worker | Hành vi |
| --- | --- | --- |
| false | false | REST/WS như cũ; không tạo delivery mới; job cũ giữ lại |
| true | false | Notification mới có delivery; chưa gọi Firebase |
| true | true | Ghi delivery và worker xử lý |
| false | true | Chỉ xử lý backlog còn hạn, không enqueue mới; chỉ dùng drain có chủ đích |

Cả hai false mặc định. Việc chuyển cờ chỉ áp dụng cho lần xử lý tiếp theo, không quét inbox lịch sử. Job quá hạn CANCELLED với lý do EXPIRED khi worker chạy; dừng dài không tạo đợt gửi notification cũ sau bật lại.

## Related Code Files

Create `application/EnqueueNotificationPushUseCase.java` hoặc collaborator tương đương được dùng bởi tất cả Record*; `domain/repository/PushDeliveryRepository.java`, VO status/job; `infrastructure/persistence/PushDeliveryRepositoryAdapter.java`; migration V6.

Modify sáu `application/Record*UseCase.java` liệt kê ở trên và constructor tests tương ứng. Enqueue phải tham gia transaction hiện có qua bean/collaborator, không tạo transaction độc lập. `NoticePushBridge` không đổi trách nhiệm. Chỉ sửa listener nếu cần thống nhất gọi collaborator, không sửa broker topology/dedupe hiện tại như side effect.

## Implementation Steps

1. Viết persistence integration test rollback notification+delivery và duplicate event cho ít nhất community + grading; tham số hóa coverage đủ sáu nguồn.
2. Thêm schema/repository và cấu hình enqueue flag. Kiểm tra index theo query thực tế, không `findAll` rồi lọc trong bộ nhớ.
3. Thêm collaborator snapshot active devices, batch INSERT delivery, gọi sau insert mới và trước commit ở cả sáu use case.
4. Giữ lại dữ liệu correlation/trace theo technical context hiện có. Không copy event body hoặc token.
5. Kiểm tra source retry: crash trước commit rollback; commit xong nhưng trước ACK redelivery không thêm inbox/job; crash sau ACK trước gửi không mất job.
6. Cập nhật contract delivery guarantees; không tạo event `push.requested` nếu dùng local DB worker.

## Verification

PostgreSQL transaction tests: ép lỗi delivery insert; kiểm tra rollback đầy đủ; chạy lại event thành công. Hai device → hai delivery, một notification và một unread increment; người khác không có delivery. Community preference off → không có cả ba tác động. Firebase off/error chưa được liên hệ trong transaction.

## Success Criteria

- [ ] Sáu nguồn đi qua cùng cơ chế enqueue, đúng chính sách riêng.
- [ ] Atomic notification+jobs; không có cửa sổ mất push do callback trong bộ nhớ.
- [ ] Duplicate events/mentions không nhân đôi inbox, unread hoặc jobs.
- [ ] Flag matrix rõ ràng, không backfill và không coi disabled là SENT.

## Risk Assessment / rollback

Mỗi recipient có thể nhiều device: fan-out SQL batch, query có index và đo độ lớn transaction. Không đặt giới hạn device ngầm làm bỏ sót; nếu cần cap sản phẩm phải ghi rõ contract trước khi áp dụng. Tắt enqueue ngừng công việc mới, worker có thể pause hoặc drain có chủ đích; không sửa migration đã áp dụng.
