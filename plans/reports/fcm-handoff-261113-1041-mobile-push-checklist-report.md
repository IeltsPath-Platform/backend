# FCM push cho mobile: checklist bàn giao

Ngày: 2026-11-13. Backend đã xong phần nhận đăng ký thiết bị và gửi push; contract ở `docs/contracts/notification-push-v1.md`.
Việc còn lại cần quyền Firebase console hoặc repo mobile, nên chưa làm.

## A. Người giữ Firebase console

1. Tạo (hoặc chọn) Firebase project; ghi lại **Project ID**.
2. Thêm app Android (package name) và app iOS (bundle ID) trong Project settings.
3. iOS: tải APNs Authentication Key (.p8) từ Apple Developer, upload vào Project settings → Cloud Messaging.
4. Tạo service account chỉ có quyền gửi FCM (role `Firebase Cloud Messaging API Admin`), tải file JSON khóa.
   - Không commit, không dán vào chat, không đưa vào app mobile hay `.env`.
   - Đặt file ngoài repo, ví dụ `C:\secrets\ieltspath-fcm.json`.
5. Bật "Firebase Cloud Messaging API (V1)" trong Google Cloud console của project nếu chưa bật.

## B. Người vận hành backend

1. Trong `.env` gốc (không commit):
   - `NOTIFICATION_FCM_ENABLED=true`
   - `FIREBASE_PROJECT_ID=<project id>`
   - `GOOGLE_APPLICATION_CREDENTIALS=<đường dẫn tuyệt đối tới file JSON>`
2. Chạy trong Compose: mount file JSON vào container notification-service (read-only) và trỏ
   `GOOGLE_APPLICATION_CREDENTIALS` tới đường dẫn trong container. Chưa sửa `docker-compose.yml` lần này.
3. Restart notification-service. Flyway chạy migration V5 (`push_devices`, `notification_deliveries`).
4. Kiểm tra: đăng ký một máy thật, tạo một thông báo (ví dụ follow), xem `notification_deliveries.status = 'SENT'`.
5. Xoay khóa: tạo khóa mới → thay file → restart → xóa khóa cũ trong console.

## C. Đội mobile

1. Thêm `google-services.json` (Android) và `GoogleService-Info.plist` (iOS). Các file này chứa API key phía client,
   **không** cho quyền gửi push. Chỉ backend gửi.
2. Xin quyền thông báo (Android 13+ `POST_NOTIFICATIONS`, iOS `requestAuthorization`).
3. Sau đăng nhập và mỗi khi token đổi: `PUT /api/notifications/devices/{installationId}` với `{platform, pushToken}`.
   `installationId` phải ổn định cho một lần cài.
4. Khi đăng xuất: `DELETE /api/notifications/devices/{installationId}` trước khi xóa JWT.
5. Khi người dùng mở push: đọc `data.notificationId`, `eventType`, `postId`/`commentId`/`gradingJobId` để mở màn hình;
   gọi `PUT /api/notifications/{notificationId}/read`.

## Chưa làm (cố ý)

- `GET /api/notifications/devices` (tab Devices trong FDS): app chưa cần để nhận push.
- Tùy chọn bật/tắt theo kênh PUSH/EMAIL: tùy chọn hiện có theo loại sự kiện áp cho cả hộp thư và push.
- Dọn bản ghi delivery cũ và token không hoạt động lâu (đã ghi `shortcut:` trong `FcmPushConfig`).
- Email.

## Câu hỏi còn mở

- Firebase project dùng chung cho dev/prod hay tách riêng?
- Chữ push tiếng Việt cố định đã ổn chưa, hay cần theo ngôn ngữ người dùng?
