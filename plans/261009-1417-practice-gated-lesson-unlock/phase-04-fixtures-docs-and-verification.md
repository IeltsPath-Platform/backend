---
phase: 4
title: "Fixtures docs and verification"
status: pending
priority: P1
dependencies: [3]
---

# Phase 4: Fixtures docs and verification

## Overview

Sửa các integration test cũ đi L1 → L2 không làm Practice, cập nhật tài liệu, chạy toàn bộ test và smoke qua Gateway.

## Requirements

- Không làm yếu test: fixture phải đi đúng luồng mới (làm/đạt Practice hoặc lesson không có Practice), không tắt gate.
- Tài liệu khớp code thật (mã lỗi, HTTP status, thứ tự kiểm, `maxFailedSets`).

## Related Code Files

- Test có thể cần sửa (đi nhiều lesson trong một topic): `infrastructure/persistence/{LessonSubmissionIntegrationTest,
  LessonWritingIntegrationTest, MultiSkillLessonIntegrationTest, PracticeIntegrationTest, PracticeEssayIntegrationTest,
  ReviewAndTestAssignmentIntegrationTest, CoursePathIntegrationTest, AssessmentResultIntegrationTest}.java`,
  `api/{LessonLearningWebMvcTest, WritingWebMvcTest}.java`. Chỉ sửa test thật sự đỏ.
- Docs:
  - `docs/contracts/lesson-learning-v1.md`: luật `status` của lesson; `REVIEW_REQUIRED` chặn lesson sau khi lesson trước
    trong topic còn bài ôn PENDING (bất kể skill); `PRACTICE_REQUIRED` 403 + `lessonIds` ở cổng lesson khi Practice chưa
    làm/chưa đạt (thêm dòng bảng lỗi, giữ 409 ở xin đề topic); bài ôn lặp đến khi đạt, chỉ lần trượt đầu bắt đọc theory,
    SKIPPED chỉ khi hết package; `maxFailedSets: null`.
<!-- Updated: Validation Session 1 - REVIEW_REQUIRED cho bài ôn lesson trước, theory chỉ lần trượt đầu -->
  - `AGENTS.md` §3.8: thay "Bài ôn vẫn chặn bài, Practice và thi topic..." bằng luật mới (lesson sau cần Practice lesson
    trước PASSED; bài ôn lặp đến khi đạt).
  - `docs/system-architecture.md`: luồng học và cổng lesson.
  - `services/learning-service/README.md`, `docs/fe-main-flow-guide.md`: bỏ "2 set trượt thì bỏ qua", thêm
    `PRACTICE_REQUIRED` ở cổng lesson.

## Implementation Steps

1. `mvn -q -pl services/learning-service -am test`; với mỗi test đỏ do luật mới, sửa fixture cho học viên làm Practice
   (hoặc đạt) trước khi sang lesson sau. Test đỏ vì lý do khác → dừng, điều tra.
2. Cập nhật docs theo danh sách trên.
3. Chạy lại `mvn -q -pl services/learning-service -am test` (Docker bật; xác nhận số test Testcontainers chạy > 0).
4. `mvn -q compile -DskipTests` cả reactor.
5. Smoke: `docker compose up -d --build learning-service`; qua Gateway với tài khoản demo: hoàn thành L1, mở L2 →
   403 `PRACTICE_REQUIRED`; làm Practice L1 trượt → mở L2 403 `REVIEW_REQUIRED`; xong bài ôn (hoặc Practice đạt) → mở
   L2 200.
6. `graphify update .`

## Success Criteria

- [ ] Toàn bộ test learning-service xanh, không skip Testcontainers.
- [ ] Contract, AGENTS.md, architecture, README, FE guide khớp code.
- [ ] Smoke qua Gateway đúng hai kết quả trên.

## Risk Assessment

- Dữ liệu demo có `lesson_practice_passes` theo luật cũ → smoke nên chạy trên tài khoản mới hoặc sau `docker compose down -v`.
- CI chạy `mvn verify` cả reactor; Testcontainers skip ở local khi thiếu Docker → luôn kiểm số test đã chạy.
