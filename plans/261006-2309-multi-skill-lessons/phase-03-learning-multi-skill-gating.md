---
phase: 3
title: "Learning multi-skill gating"
status: pending
priority: P1
dependencies: [1]
---

# Phase 3: Learning multi-skill gating

## Overview
Learning đọc `skills` thay cho một skill. Review chờ khóa lesson có chứa skill của review (M4); skill của review lấy
từ KP (M12). Lesson nhiều skill hoàn thành khi mọi block khách quan (R, L) đạt; essay không chặn (M2).
<!-- Updated: Validation Session 1 - essay không chặn hoàn thành lesson -->

## Requirements
- Functional:
  - `LearningContentClient.Topic`, `Lesson`, `LessonSummary` thêm `Set<LearningSkill> skills`. Client đọc response cũ
    (chưa có `skills`) thì suy `skills = {skill}` nếu `skill` khác null, rỗng nếu null.
  - `TopicProgress`: thay `skill` bằng `skills` (migration `V8__topic_skills.sql`: `topic_progress.skills
    VARCHAR(20)[]`, backfill từ cột `skill`, giữ cột `skill` không xóa).
  - `LessonAccessGate.authorize(pendingReviews, currentReviewId, Set<LearningSkill> lessonSkills, ...)`: review chặn
    khi `review.skill() == null || lessonSkills.contains(review.skill())`. Giữ overload một skill bằng cách bọc thành
    `Set.of(skill)` để test cũ không phải sửa hàng loạt.
  - `LessonAccess`, `PracticeAccess`, `AssignTopicTestUseCase`, `GetTopicLessonsUseCase`: dùng `skills` của lesson,
    hoặc của topic với thi topic (thi topic bị chặn nếu review chờ có skill thuộc topic).
  - Skill của review:
    - `PracticeReviewCandidate`, `ReviewReevaluation`: lấy skill từ `KnowledgePointCatalogEntry.skill` của KP, không lấy
      `attempt.skill()` hay `topic.skill()`;
    - `reviews.backfillMissingSkill` vẫn chạy cho dữ liệu cũ.
  - `RefreshLearningTopicsUseCase`: đọc `skills` vào `TopicPlacement`; catalog KP giữ skill riêng của KP như hiện tại.
  - Topic response (`TopicResult`/`TopicResponse`) thêm `skills`; `skill` cũ trả theo luật của content (một phần tử thì
    có, ngược lại null).
- Non-functional: transaction và `LearnerLock` như hiện tại; không thêm lời gọi content trong vòng lặp.

## Related Code Files
- Create: `services/learning-service/src/main/resources/db/migration/V8__topic_skills.sql`
- Modify (gốc `services/learning-service/src/main/java/com/group01/learning/`):
  - `application/port/LearningContentClient.java`, `infrastructure/client/RestLearningContentClient.java`;
  - `domain/entity/TopicProgress.java`, `domain/aggregate/LearnerCurriculum.java`, `domain/service/LessonAccessGate.java`;
  - `application/service/{LessonAccess,PracticeAccess,ReviewReevaluation}.java`;
  - `application/usecase/{RefreshLearningTopicsUseCase,AssignTopicTestUseCase,GetTopicLessonsUseCase,SubmitPracticeAttemptUseCase}.java`;
  - `application/result/TopicResult.java`, `api/dto/response/TopicResponse.java`;
  - `infrastructure/persistence/JdbcLearnerCurriculumRepository.java`.
- Tests: `domain/service/LessonAccessGateTest`, `infrastructure/client/RestLearningContentClientTest`,
  `infrastructure/persistence/{LessonSubmissionIntegrationTest,RemediationLadderIntegrationTest}`,
  `MultiSkillLessonIntegrationTest` (mới), `api/LessonLearningWebMvcTest`.

## Implementation Steps
1. **Test đỏ**:
   - gate: review READING chặn lesson `{READING, LISTENING}`; không chặn lesson `{LISTENING}`; review skill null chặn
     mọi lesson;
   - client: response có `skills`; response cũ chỉ có `skill`;
   - integration (stub content, lesson R+L+W):
     - lesson `completed` khi block R và L đạt dù essay chưa nộp (giữ luật `SubmitLessonExerciseUseCase`);
     - review sinh từ KP Reading trong lesson nhiều skill có skill `READING`;
     - thi topic bị chặn khi có review READING và topic chứa READING.
2. Migration V8, entity, repo.
3. Client + refresh + gate + access services + review skill.
4. DTO.
5. `mvn -q -pl services/learning-service -am test`.

## Success Criteria
- [ ] Test cũ của gate, thang ôn tập, skill track xanh (overload một skill giữ hành vi).
- [ ] Không còn chỗ nào trong `application`/`domain` gọi `topic.skill()` hay `lesson.skill()` để ra quyết định gác cổng
      (grep xác nhận).

## Risk Assessment
- Bỏ sót một chỗ dùng `skill()` đơn sẽ làm lesson nhiều skill bị gác sai. Có bước grep bắt buộc ở success criteria.
