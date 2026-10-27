---
phase: 3
title: "Học viên gửi bài kèm examiner, thấy examiner"
status: completed
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 3: learning-service

- `POST /api/learning/writing-submissions/{id}/human-review` nhận `{ "examinerId": "…" }` (bắt buộc) và chuyển cho Assessment. Lỗi từ Assessment
  (examiner đầy hoặc tắt nhận bài, không tồn tại) trả về học viên đúng mã, **không trừ lượt chấm** (hoàn lượt như cơ chế hiện có khi Assessment từ chối).
- Review trả cho học viên có `examinerId`; Learning không lưu examiner (Assessment là nguồn duy nhất).
- Học viên đổi examiner dùng API của Assessment (`/api/assessments/grading-jobs/{id}/examiner`), không qua Learning.

## Kiểm chứng

Cập nhật `HumanReviewControllerTest`, `HumanReviewIntegrationTest`, `HumanReviewClientJsonTest`.
