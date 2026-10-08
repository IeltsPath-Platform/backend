# Phase 2: Assessment, chấm placement

## Context

- `AttemptType.fromPackageType` đã map `PLACEMENT_TEST -> PLACEMENT`.
- `overall_band` hiện chỉ có khi grader nhập (`GradingController`); gate attempt chấm tự động qua `AutoGradeAttemptService`, essay gate qua `EnqueueGateEssayGradingService` (`GATES = TOPIC_GATE, COURSE_GATE`) và `GateResultAssembler`.
- Học viên nộp essay/audio qua `POST /api/assessments/submissions` (`textPayload`, `audioReference`).
- Event `AssessmentCompleted.v2` giữ nguyên field; Learning dùng `item_results` để tính band.

## Requirements

- Attempt `PLACEMENT` được chấm tự động khi nộp: câu trắc nghiệm/điền chữ chấm ngay; Writing vào hàng LLM như gate essay; Speaking nhận `audioReference`, điểm cố định.
- Kết quả chỉ chốt (phát event) khi mọi item đã có điểm: Writing LLM xong hoặc về EXAMINER và được chấm.
- Event PLACEMENT mang `item_results` đủ (score, max_score) kèm skill của từng item để Learning tính theo kỹ năng.
- Không đổi ý nghĩa `overall_band`; PLACEMENT phát `overall_band = null` (Learning tự tính).

## Files

- Modify: `EnqueueGateEssayGradingService` (thêm PLACEMENT vào tập gate essay), `GateResultAssembler`, `JdbcGateEssayJobStore` (điều kiện `attempt_type`), `AutoGradeAttemptService`, `AssessmentCompletedEventFactory` (đưa skill vào item nếu chưa có; nếu thêm field thì chỉ thêm, không đổi/xóa, theo contract v2).
- Create: property `assessment.placement.speaking-score-percent` (điểm cố định, mặc định theo quyết định câu hỏi mở 2).
- Migration: `V6__placement_attempt_grading.sql` chỉ khi cần cột/constraint mới (đối chiếu `V5`).
- Tests: submit PLACEMENT trắc nghiệm, essay LLM pass, essay LLM lỗi chuyển EXAMINER, Speaking điểm cố định, event đủ item.

## Steps

1. Đọc `SubmitAssessmentAttemptUseCase`, `AutoGradeAttemptService`, `GateResultAssembler` để xác nhận chỗ chốt kết quả (chưa đọc kỹ).
2. Mở rộng tập gate sang PLACEMENT; Speaking item chấm điểm cố định.
3. Cập nhật event factory và contract `assessment-completed-v2.md` (chỉ mô tả field thêm, nếu có).
4. Test hẹp rồi `mvn -q -pl services/assessment-service -am test` (cần Docker).

## Risks

- Hết quota LLM hoặc thiếu cấu hình thì essay về EXAMINER: học viên bị cổng 403 chờ (câu hỏi mở 5).
- Thêm field event phải tương thích consumer cũ (Learning, DLQ); chỉ thêm optional.
- Speaking điểm cố định không phản ánh trình độ thật; ghi rõ trong docs là chỗ tạm của MVP.
