# Báo cáo triển khai: Community Block, Moderation Queue & AI Grading Report

## Status
- **Branch:** `feat/community-block-and-ai-report` (trên `feat/account-deletion`, chưa push)
- **Trạng thái:** Hoàn thành, toàn bộ test suite của `community-service` (255 tests) và `learning-service` (2029 tests) đã pass sau chỉnh sửa cuối cùng.

## Summary

### 1. Chặn người dùng hai chiều (`community-service`)
- **Migration:** `V15__add_user_blocks.sql` tạo bảng `user_blocks(blocker_id, blocked_id, created_at)` với PK đôi, `CHECK (blocker_id <> blocked_id)` và index chiều ngược `idx_user_blocks_blocked_blocker`.
- **Lọc hai chiều trong SQL (`NOT EXISTS`):** Đã áp dụng trên toàn bộ đường đọc (feed thường `ListPostsUseCase`, feed cursor, search feed, chi tiết bài `GetPostUseCase`, danh sách bình luận `ListCommentsUseCase` & `thread`, replies, `replyCounts`, `interactions`, `bookmarked`, `userPosts`, `followers`, `following`, `relationship`, `mention-suggestions`).
- **Tương tác & Thông báo:** Chặn tự động hủy follow hai chiều. Người có quan hệ chặn bị trả `403` tiếng Việt + `details.code = "USER_BLOCKED"` khi comment, reply, mention, follow hoặc react (không phát event sang `notification-service`). Tự chặn mình trả `400` tiếng Việt.
- **Xóa tài khoản (`user.deleted.v1`):** `JdbcDeletedUserStore.anonymizeAndDeleteCommunityData` xóa toàn bộ dòng `user_blocks` ở cả hai chiều.

### 2. Hàng kiểm duyệt (`community-service`)
- `POST /api/community/reports` đã hỗ trợ sẵn cả `POST` và `COMMENT`.
- `GET /api/community/admin/reports` đã có phân trang và sắp theo `createdAt,asc;id,asc`. Đã bổ sung tham số lọc `status` (`PENDING` mặc định, `DISMISSED`, `ACTIONED`, `ALL`) và trường `decidedAt` (từ `reviewed_at`) vào `ContentReportQueueItemResponse` và `ContentReportResponse` để đo SLA 24 giờ.
- Quy trình kiểm duyệt được ghi tại `docs/community-moderation-sop.md`.

### 3. Báo cáo kết quả chấm AI (`learning-service`)
- **Lựa chọn thiết kế:** Bài nộp Writing/Speaking chấm AI thuộc `learning-service` (`lesson_writing_submissions`, `speaking_submissions`). Tạo bảng riêng `ai_grading_reports` (`V62__ai_grading_reports.sql`) thay vì gộp vào `work_items` vì `work_items` đồng bộ từ flag/remediation/analytics, còn báo cáo chấm AI cần `reason` tự do và idempotency theo `(user_id, submission_id)`. Lưu ý: báo cáo chấm AI chưa áp dụng cho essay gate do `assessment-service` chấm.
- **Xóa tài khoản (`user.deleted.v1`):** Đã thêm `ai_grading_reports` vào `UserDeletionJobWorker.USER_TABLES`.
- **Gateway:** Đi qua route `learning-service` (`/api/learning/**`) với `ClientRateLimit` tier `default` (không cần route riêng).
- **Contract:** `docs/contracts/ai-grading-report-v1.md`.

## Danh sách Endpoint mới / cập nhật (cho FE & Mobile)

### A. Community User Block
1. **Chặn người dùng:** `POST /api/community/blocks/{userId}`
   - Role: `CUSTOMER`, `ADMIN`
   - Response: `204 No Content` (idempotent; hủy follow hai chiều)
   - Lỗi: `400` (`"Bạn không thể tự chặn chính mình"`), `404` (`"Không tìm thấy người dùng"`)
2. **Bỏ chặn người dùng:** `DELETE /api/community/blocks/{userId}`
   - Role: `CUSTOMER`, `ADMIN`
   - Response: `204 No Content` (idempotent)
   - Lỗi: `400` (`"Bạn không thể tự bỏ chặn chính mình"`)
3. **Danh sách người đã chặn:** `GET /api/community/blocks?page=0&size=20`
   - Role: `CUSTOMER`, `ADMIN`
   - Response (`200 OK`): `PageResponse<PublicProfile>` (sort `createdAt,desc;blockedId,desc`)
4. **Lỗi khi tương tác với người có quan hệ chặn** (comment, reply, mention, follow, reaction):
   - Status: `403 Forbidden`
   - Body: `{ "status": 403, "message": "...", "details": { "code": "USER_BLOCKED" }, ... }`

### B. Community Moderation Queue
- `GET /api/community/admin/reports?status=PENDING&page=0&size=20`
  - `status` (optional): `PENDING` (mặc định), `DISMISSED`, `ACTIONED`, `ALL`
  - Response item bổ sung `decidedAt` (`Instant | null`) cùng `createdAt` để đo SLA 24h.

### C. AI Grading Report (`learning-service`)
1. **Gửi báo cáo kết quả chấm AI:** `POST /api/learning/ai-grading-reports`
   - Role: `CUSTOMER`
   - Body: `{ "submissionId": "uuid", "submissionType": "WRITING" | "SPEAKING", "reason": "string (1..1000)" }`
   - Response: `201 Created` (tạo mới) hoặc `200 OK` (idempotent replay cùng `(userId, submissionId)`)
     `{ "id", "userId", "submissionId", "submissionType", "reason", "status": "OPEN", "createdAt" }`
   - Mã lỗi: `400 INVALID_REASON`, `404 SUBMISSION_NOT_FOUND`, `403 NOT_OWNER`
2. **Danh sách báo cáo cho Examiner:** `GET /api/learning/admin/ai-grading-reports?status=&submissionType=&page=0&size=20`
   - Role: `EXAMINER`
   - Filter: `status` (`OPEN`, `REVIEWED`, `DISMISSED`), `submissionType` (`WRITING`, `SPEAKING`)
   - Response (`200 OK`): `{ "content": [...], "page", "size", "totalElements", "totalPages" }`

## Concerns
- Báo cáo chấm AI hiện chỉ áp dụng cho bài nộp Writing/Speaking trong `learning-service`; chưa áp dụng cho essay gate do `assessment-service` chấm.

## Commits
- `19b2c830` — `feat(community): add two-way user blocking and SQL filtering`
- `ae5041b9` — `feat(community): add status filter, decidedAt SLA field, and moderation SOP`
- `5b0804a7` — `feat(learning): add AI grading report endpoints and contract`
