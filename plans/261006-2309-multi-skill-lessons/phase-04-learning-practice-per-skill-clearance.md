---
phase: 4
title: "Learning practice per-skill clearance"
status: completed
priority: P1
dependencies: [3]
---

# Phase 4: Learning practice per-skill clearance

## Overview
Practice có bộ riêng và bộ trộn. Mỗi lần nộp tính điểm theo từng skill; Practice của lesson PASSED khi mỗi skill đạt
(M5). Chỉ skill khách quan (R, L) tính vào Practice PASSED; bộ W tự chọn (M6). Essay ở phase 5.
<!-- Updated: Validation Session 1 - clearance chỉ tính R, L -->

## Requirements
- Functional:
  - `GET /api/learning/lessons/{id}/practice-sets?skill=X` (tham số mới, optional) chuyển tiếp filter sang content;
    response mỗi set có `skills`.
  - `PracticeAttempt`: thay `skill` bằng `skills` (từ set); migration `V9__practice_skills.sql`:
    - `practice_attempts.skills VARCHAR(20)[]`, backfill từ `skill`;
    - bảng pass theo skill `lesson_practice_skill_passes(user_id, lesson_id, skill, reason, passed_at,
      PRIMARY KEY(user_id, lesson_id, skill))`;
    - giữ `lesson_practice_passes` làm kết quả tổng (ghi khi mọi skill pass).
  - `ItemGrading` / kết quả nộp:
    - `PracticeSubmission` thêm `skillScores: [{skill, correct, total, percent, passed}]`;
    - skill của câu lấy từ `Section.skill` của package version;
    - `passed` tổng = mọi skill trong set đều ≥ 70% (một skill thì giống hiện tại).
  - Clearance:
    - `PracticeClearance.derive` tính **theo từng skill** của Practice của lesson (`skillsOfLessonPractice` = hợp
      `skills` của mọi set PUBLISHED của lesson, **bỏ WRITING**);
    - một skill PASSED khi: có attempt lần nộp đầu (counted) mà `skillScores[skill].passed`, hoặc review của KP skill
      đó đã xong hay SKIPPED, hoặc mọi set có skill đó đã lộ (giữ ba lý do cũ, áp theo skill);
    - Practice của lesson PASSED khi mọi skill PASSED; lesson không có set nào → `NO_PRACTICE` như cũ.
  - Thang ôn tập (`PracticeReviewRule`):
    - chạy trên item theo từng skill: một skill trượt (< 70%) thì xét KP sai của skill đó;
    - ngưỡng `< 40%` bắt ôn lý thuyết tính trên phần của skill đó;
    - KP Writing không sinh review (M11, chuẩn bị cho phase 5).
  - `PracticeProgress.persistPassed` ghi bảng theo skill và bảng tổng.
- Non-functional: `PracticeClearance` vẫn là domain service thuần (không Spring), nhận dữ liệu đã gom.

## Related Code Files
- Create: `services/learning-service/src/main/resources/db/migration/V9__practice_skills.sql`
- Modify (gốc `services/learning-service/src/main/java/com/group01/learning/`):
  - `domain/aggregate/PracticeAttempt.java`, `domain/vo/PracticeSubmission.java`;
  - `domain/service/{PracticeClearance,PracticeReviewRule}.java`;
  - `application/service/{ItemGrading,PracticeProgress,PracticeAttemptViewAssembler}.java`;
  - `application/usecase/{GetLessonPracticeSetsUseCase,StartPracticeAttemptUseCase,SubmitPracticeAttemptUseCase}.java`;
  - `application/port/LearningContentClient.java` (`lessonPracticeSets(lessonId, Optional<skill>)`, `skills` trên set);
  - `infrastructure/persistence/{JdbcPracticeAttemptRepository,JdbcLessonPracticePassRepository}.java`;
  - `api/controller/PracticeController.java`, `api/dto/response/PracticeSubmissionResponse.java`.
- Tests: `domain/service/{PracticeClearanceTest,PracticeReviewRuleTest}`, `domain/aggregate/PracticeAttemptTest`,
  `infrastructure/persistence/{PracticeIntegrationTest,RemediationLadderIntegrationTest}`, `api` WebMvc.

## Implementation Steps
1. **Test đỏ (domain)**:
   - Clearance:
     - lesson có set R, L, R+L: đạt R+L → PASSED;
     - lesson có set R, W: đạt R → PASSED (W không tính);
     - lesson chỉ có set W → `NO_PRACTICE`;
     - chỉ đạt R → REQUIRED;
     - đạt bộ R + bộ L → PASSED;
     - R đạt, L có review SKIPPED → PASSED;
     - một skill (dữ liệu cũ) → kết quả như test cũ.
   - Review rule: bộ R+L, R 90%, L 50% → chỉ KP Listening sai được xét; L 30% → stage THEORY.
2. Test đỏ grading: `skillScores` đúng; `passed` tổng.
3. Test đỏ integration: luồng bộ trộn → review → hoàn tất → Practice PASSED; `?skill=` qua API.
4. Migration V9, aggregate, repo.
5. Grading + rule + clearance + progress + use case + API.
6. `mvn -q -pl services/learning-service -am test`.

## Success Criteria
- [x] Mọi test cũ của thang ôn tập xanh không đổi kỳ vọng (một skill là trường hợp riêng của nhiều skill).
- [x] Practice của lesson nhiều skill chỉ PASSED khi mọi skill pass.

## Risk Assessment
- `PracticeClearance` và thang ôn tập là phần có nhiều test nhất của learning. Phải giữ đúng các lý do PASS cũ theo
  từng skill; dừng lại hỏi nếu một test cũ phải đổi kỳ vọng.
