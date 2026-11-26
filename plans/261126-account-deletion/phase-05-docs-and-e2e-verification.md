# Phase 5: Cập nhật tài liệu & Kiểm chứng End-to-End

## 1. Tài liệu cần cập nhật
- `docs/contracts/user-deleted-v1.md` (từ Phase 1).
- `docs/system-architecture.md`:
  - Bảng **Bất đồng bộ (RabbitMQ)**: thêm luồng `UserDeleteMyAccountUseCase` → `outbox_events` (`user.deleted.v1`) → `user.events` → 8 consumer queues.
  - Bảng **Scheduler và nhiều instance**: thêm `UserDeletionJobWorker` (`FOR UPDATE SKIP LOCKED`) của các service dùng job xóa theo lô.
  - Mục **Rate limit Gateway**: ghi nhận `DELETE /api/users/me` dùng tier `auth`.

## 2. Thứ tự triển khai & Rollback
- **Thứ tự deploy:**
  1. `shared/common-events` + các service consumer (`learning`, `assessment`, `library`, `access`, `notification`, `game`, `community`, `content`) → queue được tạo và bind sẵn vào `user.events`.
  2. `user-service` (migration V20 + endpoint + outbox) + `api-gateway` / `config-repo` (route `DELETE /api/users/me`).
- **Rollback:**
  - Nếu lỗi ở Gateway/API: gỡ route `user-delete-account-limited` hoặc tắt endpoint ở `user-service`.
  - Nếu lỗi ở consumer: message tự động vào retry queue và DLQ (`*.user-deleted.v1.dlq`), không mất sự kiện; sửa bug consumer rồi replay DLQ.
  - Migration V20 chỉ thêm cột nullable `deleted_at` và mở rộng `CHECK` constraint nên tương thích ngược với code cũ.

## 3. Kế hoạch kiểm chứng E2E
1. Tạo tài khoản thử có: bài viết/bình luận (`community`), ghi âm Speaking (`speaking-recordings`), thẻ từ (`library` + `flashcards`), thiết bị FCM (`notification_devices`), avatar (`avatars`), điểm/gói (`access`), tiến độ học (`learning`), phiên chơi (`game`).
2. Gọi `DELETE /api/users/me` (thử sai mật khẩu → 401 tiếng Việt; đúng mật khẩu → 204; gọi lại với token cũ → 401 `SESSION_REVOKED`).
3. Xác minh:
   - Đăng nhập lại (email/mật khẩu hoặc Google) thất bại; `POST /auth/refresh` với refresh token cũ bị từ chối.
   - Không còn bản ghi/file gắn `userId` ở các service/bucket (trừ phần giữ theo luật ở `access` và `community` theo chính sách đã chốt).
4. Ghi nhận rõ ràng: `learning-service` có 9 integration test lỗi sẵn trên `main` liên quan `restAccessClient.premium()` (`FreePractice`, `LessonWriting`, `PracticeEssay`, `PracticeGatedLessonUnlock`, `SuggestionsAndOverview`), không thuộc phạm vi thay đổi.
5. **URL web cần có (repo frontend):** trang web cho phép người dùng đăng nhập và yêu cầu xóa tài khoản (gọi `DELETE /api/users/me`) để khai báo trên Google Play Console Data Safety.
