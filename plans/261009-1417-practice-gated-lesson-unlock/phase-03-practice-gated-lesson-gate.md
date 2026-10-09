---
phase: 3
title: "Practice-gated lesson gate"
status: pending
priority: P1
dependencies: [1, 2]
---

# Phase 3: Practice-gated lesson gate

## Overview

Cổng lesson thêm điều kiện "Practice của mọi lesson trước PASSED"; danh sách lesson của topic dùng cùng luật. Lesson
trước còn bài ôn PENDING → `403 REVIEW_REQUIRED` + danh sách bài ôn; Practice chưa làm/chưa đạt → `403 PRACTICE_REQUIRED`
+ `lessonIds`.

## Requirements

- Thứ tự kiểm trong `LessonAccessGate.authorize`: `REVIEW_REQUIRED` → `TOPIC_LOCKED` → `LESSON_LOCKED` (lesson trước
  chưa COMPLETED) → `PRACTICE_REQUIRED` (lesson trước COMPLETED nhưng Practice chưa PASSED).
- `REVIEW_REQUIRED` mở rộng: ngoài bài ôn PENDING cùng skill (luật cũ), bài ôn PENDING có `lessonId` thuộc **các lesson
  đứng trước** trong cùng topic cũng chặn, **bất kể skill**; `reviews` liệt kê các bài ôn đó để FE dẫn thẳng vào.
- `PRACTICE_REQUIRED` chỉ còn cho trường hợp Practice lesson trước chưa làm/chưa đạt mà không có bài ôn PENDING.
  `lessonIds` = các lesson đứng trước có Practice khác PASSED, theo thứ tự lesson.
<!-- Updated: Validation Session 1 - bài ôn PENDING của lesson trước → REVIEW_REQUIRED bất kể skill -->
- Áp cho mọi đường đi qua `LessonAccess.authorize` / `reauthorize`: `GetLessonUseCase`, `SubmitLessonExerciseUseCase`
  (cả `reauthorize`), `SubmitLessonEssayUseCase`, `CompleteLessonUseCase`.
- `GetTopicLessonsUseCase`: lesson chưa COMPLETED là `AVAILABLE` chỉ khi mọi lesson trước COMPLETED và Practice PASSED;
  ngược lại `LOCKED`. `testStatus` giữ nguyên.
- Lesson đầu tiên của topic không cần gọi tính Practice (bỏ lời gọi Content khi không có lesson trước).
- Khi gate đọc thấy Practice PASSED, ghi `lesson_practice_passes` (`PracticeProgress.persistPassed`, như
  `AssignTopicTestUseCase` L83) để lesson đã mở không bị khóa lại khi Content publish thêm set. Mọi caller đã giữ lock.

## Architecture

```text
LessonAccess.authorize(userId, lesson)
  summaries = ordered topic lessons; progress = lessons.findByTopic
  previous = lessons before current; previousComplete = all previous COMPLETED
  needPractice = []
  if (previousComplete && !previous.isEmpty())
      states = practice.forTopic(userId, topicId, completedMap).clearances()
      practice.persistPassed(userId, states)
      needPractice = previous.filter(id -> states.get(id).status() != PASSED)
  gate.authorize(pendingReviews, null, skills, previousLessonIds, topicStatus, previousComplete, needPractice)
  Context(lesson, progress, previousComplete, previousLessonIds, needPractice)

LessonAccessGate.authorize(pending, currentReviewId, skills, Set<UUID> previousLessonIds, topicStatus,
                           previousLessonsComplete, List<UUID> lessonsNeedingPractice)
  blocking = pending.filter(r -> r.skill()==null || skills.contains(r.skill()) || previousLessonIds.contains(r.lessonId()))
  ... existing checks (REVIEW_REQUIRED với blocking, TOPIC_LOCKED, LESSON_LOCKED) ...
  if (!lessonsNeedingPractice.isEmpty()) throw LearningGateException("PRACTICE_REQUIRED", List.of(), lessonsNeedingPractice)

LearningGateException: + List<UUID> lessonIds; message "Complete practice of the earlier lessons first."
GlobalExceptionHandler.gate: 403, truyền lessonIds khi code PRACTICE_REQUIRED (LearningErrorResponse đã có field)
```

`PracticeProgress` không phụ thuộc `LessonAccess` (chỉ dùng static `completed`) → inject vào `LessonAccess` không tạo vòng.

