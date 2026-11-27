# AI Grading Report v1

Báo cáo kết quả chấm AI cho bài nộp Writing (`lesson_writing_submissions`) và Speaking (`speaking_submissions`) thuộc `learning-service`.

## Lựa chọn thiết kế

- **Service sở hữu:** `learning-service` (vì toàn bộ bài luyện Writing/Speaking chấm AI cho học viên nằm tại `lesson_writing_submissions` và `speaking_submissions`).
- **Lưu trữ:** Bảng riêng `ai_grading_reports` (thay vì tái sử dụng `work_items`), vì `work_items` đồng bộ định kỳ từ flag/remediation/analytics và không lưu `reason` tự do hay khóa idempotent theo `(user_id, submission_id)`.
- **Phạm vi:** Áp dụng cho bài nộp Writing/Speaking chấm AI trong `learning-service`; chưa áp dụng cho essay gate do `assessment-service` chấm.
- **Gateway:** Đi qua route `learning-service` (`/api/learning/**`) với `ClientRateLimit` tier `default`.

## 1. Gửi báo cáo kết quả chấm AI

- **Method & Path:** `POST /api/learning/ai-grading-reports`
- **Role:** `CUSTOMER`
- **Request Body:**
  ```json
  {
    "submissionId": "uuid",
    "submissionType": "WRITING | SPEAKING",
    "reason": "string (1..1000 chars)"
  }
  ```
- **Response:**
  - `201 Created` khi tạo mới, `200 OK` khi gửi lại cùng `(userId, submissionId)` (idempotent).
  ```json
  {
    "id": "uuid",
    "userId": "uuid",
    "submissionId": "uuid",
    "submissionType": "WRITING",
    "reason": "...",
    "status": "OPEN",
    "createdAt": "2026-11-27T01:00:00Z"
  }
  ```
- **Mã lỗi:**
  - `400 INVALID_REASON` — lý do rỗng hoặc vượt quá 1000 ký tự
  - `404 SUBMISSION_NOT_FOUND` — không tìm thấy bài nộp
  - `403 NOT_OWNER` — bài nộp không thuộc về người dùng hiện tại

## 2. Danh sách báo cáo cho Examiner

- **Method & Path:** `GET /api/learning/admin/ai-grading-reports?status=&submissionType=&page=0&size=20`
- **Role:** `EXAMINER`
- **Query Params:**
  - `status` (optional): `OPEN`, `REVIEWED`, `DISMISSED`
  - `submissionType` (optional): `WRITING`, `SPEAKING`
  - `page` (default `0`), `size` (default `20`, `1..100`)
- **Response (`200 OK`):**
  ```json
  {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
  ```
