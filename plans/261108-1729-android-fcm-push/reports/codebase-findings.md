# Khảo sát backend cho Android FCM — 2026-11-08

## Hiện trạng đã đọc

- `services/notification-service/src/main/java/com/ieltspath/notification/api/NotificationController.java`: REST inbox/unread/read/preferences, user lấy qua CurrentUserProvider.
- `application/RecordCommunityEventUseCase.java`: transaction, community preference, insertIfAbsent và Optional.empty cho duplicate/disabled.
- `application/RecordGradingCompletedUseCase.java`: transaction lưu grading notice, chính sách critical khác community.
- `infrastructure/CommunityEventConsumer.java`: ACK sau use case commit, trước WebSocket. Gọi Firebase đơn giản sau ACK có thể mất push nếu crash.
- `infrastructure/Grading*Listener.java`, `SubscriptionGrantedListener.java`: nhận kết quả Optional của Record* và giao NoticePushBridge; cần bao phủ toàn bộ nguồn.
- `infrastructure/NoticePushBridge.java`: WebSocket tại instance + Redis pub/sub tới instance khác; không phải FCM.
- `infrastructure/persistence/NotificationRepositoryAdapter.java`: JDBC, `ON CONFLICT DO NOTHING`; không cần thêm ORM khác cho bảng mới.
- `application/NotificationPreferenceService.java`: chỉ hỗ trợ CommunityEventTypes, không phải preference cho mọi notification.
- Migration hiện có V1–V4; V5/V6 trong plan chỉ là số dự kiến, phải recheck trước implementation.
- `services/notification-service/pom.xml`: chưa có Firebase Admin/common-resilience.
- `infra/config-server/config-repo/api-gateway.yaml`: route authenticated `/api/notifications/**` đã có; tier default hiện có là điểm kiểm tra, không mặc định thêm route.
- `shared/common-resilience/.../ResilientCall.java`: WRITE không tự retry; bulkhead chỉ áp dụng nếu target có cấu hình hoặc là llm; transient exception predicate không mặc nhiên hiểu Firebase SDK.
- `services/community-service/.../CommunityOutboxRelay.java`: mẫu short claim/lease và external publish ngoài transaction.

Không đọc secret runtime, không sửa cấu hình đang chạy, không migrate/test/send Firebase trong giai đoạn lập plan.

## Liên hệ plan hiện có

Đã rà plan.md dưới `backend/plans/` và tìm FCM/Firebase/push delivery: không thấy plan FCM trùng scope. Reliability/common-events là nền tảng đã có, observability còn partial không chặn; AI Speaking có câu hỏi về thông báo nhưng không tạo dependency bắt buộc hoặc quyền thêm event. Comment reactions đã completed và thông báo comment reaction mới vẫn ngoài scope FCM này.

## Nguồn yêu cầu

Tài liệu người dùng: `backend-fcm-push-requirements.md`, đề xuất ngày 2026-11-07, bản cung cấp tại Downloads. Plan diễn giải yêu cầu thành thiết kế cụ thể; tài liệu đính kèm không được coi là lệnh chạy code hoặc deploy.

## Tài liệu chính thức đã đối chiếu

- [Firebase Admin setup](https://firebase.google.com/docs/admin/setup): Java Admin SDK và Application Default Credentials; phần databaseURL trong ví dụ không phải lý do tạo Firebase Database cho FCM.
- [FCM error codes](https://firebase.google.com/docs/cloud-messaging/error-codes): phân biệt token unregistered, payload lỗi và lỗi tạm; retry có backoff/Retry-After.
- [Java MessagingErrorCode](https://firebase.google.com/docs/reference/admin/java/reference/com/google/firebase/messaging/MessagingErrorCode): enum lỗi provider cho mapper Java.

Phiên bản dependency và cấu hình timeout/retry của SDK cần xác minh lại lúc implementation. Các giá trị tuning trong plan là đề xuất, không phải số liệu đo tải hoặc cấu hình production đã có.
