---
phase: 2
title: "Practice clearance waits for every review"
status: pending
priority: P1
dependencies: []
---

# Phase 2: Practice clearance waits for every review

## Overview

Sửa lỗi: Practice được tính PASSED (`REVIEW_FINISHED`) khi **bất kỳ** bài ôn của skill không PENDING, dù bài ôn khác
cùng skill còn PENDING. Luật mới: có ít nhất một bài ôn của skill và **không còn bài ôn PENDING** nào của skill đó.

## Requirements

- `PracticeClearance.skillReason`: `REVIEW_FINISHED` khi `ofSkill` không rỗng và `noneMatch(PENDING)`.
- `PracticeClearance.wholeLesson` (set không có skill, Content cũ): cùng luật trên toàn bộ `reviews`.
- `FIRST_SUBMISSION`, `ALL_SETS_ATTEMPTED`, `NO_PRACTICE`, `stored` (dòng `lesson_practice_passes`) giữ nguyên.
- Thứ tự ưu tiên: `FIRST_SUBMISSION` vẫn đứng trước (Practice đạt lần đầu không sinh bài ôn).

## Architecture

```text
skillReason(skill):
  first counted attempt passed skill       -> FIRST_SUBMISSION
  ofSkill non-empty && none PENDING         -> REVIEW_FINISHED      // trước: any != PENDING
  all sets revealed && none PENDING         -> ALL_SETS_ATTEMPTED
  else                                      -> null (REQUIRED)
```

Review DONE hoặc SKIPPED (hết package) đều là "xong" theo quyết định đã chốt.

## Related Code Files

- Modify: `services/learning-service/src/main/java/com/ieltspath/learning/domain/service/PracticeClearance.java`
  (L64-67, L80-81, Javadoc lớp)
- Test: `services/learning-service/src/test/java/com/ieltspath/learning/domain/service/PracticeClearanceTest.java`

## Implementation Steps

1. **Đỏ trước:** thêm test `PracticeClearanceTest`:
   - Reading có 2 review, một DONE một PENDING → `REQUIRED`.
   - Reading có 2 review đều DONE → `PASSED / REVIEW_FINISHED`.
   - Một DONE, một SKIPPED → `PASSED`.
   - Bản `wholeLesson` (set không skill): DONE + PENDING → `REQUIRED`.
   - Review PENDING của Listening không ảnh hưởng Reading (giữ hành vi theo skill).
2. Sửa `skillReason` và `wholeLesson`.
3. Chạy `PracticeClearanceTest` và `PracticeIntegrationTest`.

## Success Criteria

- [ ] Test mới đỏ trước, xanh sau; test cũ của `PracticeClearanceTest` vẫn xanh (sửa test nào kỳ vọng hành vi lỗi cũ thì
      ghi rõ lý do trong commit).

## Risk Assessment

- `lesson_practice_passes` đã lưu theo luật cũ không bị ghi đè (dòng `stored` thắng). Chấp nhận với dữ liệu demo; reset
  bằng `docker compose down -v` nếu cần.
