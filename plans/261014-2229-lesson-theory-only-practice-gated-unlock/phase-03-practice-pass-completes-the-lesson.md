---
phase: 3
title: Practice pass completes the lesson
status: completed
priority: P1
dependencies:
  - 2
---

# Phase 3: Practice pass completes the lesson

## Overview

`lesson_progress.completed_at` đổi nghĩa: với bài có Practice bắt buộc, nó **chỉ** được ghi khi Practice vừa PASSED.
Với bài không có Practice bắt buộc, nút "Hoàn thành bài" ghi như cũ. Nhờ vậy cổng hiện có ("mọi bài trước
COMPLETED"), danh sách bài, `completedLessonCount`, `TopicCompletion` và cổng thi chặng tự đúng theo luật mới mà không
phải viết thêm cổng thứ hai. Practice mở ngay khi bài mở.

## Requirements

**Tự hoàn thành bài khi Practice đạt**
- Thêm `LessonCompletion` (`application/service`), một chỗ duy nhất cho việc ghi hoàn thành:
  - `afterPracticeChange(userId, lessonId)`: gọi `PracticeProgress.refreshPassForLesson` (đổi kiểu trả về thành
    `Clearance`). Nếu `PASSED` và `reason != NO_PRACTICE` và bài chưa COMPLETED thì hoàn thành bài.
  - `complete(userId, lesson, progress)`: tạo `LessonProgress` nếu chưa có (`LessonProgress.start` từ
    `content.getLesson`, như `LessonAccess.refresh`), rồi `progress.complete(now)`, lưu, và
    `TopicCompletion.onLessonCompleted`. Idempotent.
  - Caller giữ learner lock (mọi caller hiện có đã lock).
- Đổi `practice.refreshPassForLesson(...)` thành `lessonCompletion.afterPracticeChange(...)` ở
  `SubmitPracticeAttemptUseCase` L132, `SubmitReviewUseCase` L119 và `GetReviewUseCase` L78 (nhánh skip hết package).
- Bài chỉ có Writing hoặc không có bộ nào (`NO_PRACTICE`) **không** tự hoàn thành khi nộp Writing; dùng nút.

**Nút "Hoàn thành bài" chỉ cho bài không có Practice bắt buộc**
- `CompleteLessonUseCase`: sau `access.authorize`, tính clearance (`open = true`). Nếu khác `PASSED` thì ném
  `409 PRACTICE_REQUIRED` với `lessonIds = [lessonId]`, dùng lại mã và cách dựng của `AssignTopicTestUseCase` L87.
  Nếu `PASSED` (gồm `NO_PRACTICE`, hoặc pass đã lưu mà bài chưa ghi hoàn thành) thì `lessonCompletion.complete(...)`.

**Practice mở cùng bài**
- `PracticeAccess.require`: bỏ kiểm "đã hoàn thành" (`PRACTICE_LOCKED`); thay bằng `LessonAccess.authorize(userId,
  lessonId)` (bài trước COMPLETED, chặng mở, bài ôn PENDING cùng skill → `REVIEW_REQUIRED`). Áp cho 3 caller ghi, cả 3
  đều giữ learner lock trong transaction ghi: `StartPracticeAttemptUseCase` L38, `SubmitPracticeAttemptUseCase` L74,
  `SubmitPracticeEssayUseCase` L107.
- `GetPracticeAttemptUseCase` (L35, `@Transactional(readOnly)`, không lock): **bỏ** `access.require`, chỉ kiểm quyền sở
  hữu (`attempts.findOwned` đã làm). Học viên luôn xem lại được attempt của mình. Không gọi `LessonAccess.authorize` ở
  đây, vì `authorize` có thể gọi `refreshTopics` (ghi DB) và yêu cầu lock.
  <!-- Updated: Validation Session 1 - đọc attempt chỉ kiểm sở hữu -->
- Bài ôn PENDING cùng skill vẫn chặn Practice của chính bài đó (luật hiện có): học viên làm bài ôn trước rồi mới thử bộ
  khác. <!-- Updated: Validation Session 1 - giữ REVIEW_REQUIRED cho Practice -->
- Xóa mã `PRACTICE_LOCKED` khỏi backend.
- Thêm `LessonAccess.isOpen(userId, lesson)` cho đường đọc. Hàm này **chỉ đọc**: dùng `status(userId, topicId)`
  (`curricula.find`), `content.getTopicLessons` và `lessons.findByTopic`. **Không** gọi `topic()`/`refreshTopics`, vì
  caller `GetLessonPracticeSetsUseCase` chạy `readOnly` và không lock. Trả `true` khi chặng `IN_PROGRESS`/`PASSED` và
  mọi bài đứng trước COMPLETED; không xét bài ôn (đường đọc tính `blocked` riêng như hiện có).
  <!-- Updated: Validation Session 1 - isOpen read-only, không refresh -->

**Đường đọc dùng "bài mở"**
- `GetLessonPracticeSetsUseCase`: `completed` → `open = access.isOpen(...)`; trạng thái từng bộ `LOCKED` khi
  `!open || blocked`. Field `completed` trong response giữ (FE còn dùng để biết bài đã xong).
- `GetTopicLessonsUseCase`: map gửi vào `practice.forTopic` là `openByLesson` (bài i mở khi mọi bài trước COMPLETED;
  chặng đã qua kiểm `TOPIC_LOCKED`). Trạng thái bài (`COMPLETED/AVAILABLE/LOCKED`) và `testStatus` giữ code hiện có.
- `AssignTopicTestUseCase` L79: truyền map mở cùng cách. Luật thi không đổi.

## Architecture

```text
SubmitPracticeAttempt / SubmitReview / GetReview(skip)
   └─ LessonCompletion.afterPracticeChange(user, lesson)
        clearance = PracticeProgress.refreshPassForLesson(user, lesson)   // persist pass row nếu PASSED
        if PASSED && reason != NO_PRACTICE && !completed:
             complete(): LessonProgress (tạo nếu thiếu) .complete(now) → save → TopicCompletion.onLessonCompleted

POST /lessons/{id}/complete (CompleteLessonUseCase)
   authorize → clearance(open=true)
   != PASSED → 409 PRACTICE_REQUIRED [lessonId]
   == PASSED → LessonCompletion.complete()

PracticeAccess.require(user, lesson) = LessonAccess.authorize(user, lesson).lesson()   // 3 caller ghi, có lock
GetPracticeAttempt                    = findOwned only                                  // read-only, không cổng
LessonAccess.isOpen(user, lesson)     = topic open && every earlier lesson COMPLETED     // read-only, no throw, no refresh
```

Phụ thuộc: `LessonCompletion → PracticeProgress, LessonProgressRepository, LearningContentClient, TopicCompletion`;
`PracticeAccess → LessonAccess`. Không ai trong số này phụ thuộc ngược lại `LessonCompletion` hay `PracticeAccess`,
nên không có vòng.

## Related Code Files

(`...` = `services/learning-service/src/main/java/com/ieltspath/learning`)

- Create: `.../application/service/LessonCompletion.java`
- Modify: `.../application/service/PracticeProgress.java` (`refreshPassForLesson` trả `Clearance`),
  `.../application/service/PracticeAccess.java`, `.../application/service/LessonAccess.java` (`isOpen`, Javadoc lớp),
  `.../application/usecase/CompleteLessonUseCase.java`, `SubmitPracticeAttemptUseCase.java`, `SubmitReviewUseCase.java`,
  `GetReviewUseCase.java`, `GetLessonPracticeSetsUseCase.java`, `GetTopicLessonsUseCase.java`,
  `AssignTopicTestUseCase.java`, `StartPracticeAttemptUseCase.java`, `SubmitPracticeEssayUseCase.java`,
  `GetPracticeAttemptUseCase.java`
- Test: `infrastructure/persistence/PracticeIntegrationTest.java` (L114 `PRACTICE_LOCKED` → kịch bản mới),
  `api/LessonLearningWebMvcTest.java` (complete → 409 `PRACTICE_REQUIRED` + `lessonIds`), các test integration khác
  đang gọi `complete` trước Practice (`ReviewAndTestAssignmentIntegrationTest`, `RemediationLadderIntegrationTest`,
  `MultiSkillLessonIntegrationTest`, `PracticeEssayIntegrationTest`, `CoursePathIntegrationTest`).

## Implementation Steps

1. **Đỏ trước** — integration (mở rộng `PracticeIntegrationTest`, fixture L1, L2 có bộ Reading):
   - L1 chưa làm gì: start Practice L1 → OK (trước: 409 `PRACTICE_LOCKED`); `GET /topics/{id}/lessons`: L1
     `AVAILABLE` + practice `REQUIRED`, L2 `LOCKED`; GET L2 → 403 `LESSON_LOCKED`.
   - `POST /lessons/L1/complete` → 409 `PRACTICE_REQUIRED`, `lessonIds=[L1]`.
   - Nộp bộ L1 đạt lần đầu → L1 `COMPLETED` (không gọi complete), L2 `AVAILABLE`, `completedLessonCount == 1`.
   - Nộp bộ L1 trượt → bài ôn PENDING; L1 chưa COMPLETED; nộp set ôn đạt → DONE → L1 COMPLETED → L2 mở.
   - Hết package khi mở bài ôn → SKIPPED → L1 COMPLETED.
   - Bài chỉ Writing: nộp Writing → bài **không** tự COMPLETED; `complete` → 200.
   - Chặng không có thi: Practice bài cuối đạt → chặng PASSED (`TopicCompletion`).
   - Bài ôn PENDING của L1 → start bộ khác của L1 → 403 `REVIEW_REQUIRED` (giữ luật).
   - `GET /practice-attempts/{id}` của chính học viên → 200 kể cả khi bài đó đang khóa; attempt của người khác → 404.
   - `GET /lessons/{id}/practice-sets` không lỗi transaction read-only (bộ `LOCKED` khi bài chưa mở).
   Chạy → đỏ.
2. `PracticeProgress.refreshPassForLesson` trả `Clearance`; tạo `LessonCompletion`.
3. Đổi các caller sang `LessonCompletion`; sửa `CompleteLessonUseCase`.
4. `LessonAccess.isOpen` (read-only); `PracticeAccess.require` qua `LessonAccess.authorize` cho 3 caller ghi;
   `GetPracticeAttemptUseCase` bỏ `access.require`; xóa `PRACTICE_LOCKED`.
5. `GetLessonPracticeSetsUseCase`, `GetTopicLessonsUseCase`, `AssignTopicTestUseCase` dùng map "mở".
6. Sửa các test integration đang gọi `complete` trước Practice: bài có Practice bắt buộc thì thay bằng nộp bộ đạt;
   bài `NO_PRACTICE` thì giữ.
7. Chạy `mvn -q -pl services/learning-service -am test`.

## Success Criteria

- [ ] Test mới đỏ trước, xanh sau.
- [ ] `grep -r PRACTICE_LOCKED services/learning-service/src` rỗng.
- [ ] Chỉ `LessonCompletion` gọi `LessonProgress.complete` (một chỗ ghi hoàn thành).
- [ ] Danh sách bài, cổng bài và cổng Practice cho cùng kết quả "mở/khóa" cho mọi bài.

## Risk Assessment

- **Bài ôn sinh từ kết quả thi** (`ReviewReevaluation`) gắn vào bài đã COMPLETED, không làm bài mất hoàn thành.
  Chúng vẫn chặn theo skill như cũ. Đúng ý định.
- **Practice bị chặn bởi bài ôn PENDING cùng skill** (luật hiện có, ở `PracticeAccess` L30): học viên trượt bộ đầu
  của L1 phải làm bài ôn trước khi thử bộ khác của L1. Giữ nguyên; FE dẫn vào bài ôn.
- **Content publish thêm bộ** sau khi bài đã COMPLETED: pass row đã lưu thắng, bài không bị khóa lại.
- **Transaction read-only**: `GetLessonPracticeSetsUseCase` và `GetPracticeAttemptUseCase` là `readOnly` và không lock.
  Không được đưa `LessonAccess.authorize`/`topic()` vào hai đường này (refresh curriculum sẽ ghi DB và lỗi).
  `isOpen` chỉ đọc; test integration phải gọi hai endpoint này với học viên chưa có curriculum để bắt lỗi ghi.
