# Notification push (FCM) v1

- Trạng thái: phía backend đã triển khai (2026-11-13). Chưa có app mobile và Firebase project; gửi thật chỉ chạy khi
  `NOTIFICATION_FCM_ENABLED=true`.
- Chủ sở hữu: notification-service. Route qua Gateway `/api/notifications/**` (đã có, cần JWT).
- Hướng: backend → Firebase Cloud Messaging → app. App chỉ đăng ký token, không bao giờ tự gửi push và không giữ credential server.

## 1. Đăng ký thiết bị

`PUT /api/notifications/devices/{installationId}` → `204`

```json
{ "platform": "ANDROID", "pushToken": "<FCM registration token>" }
```

| Trường | Luật |
| --- | --- |
| `installationId` | `[A-Za-z0-9_.:-]{8,200}`, do app tạo và giữ ổn định cho một lần cài (nên dùng Firebase Installation ID hoặc UUID lưu trong app). Sai → `400`. |
| `platform` | `ANDROID` hoặc `IOS`. Sai → `400`. |
| `pushToken` | Bắt buộc, tối đa 512 ký tự. |

- Idempotent: gọi lại với cùng `installationId` chỉ cập nhật token, platform và chủ sở hữu. App gọi mỗi lần mở app sau khi đăng
  nhập và trong callback `onTokenRefresh`/`messaging(_:didReceiveRegistrationToken:)`.
- Một installation thuộc người đăng nhập gần nhất: người khác đăng nhập trên cùng máy và gọi PUT thì máy chuyển sang người đó;
  push còn chờ của người cũ không được gửi tới máy.
- Một token chỉ thuộc một installation: token xuất hiện ở installation mới thì bản ghi cũ bị xóa.
- Mọi role đã đăng nhập đều dùng được (học viên và EXAMINER đều nhận thông báo).

## 2. Thu hồi thiết bị

`DELETE /api/notifications/devices/{installationId}` → `204`; không có hoặc thuộc người khác → `404`.
App gọi khi đăng xuất, trước khi xóa JWT.

Không có endpoint nào trả lại `pushToken`.

## 3. Khi nào có push

Mỗi thông báo được ghi vào hộp thư (`GET /api/notifications`) sẽ được xếp một lần gửi cho mỗi thiết bị người nhận đã đăng ký,
trong cùng transaction với thông báo. Thông báo bị tắt ở tùy chọn hiện có thì không được ghi nên cũng không có push. Thiết bị
đăng ký sau không nhận lại thông báo cũ.

## 4. Payload FCM

Message kiểu `notification` + `data` (FCM HTTP v1, gửi bằng Firebase Admin SDK):

- `notification.title`, `notification.body`: chữ tiếng Việt cố định theo `eventType`, không chứa tên người hay nội dung bài
  (an toàn trên màn hình khóa). Bảng chữ nằm ở `PushMessages` của notification-service; loại chưa biết dùng "Thông báo mới".
- `data` (mọi giá trị là chuỗi), cùng định danh như một item hộp thư:

| Khóa | Luôn có | Ghi chú |
| --- | --- | --- |
| `notificationId` | Có | Dùng cho `PUT /api/notifications/{id}/read` khi người dùng mở push. |
| `eventType` | Có | Ví dụ `assessment.grading-completed.v1`, `community.post-commented.v1`. |
| `postId`, `commentId` | Khi có | Thông báo community. |
| `gradingJobId` | Khi có | Thông báo chấm bài. |

## 5. Gửi và retry

- Job `PushDeliveryJob` (mỗi `NOTIFICATION_FCM_DISPATCH_DELAY_MS`, mặc định 5 giây) claim tối đa 50 bản ghi `PENDING` bằng
  `FOR UPDATE SKIP LOCKED` trong một câu lệnh tự commit, rồi gọi FCM ngoài transaction qua `ResilientCall` (`target=fcm`, `WRITE`).
- Thành công → `SENT` + `provider_message_id`. `UNREGISTERED`/`SENDER_ID_MISMATCH` → xóa thiết bị, `FAILED`.
  `INVALID_ARGUMENT`/`THIRD_PARTY_AUTH_ERROR` → `FAILED`. Lỗi khác → thử lại sau 30 s × 2^(lần−1), tối đa 5 lần, rồi `FAILED`.
- Retry chỉ cập nhật `notification_deliveries`, không bao giờ tạo thêm thông báo. Instance chết giữa chừng thì bản ghi được
  claim lại sau 2 phút (có thể gửi trùng một lần).
- Log chỉ có `deliveryId`, số lần thử và mã lỗi; không log token.
