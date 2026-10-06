---
phase: 6
title: "Learning course path and course test"
status: pending
priority: P1
dependencies: [2, 5]
---

<!-- Updated: Validation Session 1 - mọi course mở, bỏ OPTIONAL, recommended từ placement, API mới chỉ CUSTOMER -->

# Phase 6: Learning course path and course test

## Overview
Chuỗi topic theo course với mọi course đều mở (D4, D5).
<!-- Updated: Validation Session 2 - chuỗi theo course, không tách skill --> Thêm danh sách course có `recommended` (D10) và thi
cuối course là mốc không chặn (D8). Parser nhận `COURSE_GATE` trước khi assessment phát (D9).

## Requirements
- Functional:
  - `LearningContentClient.Topic` thêm `Course course(courseId, code, name, bandLevel, hasCourseTest)`; thêm
    `getCourseTestPackages(courseId)`. Topic-sequence cũ không có `course` → `null`, không vỡ.
  - `RefreshLearningTopicsUseCase` sắp `course.bandLevel → free/premium → sortOrder → topicId` (D7);
    `TopicPlacement`/`TopicProgress` mang `courseId`.
  - Migration `V7__course_progress.sql`:
    - `topic_progress` thêm `course_id UUID`;
    - `course_progress(user_id, course_id, passed_at, PRIMARY KEY(user_id, course_id))`;
    - `course_test_assignments`: cùng shape `topic_test_assignments`, thay `topic_id` bằng `course_id`, unique open
      index, index `(user_id, package_version_id, assigned_at)`.
  - `TopicStatusDeriver`: đổi khóa chuỗi từ `skill` thành `courseId` (không còn tách theo skill). Mọi thứ khác giữ nguyên (passed →
    `PASSED`; topic chưa pass đầu tiên của mỗi course → `IN_PROGRESS`; còn lại `LOCKED`). `courseId` null là một nhóm riêng.
  - `LessonAccessGate`, `AssignTopicTestUseCase`, `GetTopicLessonsUseCase`: **không đổi luật**; review chờ vẫn chặn theo
    skill ở mọi course (D6). Chỉ cần test xác nhận.
  - `TopicResult`/`TopicResponse` thêm `course {courseId, code, bandLevel}`.
  - `GET /api/learning/courses` (`@PreAuthorize("hasRole('CUSTOMER')")`) → mảng sắp theo band:
    - shape `{courseId, code, name, bandLevel, topicCount, passedTopicCount, recommended, testStatus, passedAt}`;
    - `testStatus` ∈ `NONE` (không có COURSE_TEST), `LOCKED` (còn topic chưa PASS), `AVAILABLE`, `PASSED`;
    - `recommended`: theo D10 từ `learner_placements`; không có placement thì mọi course `false`;
    - dữ liệu lấy từ một lần refresh topic-sequence + `course_progress` + `learner_placements`, không gọi content theo
      từng course.
  - `POST /api/learning/courses/{id}/test-assignments` (`CUSTOMER`):
    - mọi topic của course phải `PASSED`, không thì 403 `COURSE_TEST_LOCKED`;
    - course không có test → 409 `NO_COURSE_TEST`; course đã PASS → 409 `COURSE_ALREADY_PASSED`;
    - còn assignment mở thì trả lại assignment đó;
    - chọn package bằng `PackageRotation.leastRecentlyUsed`; hết package → 409 `TEST_UNAVAILABLE`;
    - trả `TestAssignmentResponse` như thi topic.
    - Lỗi dùng `LearningRequestException` (403/409) ở tầng application, không dùng `LearningGateException` (luôn map 403
      và kèm review).
  - Consumer:
    - parser chấp nhận `COURSE_GATE`;
    - `ApplyAssessmentResultUseCase`: `COURSE_GATE` ghi evidence và nằm trong `REVIEWED_TYPES` như `TOPIC_GATE`;
    - assignment mở khớp `packageVersionId` → `consume`; ≥ `PassMark` (70%) thì ghi `course_progress.passed_at` (một
      chiều).
- Non-functional:
  - mọi lượt ghi trong một transaction có `LearnerLock`;
  - không gọi content trong vòng lặp;
  - `@PreAuthorize` chỉ trên API mới, API learning cũ giữ nguyên.

## Architecture
```
TopicStatusDeriver (chuỗi theo courseId) ← LearnerCurriculum ← JdbcLearnerCurriculumRepository (+course_id)
CourseProgress (domain/aggregate, pass một chiều)   CourseTestAssignment (domain/aggregate, mirror TopicTestAssignment)
ListCoursesUseCase, AssignCourseTestUseCase (application/usecase) → CourseController (api)
ApplyAssessmentResultUseCase.applyCourseGate(...) — dùng chung PassMark, PackageRotation
```
Không gộp `TopicTestAssignment` và `CourseTestAssignment` thành một aggregate có scope nullable: hai bảng tách giữ luật
unique rõ và không đụng flow thi topic đang chạy ổn.

