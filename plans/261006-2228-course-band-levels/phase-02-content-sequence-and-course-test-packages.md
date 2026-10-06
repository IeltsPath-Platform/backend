---
phase: 2
title: "Content sequence and course test packages"
status: pending
priority: P1
dependencies: [1]
---

# Phase 2: Content sequence and course test packages

## Overview
`topic-sequence` trả course của mỗi topic và chỉ trả topic có course ACTIVE (D2). Thêm loại package `COURSE_TEST` gắn
`course_id` cùng endpoint nội bộ để learning lấy mã đề thi cuối course (D8).

## Requirements
- Functional:
  - `GET /internal/learning-content/topic-sequence`: mỗi topic thêm
    `course: {courseId, code, name, bandLevel, hasCourseTest}`. Điều kiện lọc thêm `t.course_id` trỏ tới course ACTIVE.
    Thứ tự trả về: `skill, course.band_level, sort_order, id`. Learning vẫn tự sắp lại (phase 6).
  - `hasCourseTest`: course có ít nhất một `COURSE_TEST` PUBLISHED có `current_published_version_id`.
  - Migration `V20__course_test_packages.sql`: CHECK `package_type` thêm `'COURSE_TEST'`; `content_packages.course_id
    UUID REFERENCES courses(id)` + CHECK `package_type <> 'COURSE_TEST' OR course_id IS NOT NULL` + index.
  - `PackageType.COURSE_TEST`. `CreateContentPackageUseCase` từ chối `COURSE_TEST` giống `TOPIC_TEST` (chỉ seed).
    `PublishContentPackageUseCase` xử lý `COURSE_TEST` như `TOPIC_TEST` (khóa câu, kiểm câu dùng chỗ khác, purpose
    `LEARNING`). Thêm: mọi câu trong `COURSE_TEST` phải tự chấm được (không phải essay); nếu có loại câu không tự chấm
    thì từ chối khi publish.
  - Mọi danh sách `package_type IN ('TOPIC_TEST', 'MOCK_TEST', 'PLACEMENT_TEST' …)` trong `JdbcLearningContentReader`
    (`LESSON_OR_TEST_QUESTION_VERSIONS`, truy vấn version của package ~dòng 444, ~dòng 616) thêm `'COURSE_TEST'`, để
    câu thi cuối course không bị giao làm set ôn và `package-versions/{id}` đọc được.
  - `GET /internal/learning-content/courses/{id}/test-packages`: cùng shape với `topics/{id}/test-packages`.
- Non-functional: contract `docs/contracts/learning-content-internal-v1.md` cập nhật (field mới là additive, ghi ngày).

## Architecture
`LearningContentReader` thêm `courseTestPackages(UUID courseId)`. `GetCourseTestPackagesUseCase` mirror
`GetTopicTestPackagesUseCase`. `TopicSequenceResult` thêm `CourseEntry course`. Gom course trong cùng query topic
(JOIN `courses`), không query thêm theo từng topic.

## Related Code Files
- Create: `resources/db/migration/V20__course_test_packages.sql`, `application/usecase/GetCourseTestPackagesUseCase.java`
- Modify: `domain/vo/PackageType.java`, `application/usecase/{Create,Publish}ContentPackageUseCase.java`,
  `application/port/LearningContentReader.java`, `application/result/TopicSequenceResult.java`,
  `api/dto/internal/TopicSequenceResponse.java`, `api/controller/InternalLearningContentController.java`,
  `infrastructure/persistence/adapter/JdbcLearningContentReader.java`, `infrastructure/persistence/entity/ContentPackageJpaEntity.java`
  (+ mapper/aggregate `ContentPackage` thêm `courseId`)
- Docs: `docs/contracts/learning-content-internal-v1.md`
- Tests: `api/controller/InternalLearningContentControllerTest.java`, `application/usecase/LearningContentUseCasesTest.java`,
  `application/usecase/PublishContentPackageUseCaseTest.java`, Testcontainers test cho `topicSequence` (mở rộng
  `LessonPipelineSeedTest` hoặc tạo `CourseSequenceIntegrationTest`)

## Implementation Steps
1. **Test đỏ**:
   - topic không có course không xuất hiện; topic có course INACTIVE không xuất hiện; `course.bandLevel` đúng;
     `hasCourseTest` đúng;
   - publish `COURSE_TEST` có câu essay → lỗi; tạo `COURSE_TEST` qua API → 400;
   - câu của `COURSE_TEST` không được tính là eligible practice;
   - endpoint `courses/{id}/test-packages` trả đúng package PUBLISHED.
2. Migration V20 + enum + aggregate/JPA field.
3. Reader: JOIN course trong query topic; thêm `'COURSE_TEST'` vào 3 danh sách; query `courseTestPackages`.
4. Use case + endpoint + DTO.
5. Cập nhật contract doc.
6. `mvn -q -pl services/content-service -am test`.

## Success Criteria
- [ ] Mọi test mới đỏ trước, xanh sau; test content cũ xanh **sau khi phase 3 backfill** (seed topic cũ chưa có course
      sẽ biến mất khỏi sequence giữa phase 2 và 3; nên gộp 2+3 vào một PR).
- [ ] Không N+1: course đến từ JOIN, không lặp query.

## Risk Assessment
- Giữa phase 2 và 3 lộ trình trống vì seed chưa có course → làm 2 và 3 cùng nhánh, cùng PR.
- Quên thêm `COURSE_TEST` vào một danh sách `IN (...)` → câu thi lộ thành set ôn. Có test riêng cho trường hợp này.
