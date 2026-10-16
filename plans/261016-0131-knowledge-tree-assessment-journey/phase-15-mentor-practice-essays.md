---
phase: 15
title: "Mentor chữa essay Practice và lesson"
status: pending
priority: P2
dependencies: [10, 12]
---

# Phase 15: Mentor chữa essay Practice và lesson

## Overview

Mentor mô tả gói có người hỗ trợ: Mentor "chữa bài tập tự luận" cho học viên đã trả tiền. Phase 12 mới nối tín dụng chấm
bởi người với essay của **bài thi** (topic test). Essay trong lesson và Practice (lưu ở learning, chấm LLM) chưa có đường
tới Mentor. Phase này cho học viên có tín dụng yêu cầu EXAMINER chữa một essay đã nộp.

## Requirements

- **Yêu cầu:** `POST /api/learning/writing-submissions/{id}/human-review` (`CUSTOMER`, của chính mình):
  - essay phải `GRADED` và chưa có yêu cầu chữa; học viên còn tín dụng chấm bởi người (`access-service`, tiêu 1 tín dụng
    theo `consume-human-grading`, idempotent theo `submissionId`, hết tín dụng → `402` hoặc `409` theo contract access);
  - learning gửi essay, đề và rubric sang assessment tạo một **submission độc lập** (không thuộc attempt) và một việc chấm
    `HUMAN` với `reason = HUMAN_CREDIT` (phase 10). Learning chỉ giữ `human_review_submission_id`.
- **Giám khảo:** việc xuất hiện ở hàng chờ phase 10 như việc khác (kèm loại nguồn "lesson/practice"); EXAMINER nhận việc,
  chấm theo tiêu chí và mã lỗi của danh mục, ghi nhận xét.
- **Học viên xem kết quả:** `GET /api/learning/writing-submissions/{id}/human-review` trả band, nhận xét theo tiêu chí, mã
  lỗi, tên hiển thị của giám khảo không có (chấm ẩn danh); trạng thái `PENDING` / `DONE`.
- **Không đổi cổng hay mastery:** bản chữa của Mentor chỉ là phản hồi; không ghi `kp_evidence`, không tạo bài ôn, không
  đổi trạng thái Practice (khác essay trong gate).
- **Đánh giá Mentor:** bản chữa này nằm trong nhóm mẫu lấy cho luồng R&D chấm lại ở phase 12.

## Việc phải tìm hiểu trước khi code

Kết quả của giám khảo cho submission độc lập hiện chưa rõ đường trả về: `GradingController` chấm theo attempt và kết quả.
Bước đầu của phase là đọc `LearnerSubmissionController`, `GradingJobController` và `SaveAssessmentResultDetailsUseCase`,
rồi chốt một trong hai hướng, ghi vào báo cáo phase:
1. mở rộng đường lưu kết quả cho submission độc lập (bảng kết quả riêng `submission_reviews`), hoặc
2. dùng lại cấu trúc kết quả hiện có nếu đã hỗ trợ.

Đây là thay đổi hợp đồng giữa learning và assessment, nên cần duyệt thiết kế trước khi code (AGENTS.md §5).

## Related Code Files

- learning: use case yêu cầu và xem, client sang assessment và access (theo port có sẵn), migration `V19` thêm
  `human_review_submission_id` và trạng thái
- assessment: tạo submission độc lập kèm job `HUMAN`, lưu và trả kết quả chữa; mở rộng hàng chờ phase 10 với trường nguồn
- access: không đổi (dùng API đã có)
- Test: `HumanReviewRequestIntegrationTest` (trừ tín dụng một lần dù gọi hai lần; hết tín dụng bị từ chối),
  `HumanReviewFlowIntegrationTest` (giám khảo chấm, học viên đọc được, không đổi mastery)

## Implementation Steps

1. Khảo sát luồng submission độc lập; chốt hướng lưu kết quả; xin duyệt hợp đồng.
2. **Đỏ trước:** hai test integration ở trên.
3. Assessment: lưu và trả kết quả; hàng chờ.
4. Learning: yêu cầu, xem; nối access.
5. Contract `docs/contracts/human-review-v1.md`.
6. `mvn -q -pl services/learning-service,services/assessment-service,services/access-service -am test`.

## Success Criteria

- [ ] Học viên có tín dụng yêu cầu chữa một essay lesson hoặc Practice và nhận được phản hồi của giám khảo.
- [ ] Tín dụng chỉ trừ một lần cho mỗi essay.
- [ ] Phản hồi của Mentor không đổi mastery, cổng hay bài ôn.

## Risk Assessment

- Essay và đề đi qua service khác; chỉ truyền dữ liệu cần thiết, không log nội dung.
- Nếu tín dụng đã trừ mà tạo việc lỗi, phải hoàn lại tín dụng (refund API đã có ở access); viết test cho nhánh lỗi.

## Bàn giao FE

Nút "Nhờ Mentor chữa" ở màn kết quả essay (hiện số tín dụng còn lại), màn xem phản hồi của Mentor.
