---
phase: 5
title: Docs data reset and verification
status: completed
priority: P2
dependencies:
  - 3
  - 4
---

# Phase 5: Docs data reset and verification

## Overview

Dọn dữ liệu cũ không còn đúng nghĩa, cập nhật README/contract, đóng plan cũ, chạy kiểm toàn bộ.

## Requirements

**Dữ liệu cũ** (learning-service, migration `V11__lesson_completion_requires_practice.sql`; V10 là bản mới nhất hiện có)
- Trước đây học viên bấm "Hoàn thành bài" mà Practice chưa đạt (như ảnh: L1 "Cần luyện thêm" nhưng COMPLETED). Theo
  nghĩa mới, các bài đó chưa xong. Nếu giữ nguyên thì L2 vẫn mở sai.
- `UPDATE lesson_progress lp SET completed_at = NULL WHERE completed_at IS NOT NULL AND NOT EXISTS (SELECT 1 FROM
  lesson_practice_passes p WHERE p.user_id = lp.user_id AND p.lesson_id = lp.lesson_id);`
  - Bài `NO_PRACTICE` đã hoàn thành đều có pass row, vì `CompleteLessonUseCase` gọi `refreshPassForLesson` ngay sau
    khi hoàn thành. Bài có Practice đã đạt cũng có pass row. Hai loại này không bị đụng.
  - Không đổi trạng thái chặng/khóa học đã PASSED (không hạ cấp tiến độ đã đạt).
  - Bảng `lesson_progress` tạo ở `V1__learning_schema.sql`, cột `completed_at`; `lesson_practice_passes` ở
    `V4__practice_attempts.sql`.
  - learning-service **chưa có** test migration riêng (mẫu `*MigrationTest` chỉ có ở content-service, ví dụ
    `LessonResourceMigrationTest`). Viết `LessonCompletionMigrationIntegrationTest` theo mẫu đó: Testcontainers
    Postgres (`@Testcontainers(disabledWithoutDocker = true)` như các test persistence của learning-service), Flyway
    thuần `target("10")`, chèn dữ liệu, rồi `migrate()` lên bản mới nhất và kiểm.
    <!-- Updated: Validation Session 1 - không có MigrationTest sẵn ở learning-service -->

**Tài liệu**
- `services/learning-service/README.md`: luồng bài/Practice/bài ôn mới; L162: bỏ `PRACTICE_LOCKED`, ghi
  `PRACTICE_REQUIRED` (409, `lessonIds`) cho cả `POST /lessons/{id}/complete`; bài ôn không giới hạn số lần trượt,
  `maxFailedSets: null`.
- Contract trong `docs/contracts` nếu có mục bài học/bài ôn (kiểm trước, repo backend hiện không có thư mục `docs/`).
- Plan `261009-1417-practice-gated-lesson-unlock/plan.md`: `status: cancelled`, thêm một dòng "Superseded by
  261014-2229-lesson-theory-only-practice-gated-unlock".
- Plan `261014-0050-lesson-resources-replace-blocks/plan.md`: thêm ghi chú rằng tab Luyện tập đã được tách ra theo plan
  này.
- SRS/FDS: người dùng tự cập nhật (không chạy script build tài liệu).

**Kiểm**
- Backend: `mvn -q -pl services/content-service,services/learning-service -am test`. `AnswerSpecGraderTest` thiếu file
  `docs/contracts/answer-spec-v1-vectors.json` từ trước, không liên quan; ghi rõ trong báo cáo.
- Frontend: `npm run typecheck`, `npm run lint`, `npm run build`, và `tests/learning-path.test.mjs` bằng lệnh đã xác
  định ở phase 4.
- Chạy thật (skill `run`): reset/migrate DB, đi luồng acceptance trong `plan.md` trên trình duyệt.

## Related Code Files

- Create: `services/learning-service/src/main/resources/db/migration/V11__lesson_completion_requires_practice.sql`,
  `services/learning-service/src/test/java/com/ieltspath/learning/infrastructure/persistence/LessonCompletionMigrationIntegrationTest.java`
- Modify: `services/learning-service/README.md`, `plans/261009-1417-practice-gated-lesson-unlock/plan.md`,
  `plans/261014-0050-lesson-resources-replace-blocks/plan.md`

## Implementation Steps

1. Viết test migration (đỏ): user có L1 COMPLETED không pass row → sau V11 `completed_at` NULL; L2 NO_PRACTICE có pass
   row → giữ; L3 Practice đã đạt → giữ.
2. Viết V11; chạy test.
3. Cập nhật README, đóng plan cũ, ghi chú plan 14/10.
4. Chạy toàn bộ kiểm backend + frontend; chạy thật.

## Success Criteria

- [ ] Test migration đỏ trước, xanh sau.
- [ ] Toàn bộ test backend xanh (trừ `AnswerSpecGraderTest` đã biết); frontend typecheck/lint/build/test xanh.
- [ ] README không còn `PRACTICE_LOCKED` và không còn nói bài ôn bị bỏ qua sau 2 lần trượt.
- [ ] Đã đi luồng acceptance trên app thật.

## Risk Assessment

- V11 làm học viên demo "mất" hoàn thành ở bài chưa đạt Practice. Đây là chủ đích (đúng nghĩa mới). Môi trường demo có
  thể reset bằng `docker compose down -v`.
- Bài ôn PENDING của bài vừa bị bỏ hoàn thành vẫn giữ; học viên làm tiếp như bình thường.