## Related Code Files

(`...` = `services/learning-service/src/main/java/com/ieltspath/learning`)

- Modify: `.../domain/service/LessonAccessGate.java`, `.../domain/exception/LearningGateException.java`,
  `.../application/service/LessonAccess.java` (+ `PracticeProgress`, `Context`, Javadoc lớp),
  `.../application/usecase/GetTopicLessonsUseCase.java`, `.../api/exception/GlobalExceptionHandler.java`
- Kiểm callers dùng overload cũ của `LessonAccessGate.authorize` (giữ overload hoặc cập nhật hết; không để overload chết).
- Test: `domain/service/LessonAccessGateTest.java`, `api/LessonLearningWebMvcTest.java` (map lỗi 403 + `lessonIds`),
  integration test mới hoặc mở rộng `PracticeIntegrationTest` cho luồng L1 → Practice → L2.

## Implementation Steps

1. **Đỏ trước:**
   - `LessonAccessGateTest`: previous complete + `lessonsNeedingPractice=[L1]` → `PRACTICE_REQUIRED` với `lessonIds`;
     bài ôn PENDING Listening của L1 khi mở L2 dạy Reading → `REVIEW_REQUIRED` liệt kê bài ôn đó; bài ôn PENDING của
     lesson ở topic khác, khác skill → không chặn; thứ tự ưu tiên (`REVIEW_REQUIRED`, `TOPIC_LOCKED`, `LESSON_LOCKED`
     thắng `PRACTICE_REQUIRED`); rỗng → qua.
   - `LessonLearningWebMvcTest`: use case ném gate `PRACTICE_REQUIRED` → 403, body có `code` và `lessonIds`.
   - Integration: L1 COMPLETED không Practice → GET L2 403 `PRACTICE_REQUIRED`; topic lessons: L2 LOCKED; Practice L1
     đạt → L2 AVAILABLE và mở được; Practice L1 trượt → review PENDING → mở L2: 403 `REVIEW_REQUIRED` (reviews chứa
     bài ôn đó); review DONE → L2 mở; hết package → review SKIPPED → L2 mở.
2. `LearningGateException` + handler.
3. `LessonAccessGate` (tham số mới), `LessonAccess` (tính `needPractice`, persist pass), `reauthorize` dùng
   `context.lessonsNeedingPractice()`.
4. `GetTopicLessonsUseCase`: thay `previousComplete &= completed` bằng `previousOpen &= completed && practicePassed`
   (đã có `practiceStates`), giữ biến riêng cho `testStatus` nếu khác nghĩa.
5. Chạy test hẹp: `LessonAccessGateTest`, `LessonLearningWebMvcTest`, `PracticeIntegrationTest`.

## Success Criteria

- [ ] Test mới đỏ trước, xanh sau.
- [ ] Cả 4 use case đi qua `LessonAccess` trả `REVIEW_REQUIRED` khi lesson trước còn bài ôn PENDING và
      `PRACTICE_REQUIRED` khi Practice lesson trước chưa PASSED.
- [ ] Danh sách lesson và cổng lesson cho cùng kết quả cho mọi lesson.
- [ ] Không query trong vòng lặp; lesson đầu topic không gọi `topicPracticeSets`.

## Risk Assessment

- Thêm một lời gọi Content (`topicPracticeSets`) mỗi lần mở lesson không phải lesson đầu. Chấp nhận (danh sách lesson đã
  làm tương tự); Content lỗi → `503 CONTENT_UNAVAILABLE` như các lời gọi khác.
- `persistPassed` trong đường đọc: ghi chỉ khi PASSED, `insertIfAbsent`, caller giữ lock → an toàn.
- Bài ôn PENDING của lesson trước khác skill: trước chỉ chặn lesson cùng skill, nay chặn lesson sau trong cùng topic
  qua `REVIEW_REQUIRED`. Bài ôn sinh từ kết quả thi gắn vào lesson trước trong cùng topic cũng chặn theo luật này (hiếm:
  thi topic diễn ra sau khi học hết lesson). Đúng ý định.
- `LessonAccessGate.blocking` (static) đang được `PracticeAccess` và `AssignTopicTestUseCase` dùng theo skill: giữ
  nguyên chữ ký đó, thêm biến thể có `previousLessonIds` cho cổng lesson.
