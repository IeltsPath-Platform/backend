---
phase: 5
title: "Learning practice essays"
status: pending
priority: P2
dependencies: [4]
---

# Phase 5: Learning practice essays

## Overview
Bộ Practice có essay: học viên nộp từng essay qua use case mới, LLM chấm, trừ điểm, tính hạn mức (M6). Bộ chỉ nộp
được khi mọi essay đã GRADED. Kết quả essay vào `skillScores[WRITING]` và mastery, không sinh review (M11). Bộ W **tự chọn**, không tính vào Practice
PASSED (M6).
<!-- Updated: Validation Session 1 - bộ W tự chọn -->

## Requirements
- Functional:
  - `POST /api/learning/practice-attempts/{id}/essays/{questionVersionId}/submissions` body `{requestId, essayText}`:
    - cùng mã lỗi và luồng trạng thái như bài luận lesson (`GRADING → PAYMENT_PENDING → GRADED`, `FAILED` thử lại;
      402/429/503/422);
    - câu phải là essay trong package version của attempt, không thì 409 `NOT_ESSAY_ITEM`;
    - attempt đã nộp → 409.
  - `GET /api/learning/practice-attempts/{id}`: mỗi item essay có `latestSubmission {id, status, overallBand, passed}`.
  - `WritingSubmission` thêm ngữ cảnh nguồn:
    - `source` (`LESSON_BLOCK` | `PRACTICE_ITEM`), `practice_attempt_id`, `question_version_id` (migration
      `V10__practice_essays.sql`, các cột nullable có CHECK theo `source`);
    - lesson cũ giữ `source = LESSON_BLOCK`.
  - Nộp attempt (`SubmitPracticeAttemptUseCase`):
    - set có essay mà còn essay chưa GRADED → 409 `ESSAY_NOT_GRADED`;
    - essay tính đúng khi `overallBand ≥ answer_spec.passBand` (M10) và vào `skillScores[WRITING]`;
    - attempt counted thì ghi evidence KP Writing (một bản ghi mỗi KP của câu, như essay lesson);
    - không tạo review cho KP Writing.
  - `ItemGrading` bỏ qua essay khi chấm khách quan; use case ghép kết quả essay vào.
  - Hạn mức dùng `LlmUsageQuota` chung của learning (M8). Giá điểm dùng `WritingSettings.pointCost` (câu hỏi mở 1).
- Non-functional:
  - không log essay hay prompt;
  - test dùng `LlmClient` giả;
  - `SubmitPracticeEssayUseCase` dùng lại phần dùng chung với `SubmitLessonEssayUseCase` bằng cách tách một service
    application `EssaySubmissionFlow` (charge, grade, retry), không chép code.

## Related Code Files
- Create:
  - `services/learning-service/src/main/resources/db/migration/V10__practice_essays.sql`;
  - `application/usecase/SubmitPracticeEssayUseCase.java`, `application/service/EssaySubmissionFlow.java` (tách từ
    `SubmitLessonEssayUseCase`), `api/dto/request/SubmitPracticeEssayRequest.java`.
- Modify: `domain/aggregate/WritingSubmission.java`, `domain/repository/WritingSubmissionRepository.java`,
  `infrastructure/persistence/JdbcWritingSubmissionRepository.java`, `application/usecase/{SubmitLessonEssayUseCase,SubmitPracticeAttemptUseCase,GetPracticeAttemptUseCase}.java`,
  `application/service/{ItemGrading,PracticeAttemptViewAssembler}.java`, `api/controller/PracticeController.java`.
- Tests: `domain/aggregate/WritingSubmissionTest`, `infrastructure/persistence/LessonWritingIntegrationTest` (không
  đổi hành vi), `PracticeEssayIntegrationTest` (mới), WebMvc.

## Implementation Steps
1. **Test đỏ**:
   - nộp essay → GRADED, trừ điểm một lần (replay `requestId` không trừ lại);
   - hết điểm → 402; hết hạn mức → 429; LLM lỗi → 503 rồi nộp lại được;
   - nộp bộ khi essay chưa GRADED → 409;
   - essay band ≥ `passBand` → `skillScores[WRITING].passed`;
   - không có review KP Writing;
   - bài luận lesson cũ chạy y nguyên.
2. Tách `EssaySubmissionFlow` từ `SubmitLessonEssayUseCase` (refactor, test cũ phải xanh trước khi đi tiếp).
3. Migration V10, aggregate, repo.
4. Use case mới + endpoint; sửa nộp attempt.
5. `mvn -q -pl services/learning-service -am test`.

## Success Criteria
- [ ] `LessonWritingIntegrationTest` xanh không đổi kỳ vọng sau refactor.
- [ ] Đạt hay trượt bộ W không đổi trạng thái Practice của lesson (nối với phase 4).

## Risk Assessment
- Refactor luồng bài luận lesson (có trừ điểm, idempotent) rủi ro cao: làm bước 2 riêng một commit, chạy test trước khi
  thêm tính năng.
- Học viên hết điểm chỉ không làm được bộ W (tự chọn), không bị chặn tiến độ.
