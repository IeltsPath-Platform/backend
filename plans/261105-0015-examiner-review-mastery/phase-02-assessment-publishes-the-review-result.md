---
phase: 2
title: "Assessment publishes the review result"
status: pending
priority: P2
dependencies: [1]
---

# Phase 2: Assessment publishes the review result

## Overview
Khi examiner hoàn tất review standalone Writing lần đầu, Assessment ghi outbox `assessment.human-review-completed.v1` trong cùng
transaction, cạnh `GradingCompletedAnnouncer`.

## Requirements
- Chỉ lần hoàn tất thật (khi `store.complete(...)` trả `Optional` có giá trị); lặp lại cùng bài chấm không ghi thêm.
- Chỉ skill `WRITING` của review standalone (Practice/Lesson/free writing). Bài thi (attempt) không gửi.
- Payload theo phase 1; `criteria` từ request (bỏ `comment`), `errorCodes` gộp như phase 1.
- Không log payload.

## Architecture
`HumanReviewService.complete` → `store.complete(...)` trả `Completed` (đã có `learnerId`, `reviewSubmissionId`, `skill`).
Cần thêm `sourceSubmissionId` vào `Completed` (đọc từ `human_review_submissions.source_submission_id` trong cùng transaction).
Không thêm `source`/`task` (Red Team #3).
Announcer mới `HumanReviewCompletedAnnouncer` (`Propagation.MANDATORY`, outbox + `OutboxEventsRecorded`), giống `GradingCompletedAnnouncer`.
Assessment khai báo `HumanReviewCompletedTopology` (giống cách đang khai báo `GradingCompletedTopology`).

## Related Code Files
- Create: `services/assessment-service/src/main/java/com/ieltspath/assessment/application/service/HumanReviewCompletedAnnouncer.java`
- Modify: `services/assessment-service/src/main/java/com/ieltspath/assessment/application/service/HumanReviewService.java` (gọi announcer trong `ifPresent`)
- Modify: `services/assessment-service/src/main/java/com/ieltspath/assessment/application/port/HumanReviewStore.java` (`Completed` thêm trường)
- Modify: `services/assessment-service/src/main/java/com/ieltspath/assessment/infrastructure/persistence/adapter/JdbcHumanReviewStore.java`
- Modify: cấu hình messaging của Assessment nơi khai báo `GradingCompletedTopology` (tìm bằng grep)
- Test: `HumanReviewFlowIntegrationTest` (mở rộng) hoặc test mới `HumanReviewCompletedAnnouncerTest`

## Implementation Steps
1. Baseline: `mvn -q -pl services/assessment-service -am test`; ghi test đỏ sẵn có.
2. Test đỏ:
   - hoàn tất review → đúng 1 outbox row `assessment.human-review-completed.v1`, payload đúng (criteria không có comment; errorCodes gộp + count; không có chuỗi essay/feedback/excerpt/suggestion trong JSON);
   - hoàn tất lặp lại y hệt → không thêm row;
   - review Speaking (nếu có đường) hoặc job của attempt → không có row;
   - rollback transaction (ví dụ annotation sai) → không có row.
3. Code: mở rộng `Completed`, announcer, gọi trong `complete`.
4. Chạy lại toàn module (kể cả `GradingCompleted*` để chắc không đổi event cũ).

## Success Criteria
- [ ] Các test trên xanh; test sẵn có của Assessment xanh.
- [ ] `grading-completed.v1` không đổi.

## Risk Assessment
- `HumanReviewService` đang được plan `261022-1150` sửa: chỉ thêm một lời gọi, rebase cẩn thận.
- Gộp mã lỗi: một mã chỉ trong `errorCodes` (không annotation) → `count` 1.
