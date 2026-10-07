---
phase: 1
title: "Content multi-skill lessons and practice skills"
status: pending
priority: P1
dependencies: []
---

# Phase 1: Content multi-skill lessons and practice skills

## Overview
Content ngừng gắn một skill cho topic/lesson. Skill được suy ra từ câu hỏi, và internal API trả `skills` cho topic,
lesson, Practice set. Luật publish đổi theo (M5, M7, M10).

## Requirements
- Functional:
  - Skill suy ra:
    - lesson `skills` = tập skill của câu hỏi trong block EXERCISE của lesson, cộng skill của KP gắn với block TEXT;
    - topic `skills` = hợp các lesson PUBLISHED;
    - Practice set `skills` = tập skill của câu hỏi trong version PUBLISHED.
    Mọi tập sắp theo thứ tự enum `Skill`, không có `ALL`.
  - `topic-sequence`:
    - bỏ điều kiện `t.skill IS NOT NULL` (giữ: ACTIVE, có course ACTIVE, có lesson PUBLISHED);
    - thêm `skills: [...]`;
    - field `skill` cũ trả skill duy nhất khi `skills` có đúng 1 phần tử, ngược lại `null` (tương thích reader cũ).
  - `GET /internal/learning-content/lessons/{id}`: thêm `skills`; `skill` cũ theo cùng luật.
    `GET /topics/{id}/lessons` (summary) thêm `skills`.
  - `GET /internal/learning-content/lessons/{id}/practice-sets?skill=X`:
    - mỗi set có `skills`;
    - có `skill` → chỉ set mà `skills == {X}`; `skill` không hợp lệ → 400;
    - `topics/{id}/practice-sets` cũng có `skills`.
  - Publish (`PublishContentPackageUseCase`):
    - thay `packageVersionLeavesLessonSkill` bằng `packageVersionLeavesLessonSkills`: câu của Practice phải có skill
      thuộc `skills` của lesson;
    - essay trong `PRACTICE_SET`, `TOPIC_TEST`, `COURSE_TEST` phải có `answer_spec.passBand` (0–9, bước 0.5), không có
      thì từ chối;
    - **sửa luật của plan Course (M15):** `PublishContentPackageUseCase` hiện bắt `COURSE_TEST` chỉ gồm câu Reading tự
      chấm. Đổi thành: câu Reading hoặc Listening có spec CHOICE/FILL chấm được, hoặc essay Writing có `passBand`;
      Speaking và spec không chấm được vẫn bị từ chối. Sửa 2 test của `CoursePackagesUseCaseTest` theo bảng test cũ.
  - `UpdateTopicUseCase`: bỏ luật "không đổi skill khi có lesson PUBLISHED" (topic skill không còn ý nghĩa với lộ
    trình); vẫn validate giá trị enum nếu gửi lên.
- Non-functional: tính `skills` bằng query gom theo tập (`array_agg(DISTINCT ...)`), không query theo từng lesson/set.

## Architecture
`JdbcLearningContentReader` tính `skills` trong cùng query lấy topic/lesson/practice set (subquery `ARRAY(SELECT
DISTINCT q.skill ...)`). `LearningContentReader.packageVersionLeavesLessonSkills(versionId, lessonId)` thay hàm cũ.
Thêm `PracticeSetSkillFilter` (parse `skill` ở controller → `Optional<Skill>` vào use case).

## Related Code Files
- Modify (gốc `services/content-service/src/main/java/com/group01/content/`):
  - `application/port/LearningContentReader.java`, `infrastructure/persistence/adapter/JdbcLearningContentReader.java`;
  - `application/result/{TopicSequenceResult,LessonContentResult,LessonSummaryResult,LessonPracticeSetResult,LessonPracticeSetsResult}.java`;
  - `api/dto/internal/{TopicSequenceResponse,LessonContentResponse,LessonSummaryResponse,LessonPracticeSetResponse,TopicPracticeSetsResponse}.java`;
  - `api/controller/InternalLearningContentController.java`;
  - `application/usecase/{PublishContentPackageUseCase,UpdateTopicUseCase,GetLessonPracticeSetsUseCase,GetTopicPracticeSetsUseCase,GetLessonContentUseCase,GetTopicLessonsUseCase}.java`;
  - `domain/vo/LessonBlockKind.java` (dùng lại `ESSAY_SPEC_TYPE` để nhận diện essay).
- Docs: `docs/contracts/learning-content-internal-v1.md`.
- Tests: `InternalLearningContentControllerTest`, `LearningContentUseCasesTest`, `PublishContentPackageUseCaseTest`,
  `UpdateTopicUseCaseTest`, Testcontainers `MultiSkillContentIntegrationTest` (mới).

## Implementation Steps
1. **Test đỏ**:
   - topic có lesson R+L vào sequence với `skills=[LISTENING, READING]` (theo thứ tự enum) và `skill=null`;
   - topic một skill → `skill` như cũ;
   - `?skill=READING` chỉ trả set thuần R; set trộn không lọt; `?skill=FOO` → 400;
   - publish Practice có câu WRITING cho lesson không có WRITING → lỗi; essay thiếu `passBand` → lỗi;
   - đổi skill topic có lesson PUBLISHED → được.
2. Reader: subquery skills; bỏ điều kiện skill; filter `skill`.
3. Publish rules; Update topic.
4. DTO, controller, contract doc.
5. `mvn -q -pl services/content-service -am test`.

## Success Criteria
- [ ] Test mới đỏ trước, xanh sau; test seed cũ xanh (topic một skill vẫn có `skill`).
- [ ] Reader cũ (learning chưa sửa) vẫn đọc được response, vì chỉ thêm field.

## Risk Assessment
- Ngay sau phase này, topic nhiều skill có `skill=null`. Learning cũ dựa vào `skill` sẽ coi là null, nên phải xong
  phase 3 trước khi seed topic nhiều skill lên môi trường chung (cùng PR, M13).
- Sửa luật `COURSE_TEST` của plan Course: ghi rõ trong commit.
