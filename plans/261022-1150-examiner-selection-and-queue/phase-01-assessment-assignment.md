---
phase: 1
title: "Chỉ định examiner, hàng đợi riêng, đổi examiner"
status: completed
priority: P1
effort: "1d"
dependencies: []
---

# Phase 1: assessment-service

## Dữ liệu (migration `V17__examiner_selection.sql`)

Bảng `grading_jobs` thêm:

| Cột | Ý nghĩa |
|---|---|
| `requested_examiner_id UUID` | Examiner học viên chọn. `NULL` = chờ chọn giáo viên |
| `requested_at TIMESTAMPTZ` | Lúc chỉ định, để tính hạn 48 giờ. Cùng `NULL` với cột trên |
| `examiner_changes INT` (0 đến 2) | Số lần học viên đã đổi examiner |

Backfill: việc `HUMAN` (không phải `QA_SAMPLE`) đã có `assigned_examiner_id` lấy người đó làm `requested_examiner_id`.
Chỉ mục `(requested_examiner_id, status, requested_at)` cho hàng đợi.

## Quy tắc

- **Hàng đợi examiner** `GET /api/assessments/grading-jobs`: EXAMINER chỉ thấy việc có `requested_examiner_id` là mình. ADMIN thấy tất cả, kể cả việc chờ chọn.
  Đường `QA_SAMPLE` không đổi. Thời gian chờ và `overdue` tính từ `requested_at` (hoặc `created_at` khi chưa chỉ định).
- **Nhận bài (`claim`)**: chỉ examiner được chọn. Bỏ quy tắc "24 giờ thì người khác nhận lại" cho việc đã chỉ định (còn giữ cho `QA_SAMPLE`).
  Nhận lại của chính mình là hợp lệ và không đặt lại trạng thái đang chấm.
- **Tạo việc có người chấm**: tạo review độc lập và tạo `grading-jobs` chế độ `HUMAN` bắt buộc `examinerId`; kiểm tra examiner tồn tại, đang nhận bài và
  còn chỗ. AI không nhận `examinerId`.
- **Học viên chọn hoặc đổi** `POST /api/assessments/grading-jobs/{id}/examiner` (CUSTOMER, chỉ việc của mình): lần đầu khi chờ chọn; đổi khi quá hạn
  và chưa `COMPLETED` và chưa quá 2 lần đổi. Áp dụng cho mọi việc `HUMAN` cùng một lượt thi. Xóa nháp và đặt lại việc chấm.
- **Bài của tôi** `GET /api/assessments/grading-jobs/mine` (CUSTOMER): việc `HUMAN` của mình với trạng thái, examiner, hạn, có đổi được không.
- **Chỗ còn lại của examiner**: đếm việc `HUMAN` `QUEUED` hoặc `PROCESSING` của examiner, dưới khóa theo examiner.
- Học viên đọc review có `examinerId` (cả khi còn chờ).

## Cổng ra ngoài

`ExaminerDirectory.find(examinerId)` trả `{id, acceptingSubmissions, dailyLimit}` hoặc rỗng; cài bằng `UserExaminerClient` gọi user-service (phase 2).

## Kiểm chứng

Test tích hợp trên PostgreSQL thật: examiner chỉ thấy và nhận bài của mình; người khác bị từ chối; chọn lần đầu, đổi khi quá hạn, đổi khi chưa quá hạn bị từ chối,
quá 2 lần bị từ chối; examiner đầy hoặc tắt nhận bài bị từ chối; hai yêu cầu cạnh tranh chỗ cuối chỉ một thắng; chốt rồi thì không đổi được.
Viết lại các test hàng chung cũ (`GradingQueueIntegrationTest`, `CourseGatePolicyIntegrationTest`).
