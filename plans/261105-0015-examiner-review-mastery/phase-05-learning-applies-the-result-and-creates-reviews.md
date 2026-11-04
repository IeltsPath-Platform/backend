---
phase: 5
title: "Learning applies the result and creates reviews"
status: pending
priority: P1
dependencies: [1, 4]
---

# Phase 5: Learning applies the result and creates reviews

## Overview
Listener nhận `assessment.human-review-completed.v1`, use case áp kết quả: evidence + mastery, thay evidence AI, tạo bài ôn,
lưu `studyPoints`; `GET …/human-review` trả `studyPoints`.

## Requirements
- Listener theo mẫu `AssessmentCompletedListener`: ack thủ công sau commit; lỗi tạm → retry; vi phạm contract hoặc hết lượt → DLQ
  với header lý do; không log payload. Learning khai báo `HumanReviewCompletedTopology`.
- Use case `ApplyHumanReviewResultUseCase.execute(event)` trong một transaction, dưới `LearnerLock` của learner:
  1. `outcomes.find(reviewSubmissionId)` có → return (idempotent).
  2. Tải bài viết `sourceSubmissionId`, phải thuộc `learnerId` → không có/không khớp: vi phạm contract (DLQ).
  3. `task` và nguồn bài (`LESSON_BLOCK`/`PRACTICE_ITEM`/`FREE_WRITING`) lấy từ **bài viết**, không từ event (Red Team #3).
     Rubric `content.getActiveRubric(task)` → `Map<mã rubric, KP tiêu chí>` (`RubricCriterion.knowledgePointId`); `passBand` từ prompt bài viết;
     `trapKpByCode` từ `ErrorTrapCatalog` (local); `approvedCodes` từ `content.activeErrorTypes()` (chỉ ACTIVE + APPROVED).
     Content không truy cập được → lỗi tạm (retry), không ghi gì.
  4. `HumanReviewEvidenceRule` → outcomes.
  5. Lọc KP learner biết (`access.knownKnowledgePoints`, như `SubmitLessonEssayUseCase`).
  6. **Thay AI** cho KP tiêu chí: ref AI = `forWriting(submissionId, kp)` (LESSON_BLOCK) hoặc
     `forPracticeSet(practice_attempts.request_id, questionVersionId, kp)` (PRACTICE_ITEM); FREE_WRITING không có.
     `removeWritingEvidence` (lọc user + nguồn, phase 4) → tập KP bị ảnh hưởng.
     Lưu ý bài practice (Red Team #6): ref `forPracticeSet` của một KP là **một** row cho KP đó, nên khi KP câu hỏi trùng KP tiêu chí,
     xóa row này cũng bỏ kết quả câu hỏi của KP đó. Chấp nhận được vì ở luồng AI tiêu chí đã ưu tiên hơn kết quả câu
     (`SubmitPracticeAttemptUseCase.java:170-181`); KP câu hỏi khác KP tiêu chí không bị đụng.
  7. Append evidence: tiêu chí → `HUMAN_REVIEW`, ref `forHumanReview(reviewId, kp)`, `forQuestion(questionVersionId)` **không** dùng
     (tránh `withoutRepeats` bỏ evidence examiner vì evidence AI đến trước); mã lỗi → `ERROR_TRAP` sai, ref `forHumanReview(reviewId, trapKp)`.
  8. Mastery: `rebuild(userId, KP bị xóa)` rồi `recordEvidence(userId, evidence, withoutMilestone = KP tiêu chí ∪ KP con)` (như luồng AI/bẫy).
  9. Bài ôn, tổng ≤ `learning.human-review.review-limit` (mặc định 2):
     - mã lỗi theo thứ tự → `ErrorTrapReviews.createForMarkedCodes(userId, submissionLessonId, codes, limit)` (mới): KP cha của KP con,
       bỏ KP có review PENDING, lesson = `teachingSubmissionLesson` (lesson của bài nếu dạy KP cha, không thì lesson đã hoàn thành dạy KP cha),
       `searchPracticeSets(parent, excluded, 3, 1, lessonId, code)` có set ≥2 câu bẫy → `insertPending(..., false, "ERROR_PATTERN")`; trả số đã tạo.
       **Không** kiểm tier `PRIORITY` (khác `create` hiện có).
     - phần còn lại → `WritingCriterionReviews.create(userId, lessonId, failedCriteria, limit)` (overload nhận limit); chỉ khi bài có `lesson_id`.
     - Chỉ mã **đã duyệt** được xét (Red Team #1).
  10. `reviewItemId` (Red Team #2): `insertPending` trả `void` và `ON CONFLICT … DO NOTHING` (`JdbcReviewItemRepository.java:166-172`),
      nên sau các lần insert gọi **một lần** `reviews.findPending(userId)` và map KP cha/KP tiêu chí → id review PENDING.
      KP bị bỏ qua vì đã có review PENDING cũng được gắn id của review đó (learner vẫn có nút Ôn ngay).
  11. Lưu outcome với `studyPoints` (JSON ổn định, dưới).
- `studyPoints` (mảng, thứ tự: mã lỗi theo thứ tự trên, rồi tiêu chí trượt):
  ```json
  [{"kind":"ERROR_CODE","errorCode":"LINKER_MISUSE","count":2,"knowledgePointId":"<KP cha>","code":"DEMO_WRITING_LINKING",
    "name":"Từ nối và mạch lạc","reviewItemId":"…"},
   {"kind":"CRITERION","criterionCode":"CC","band":5.0,"knowledgePointId":"…","code":"DEMO_WRITING_CRITERION_CC",
    "name":"Mạch lạc và liên kết (CC)","practiceKnowledgePointId":"…"}]
  ```
  `reviewItemId` có khi KP có review PENDING sau bước 10 (mới tạo hoặc có sẵn); `practiceKnowledgePointId` = KP khi Content báo có bộ luyện (như `EssayStudyPoints`), không thì bỏ.
  Điểm `ERROR_CODE` chỉ gồm mã đã duyệt, nên không lộ mã chưa duyệt cho learner.
  Tên KP lấy một lần từ Content (cùng cách `EssayKnowledgeJudge`/study points đang làm); Content lỗi → bỏ `code`/`name`, vẫn lưu.
  Trường null bị bỏ khỏi JSON.
- `GET /api/learning/writing-submissions/{id}/human-review`: thêm `studyPoints` (từ outcome; `[]` khi chưa có). Các trường khác không đổi.
  `HumanReviewController.review` hiện trả thẳng `HumanReviewClient.Review` (`HumanReviewController.java:40`): thêm DTO response
  giữ nguyên mọi trường cũ + `studyPoints`, không sửa record của port. `createForMarkedCodes` đặt trong `ErrorTrapReviews` nên
  dùng được `teachingSubmissionLesson` (private, `ErrorTrapReviews.java:117`) mà không đổi phạm vi truy cập.
  <!-- Updated: Validation Session 1 - verified controller shape and helper visibility -->

## Architecture
```
RabbitMQ learning.human-review-completed.v1
  → HumanReviewCompletedListener (infra/messaging, parser kiểm contract)
  → ApplyHumanReviewResultUseCase (application/usecase)
      ├ HumanReviewEvidenceRule (domain)
      ├ KnowledgeEvidenceRepository / MasteryProfileUpdater
      ├ ErrorTrapReviews.createForMarkedCodes / WritingCriterionReviews.create(limit)
      └ HumanReviewOutcomeRepository
GET human-review → HumanReviewUseCase.review + outcome.studyPoints → controller DTO
```

## Related Code Files
- Create: `infrastructure/messaging/HumanReviewCompletedListener.java`, `infrastructure/messaging/HumanReviewCompletedParser.java`,
  messaging config khai báo topology (theo `AssessmentMessagingConfig`), `application/usecase/ApplyHumanReviewResultUseCase.java`,
  `application/command/HumanReviewResult.java`
- Modify: `application/service/ErrorTrapReviews.java` (thêm `createForMarkedCodes`, tái dùng `teachingSubmissionLesson`),
  `application/service/WritingCriterionReviews.java` (overload limit, trả số đã tạo), `application/usecase/HumanReviewUseCase.java`
  (đọc outcome), `api/controller/HumanReviewController.java` + DTO response, repository bài viết nếu cần tải `practice_attempts.request_id`
- Tests:
  - `ApplyHumanReviewResultUseCaseTest` (unit, stub Content/catalog)
  - `HumanReviewMasteryIntegrationTest` (Testcontainers, stub Content như `LearningContentClientTestSupport`)
  - `HumanReviewCompletedListenerTest` (ack/retry/DLQ, theo test listener sẵn có)
  - `HumanReviewControllerTest` mở rộng (`studyPoints`)

## Implementation Steps
1. Baseline module.
2. Test đỏ theo "Tiêu chí nghiệm thu" 1–7 của `plan.md`, thêm:
   - bài practice: evidence `practice_set` AI của KP tiêu chí bị xóa đúng ref, evidence KP câu hỏi giữ;
   - Content lỗi khi tải rubric → exception tạm, không ghi gì, outcome không có;
   - event cho bài của learner khác → DLQ, không ghi gì;
   - giới hạn 2: 3 mã có bộ luyện → 2 review `ERROR_PATTERN`, 0 `WRITING_CRITERION`;
   - KP con bẫy chưa đủ dữ liệu (INSUFFICIENT) vẫn tạo review (không chờ PRIORITY);
   - `ErrorTrapReviews.create` cũ vẫn kiểm PRIORITY (test sẵn có xanh);
   - mã chưa duyệt (có KP con) → không evidence, không review, không có trong `studyPoints`;
   - KP đã có review PENDING → không tạo review mới, `studyPoints` mang `reviewItemId` của review có sẵn;
   - event bị DLQ rồi đẩy lại (replay) sau khi Content hồi phục → áp đúng một lần (Red Team #5); log chỉ có `reviewSubmissionId`, không có payload.
3. Code theo Requirements.
4. Chạy lại toàn module; chạy `ErrorTrapReviewsTest`, `ReviewBasketIntegrationTest`, `LessonWritingIntegrationTest`, `PracticeEssayIntegrationTest`.

## Success Criteria
- [ ] Mọi test trên xanh; test sẵn có xanh (trừ lỗi nền).
- [ ] Không N+1: Content gọi tối đa một lần cho rubric, một lần cho tên KP, một lần tìm set mỗi KP cha (≤ limit).
- [ ] Log không chứa payload event.

## Risk Assessment
- Gọi Content trong transaction giữ learner lock: giữ ngắn; nạp rubric/tên/catalog **trước** khi mở transaction nếu được (như `SubmitLessonEssayUseCase` tính study points ngoài transaction).
- Rebuild profile sau xóa có thể đổi tier/milestone: dùng `rebuild` (không phát milestone), như luồng chấm lại bài thi.
- Plan `261101-2358` nâng `ERROR_PATTERN` PENDING thành chặn: chấp nhận (ghi ở plan.md).
- Event vào DLQ sau 5 lần thử (`AssessmentCompletedListener.java:38-40`) là mất cho tới khi đẩy lại tay (Red Team #5): phase 7 ghi
  cách replay; listener log `reviewSubmissionId` + lý do khi park để tìm được.
