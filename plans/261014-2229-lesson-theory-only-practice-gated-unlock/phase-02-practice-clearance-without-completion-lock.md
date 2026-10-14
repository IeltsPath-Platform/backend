---
phase: 2
title: Practice clearance without completion lock
status: completed
priority: P1
dependencies: []
---

# Phase 2: Practice clearance without completion lock

## Overview

Hai sửa trong luật `PracticeClearance`:
1. Practice không còn `LOCKED` vì bài chưa hoàn thành. `LOCKED` giờ chỉ có nghĩa là **bài chưa mở** (các bài trước chưa
   xong / chặng khóa).
2. Sửa lỗi `REVIEW_FINISHED`: hiện chỉ cần **một** bài ôn của skill không PENDING là PASSED, dù bài ôn khác cùng skill
   còn PENDING. Luật mới: có ít nhất một bài ôn của skill **và không còn** bài ôn PENDING nào của skill đó.

## Requirements

- `PracticeClearance.derive(boolean lessonCompleted, ...)` → `derive(boolean lessonOpen, ...)` (cả hai overload):
  - `stored != null` → PASSED (giữ, đứng đầu);
  - không có bộ bắt buộc → PASSED `NO_PRACTICE` (kể cả khi bài chưa mở, để FE biết bài không có ô Luyện tập);
  - `!lessonOpen` → LOCKED;
  - phần còn lại giữ nguyên thứ tự.
  <!-- Updated: codex-brief - NO_PRACTICE xét trước LOCKED -->
- `skillReason`: `REVIEW_FINISHED` khi `ofSkill` không rỗng và `noneMatch(PENDING)` (L66-67, trước là `anyMatch(!= PENDING)`).
- `wholeLesson` (bộ không ghi skill, Content cũ): cùng luật trên toàn bộ `reviews` (L80-81).
- `FIRST_SUBMISSION`, `ALL_SETS_ATTEMPTED`, `NO_PRACTICE` giữ nguyên.
- Javadoc lớp ghi rõ: LOCKED = bài chưa mở; PASSED của bài có Practice bắt buộc là điều kiện hoàn thành bài.
- `PracticeProgress`: đổi tên tham số/map `completed` → `open` ở `forTopic`, `forLesson`, `derive`.
  `refreshPassForLesson` truyền `open = true`. Nó chỉ chạy sau khi nộp Practice (đã qua cổng bài ở
  `PracticeAccess.require`), sau bài ôn (bài ôn chỉ tồn tại cho bài học viên đã mở) và sau `complete` (đã qua
  `authorize`).

## Architecture

```text
derive(lessonOpen, publishedSets, attempts, revealed, reviews, stored)
  stored != null                         -> PASSED(stored)
  no objective set                       -> PASSED(NO_PRACTICE)
  !lessonOpen                            -> LOCKED
  per objective skill:
    counted first attempt passed skill   -> FIRST_SUBMISSION
    ofSkill non-empty && none PENDING    -> REVIEW_FINISHED      // trước: any != PENDING
    all sets revealed && none PENDING    -> ALL_SETS_ATTEMPTED
    else                                 -> REQUIRED
```

Review DONE hoặc SKIPPED (hết package) đều là "xong".

## Related Code Files

(`...` = `services/learning-service/src/main/java/com/ieltspath/learning`)

- Modify: `.../domain/service/PracticeClearance.java`, `.../application/service/PracticeProgress.java`
- Caller đổi nghĩa tham số (chỉ đổi tên ở phase này, logic "open" ở phase 3):
  `.../application/usecase/GetTopicLessonsUseCase.java` (L45-48), `.../application/usecase/AssignTopicTestUseCase.java`
  (L79), `.../application/usecase/GetLessonPracticeSetsUseCase.java` (L50)
- Test: `domain/service/PracticeClearanceTest.java`

## Implementation Steps

1. **Đỏ trước** — `PracticeClearanceTest`:
   - bài mở, chưa làm gì → `REQUIRED` (trước: LOCKED vì chưa hoàn thành);
   - bài chưa mở → `LOCKED`;
   - Reading 2 review: một DONE, một PENDING → `REQUIRED`; cả hai DONE → `PASSED/REVIEW_FINISHED`; DONE + SKIPPED →
     `PASSED`;
   - `wholeLesson`: DONE + PENDING → `REQUIRED`;
   - review PENDING Listening không ảnh hưởng Reading;
   - `stored` thắng cả khi bài chưa mở.
   Chạy → đỏ.
2. Sửa `PracticeClearance`, `PracticeProgress` (đổi tên, `refreshPassForLesson` truyền `true`).
3. Sửa các test cũ kỳ vọng LOCKED vì chưa hoàn thành; ghi lý do trong commit.
4. `mvn ... -Dtest='PracticeClearanceTest,PracticeIntegrationTest'`. `PracticeIntegrationTest` L114 (`PRACTICE_LOCKED`)
   sẽ đỏ đến phase 3; ghi nhận, không sửa tạm.

## Success Criteria

- [ ] Test mới đỏ trước, xanh sau.
- [ ] Không còn nhánh LOCKED dựa trên hoàn thành bài trong `PracticeClearance`.

## Risk Assessment

- Dòng `lesson_practice_passes` đã lưu theo luật cũ (REVIEW_FINISHED lỏng) không bị ghi đè. Dữ liệu demo; phase 5 có
  migration dọn `completed_at` nhưng không xóa pass rows.