## Related Code Files
- Create: `V7__course_progress.sql`, `domain/aggregate/{CourseProgress,CourseTestAssignment}.java`,
  `domain/repository/{CourseProgressRepository,CourseTestAssignmentRepository}.java`,
  `infrastructure/persistence/{JdbcCourseProgressRepository,JdbcCourseTestAssignmentRepository}.java`,
  `application/usecase/{ListCoursesUseCase,AssignCourseTestUseCase}.java`, `application/result/CourseResult.java`,
  `api/controller/CourseController.java`, `api/dto/response/CourseResponse.java`
- Modify: `domain/service/TopicStatusDeriver.java`, `domain/entity/TopicProgress.java`,
  `domain/aggregate/LearnerCurriculum.java`, `application/port/LearningContentClient.java`,
  `infrastructure/client/RestLearningContentClient.java`,
  `application/usecase/{RefreshLearningTopicsUseCase,ApplyAssessmentResultUseCase}.java`,
  `application/result/TopicResult.java`, `api/dto/response/TopicResponse.java`,
  `infrastructure/persistence/JdbcLearnerCurriculumRepository.java`, `infrastructure/messaging/AssessmentCompletedParser.java`
- Tests:
  - `domain/service/TopicStatusDeriverTest`, `domain/service/LessonAccessGateTest`, `domain/aggregate/ProgressAggregatesTest`;
  - `infrastructure/client/RestLearningContentClientTest`, `infrastructure/messaging/AssessmentCompletedParserTest`;
  - `infrastructure/persistence/ReviewAndTestAssignmentIntegrationTest` (+ case course), `CoursePathIntegrationTest` (mới);
  - `api/LessonLearningWebMvcTest`, `api/LearningSecurityWebMvcTest` (EXAMINER gọi `/courses` → 403).

## Implementation Steps
1. **Test đỏ (domain trước)**, `TopicStatusDeriverTest` (ví dụ R1→R2→L1 ở 5.5; R3→R4 ở 6.5):
   - học viên mới: R1, R3 `IN_PROGRESS`; R2, L1, R4 `LOCKED`;
   - PASS R3 → R4 `IN_PROGRESS`, R1/R2/L1 không đổi; PASS R1, R2 → L1 `IN_PROGRESS`;
   - topic `courseId` null gom nhóm riêng;
   - test cũ dựa trên chuỗi song song theo skill (Reading và Listening cùng `IN_PROGRESS`) phải **cập nhật kỳ vọng**
     sang chuỗi theo course; ghi rõ trong commit đây là đổi luật chủ ý.
2. Test đỏ `LessonAccessGateTest`: review Reading chờ chặn R3 (6.5) dù review sinh ở R1 (5.5); L1 (Listening) không bị review đó chặn.
3. Test đỏ `CourseTestAssignment`/`CourseProgress` (consume một lần, pass một chiều); parser `COURSE_GATE`; client đọc
   `course`.
4. Test đỏ integration `CoursePathIntegrationTest` (Testcontainers + stub content):
   - trạng thái theo ví dụ;
   - `COURSE_TEST_LOCKED` → xong hết → được giao mã đề;
   - `COURSE_GATE` 80% → `PASSED`; 60% → chưa, lần giao sau đổi package; replay event không đổi kết quả;
   - placement 6.0 → course 6.5 `recommended`; placement 9.0 → course cao nhất; không placement → không course nào.
5. Code: migration V7 → entity/aggregate → deriver → repo → client → use case → controller → consumer.
6. `mvn -q -pl services/learning-service -am test`.

## Success Criteria
- [ ] Mọi test learning cũ xanh (đặc biệt `RemediationLadderIntegrationTest`, `ReviewAndTestAssignmentIntegrationTest`,
      `TopicStatusDeriverTest`).
- [ ] Không thêm giá trị `TopicStatus`; response topic chỉ thêm field `course`.
- [ ] Không có lời gọi content trong vòng lặp.

## Risk Assessment
- Mở mọi course nghĩa là học viên band thấp vào thẳng 6.5: đây là quyết định chủ ý (D4), gợi ý từ placement bù lại.
- Đổi luật: topic Reading và Listening cùng course không còn học song song (chủ ý, chuẩn bị lesson nhiều skill). Test
  `TopicStatusDeriverTest` và integration của skill track phải sửa kỳ vọng.
- FE nên nhóm topic theo course (dùng field `course`).
- Học viên có topic_progress cũ: refresh tiếp theo ghi `course_id`; trước refresh thì nhóm null, giữ hành vi cũ.
