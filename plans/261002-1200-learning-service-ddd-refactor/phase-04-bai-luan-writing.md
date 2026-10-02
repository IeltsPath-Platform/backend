---
phase: 4
title: "Bài luận Writing"
status: completed
---

# Phase 4: Bài luận Writing

Thay `WritingSubmissionStore`.

## Việc

- Aggregate `WritingSubmission` (id, userId, lessonId, blockId, requestId, essayText, wordCount, prompt `EssayPrompt`,
  status `WritingSubmissionStatus`, pointCost, grade, overallBand, passed, failureCode, ledgerEntryId, gradingStartedAt):
  `start(...)` (GRADING), `isStale(now, seconds)`, `abandon()`, `fail(code)` (GRADING → FAILED), `restart(now)`
  (FAILED → GRADING), `recordGrade(grade)` (GRADING → PAYMENT_PENDING, tính `passed` theo `passBand`),
  `recordPaymentFailure(code)`, `markGraded(ledgerEntryId)` (PAYMENT_PENDING → GRADED). Chuyển sai trạng thái ném lỗi
  domain.
- Repository: `findByRequestId`, `findForUpdate`, `findOwned`, `findGrading(userId, blockId)`, `insert`,
  `save` → boolean, UPDATE có điều kiện `status = <trạng thái lúc đọc>` (compare-and-set; giữ an toàn khi không có khóa),
  `blockPassed`, `summarizeBlocks` (read model).
- Port `LlmUsageQuota.tryConsume(userId, day, kind, limit)` (giữ upsert có điều kiện hiện tại).
- Sửa `LessonEssayUseCase`, `LearnLessonUseCase` (tóm tắt khối essay). Xóa `WritingSubmissionStore`,
  `JdbcWritingSubmissionStore`.

## Kiểm

`LessonWritingIntegrationTest` pass (gồm resend, lỗi LLM, hạn mức, thiếu point, song song cùng khối).
