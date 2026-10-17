---
phase: 1
title: "Content model và authoring"
status: pending
priority: P1
dependencies: []
---

# Phase 1: Content model và authoring

## Overview
Đưa code của content-service lên khớp schema mới (D1, D3, D5, D8, D9): loại gói `PRACTICE_TEST`, nguồn tài liệu, số part và ảnh bìa của section, nhóm câu hỏi trong section,
và luật publish cho đề luyện.

**Schema không định nghĩa ở đây.** Bảng, cột, ràng buộc và quan hệ (migration content `V28`) do plan
[`261014-1021-practice-test-data-model`](../261014-1021-practice-test-data-model/plan.md) làm trước; phase này chỉ viết code dùng chúng.

Hiện **không có API nào thêm câu hỏi vào section**; toàn bộ nội dung hiện có nạp bằng migration Flyway. Phase này giữ quy ước đó:
nội dung đề luyện mới vào bằng migration seed (phase 8), API chỉ mở rộng cho `part_no`, ảnh bìa và `source_code`. API soạn câu hỏi/nhóm nằm ngoài phạm vi.

## Requirements
- Functional:
  - Tiền điều kiện: `V28` đã áp dụng (`section_question_groups`, `content_packages.source_code`, `content_sections.part_no`/`cover_asset_id`, `section_questions.group_id`, loại gói `PRACTICE_TEST`).
    Nếu chưa có, dừng và ghi `BLOCKED`.
  - `QuestionFormat` (domain/vo): `GAP_FILLING`, `SENTENCE_COMPLETION`, `SUMMARY_COMPLETION`, `NOTE_COMPLETION`, `FORM_COMPLETION`,
    `TABLE_COMPLETION`, `FLOW_CHART_COMPLETION`, `SHORT_ANSWER`, `MULTIPLE_CHOICE_ONE`, `MULTIPLE_CHOICE_MANY`, `TRUE_FALSE_NOT_GIVEN`,
    `YES_NO_NOT_GIVEN`, `MATCHING_INFORMATION`, `MATCHING_FEATURES`, `MATCHING_HEADINGS`, `DIAGRAM_LABELLING`, `MAP_LABELLING`.
    DB chỉ kiểm dạng chữ hoa của `format_code`, nên giá trị hợp lệ do enum này quyết định; giá trị lạ khi đọc → lỗi rõ.
    `MULTIPLE_CHOICE_MANY` có thể lưu nhưng chưa chấm được cho tới khi phase 7 được duyệt (publish chặn, xem dưới).
  - `PackageType.PRACTICE_TEST`.
  - `TestSource` (domain/vo): `IELTS_SPACE_PRO` ("The IELTS Space PRO"), `C10_C20` ("C10-C20"), `ACTUAL_TESTS` ("Actual Tests"), `OTHER` ("Các nguồn khác"),
    mỗi giá trị mang tên hiển thị và thứ tự. Đây là nơi duy nhất định nghĩa nguồn (không có bảng); mã lạ khi đọc → lỗi rõ.
  - `POST /api/content/packages` nhận `sourceCode` (nullable); `POST /api/content/packages/versions/{versionId}/sections` nhận `partNo`,
    `coverAssetId` (nullable). `coverAssetId` phải là asset `IMAGE` có sẵn, ngược lại 400/404 theo `GlobalExceptionHandler` hiện có.
    Response của package và section trả thêm các field này (additive).
  - Luật publish cho `PRACTICE_TEST` (`PublishContentPackageUseCase`):
    - Cùng nhánh khóa/kiểm câu hỏi dùng chỗ khác như `MOCK_TEST`; `requiredPurpose = EXAM`.
    - Mọi câu hỏi phải tự chấm được và thuộc Reading hoặc Listening (tái dùng điều kiện của `gradableCourseQuestion`, đổi tên thành
      tên chung như `gradableObjectiveQuestion` vì giờ dùng cho hai loại gói).
    - Gói có `source_code` thuộc `TestSource`; có ít nhất một section; mọi section cùng một `skill` (không rỗng, chỉ `READING` hoặc `LISTENING`);
      mọi section có `part_no` (1-4) và mỗi section có ít nhất một câu hỏi.
    - Section có nhóm thì **mọi** câu của section phải thuộc một nhóm; nhóm dùng `MULTIPLE_CHOICE_MANY` bị từ chối khi chưa có phase 7.
    - Vi phạm → `InvalidContentStateException` (cùng cách các luật publish khác).
  - Thêm `PRACTICE_TEST` vào mọi chỗ liệt kê loại gói trong SQL của `JdbcLearningContentReader` (các dòng có
    `IN ('TOPIC_TEST', 'COURSE_TEST', 'MOCK_TEST', 'PLACEMENT_TEST'...)`, hiện ở ~74, ~497, ~710) và javadoc của `LearningContentReader`:
    câu hỏi của đề luyện phải bị coi là "đã có chủ" để không lọt vào bài ôn hay bị dùng lại ở gói khác.
- Non-functional: theo layer content (aggregate tách JPA entity, MapStruct, adapter); không đổi Gateway; gói và section cũ vẫn đọc được vì mọi cột mới rỗng với dữ liệu cũ.

## Architecture
```
domain/vo/QuestionFormat ── domain/entity/SectionQuestionGroup ── ContentSection (partNo, coverAssetId, groups) ── SectionQuestion (groupId)
domain/vo/TestSource (mã, tên hiển thị, thứ tự; không có bảng)
ContentPackage (sourceCode)
infrastructure/persistence: SectionQuestionGroupJpaEntity;
  ContentSection/SectionQuestion/ContentPackage JPA entity + ContentPackagePersistenceMapper thêm field
PublishContentPackageUseCase: thêm nhánh PRACTICE_TEST, các luật bảng trên
```
`ContentSection` tự bảo vệ `partNo > 0`; việc "mọi section cùng skill", "mọi câu thuộc nhóm" cần biết nhiều section nên nằm ở use case publish
(không đưa JPA/Spring vào `domain`).

## Related Code Files
Gốc: `services/content-service/src/main/java/com/ieltspath/content/`
- Create: `domain/vo/QuestionFormat.java`,
  `domain/vo/TestSource.java`, `domain/entity/SectionQuestionGroup.java`,
  `infrastructure/persistence/entity/SectionQuestionGroupJpaEntity.java`
- Modify: `domain/vo/PackageType.java`, `domain/aggregate/ContentPackage.java`, `domain/entity/{ContentSection,SectionQuestion}.java`,
  `infrastructure/persistence/entity/{ContentPackageJpaEntity,ContentSectionJpaEntity,SectionQuestionJpaEntity}.java`,
  `infrastructure/persistence/mapper/ContentPackagePersistenceMapper.java`,
  `application/command/{CreateContentPackageCommand,AddContentSectionCommand}.java`,
  `application/usecase/{CreateContentPackageUseCase,AddContentSectionUseCase,PublishContentPackageUseCase}.java`,
  `application/result/{ContentPackageResult,ContentSectionResult}.java`, `api/dto/request/{CreateContentPackageRequest,AddContentSectionRequest}.java`,
  `api/dto/response/{ContentPackageResponse,ContentSectionResponse}.java`, `api/controller/ContentPackageController.java`,
  `infrastructure/persistence/adapter/JdbcLearningContentReader.java`, `application/port/LearningContentReader.java` (javadoc)
- Tests (gốc `src/test/java/com/ieltspath/content/`): `domain/aggregate/ContentPackageTest.java`, `domain/entity/ContentSectionTest.java`,
  `application/usecase/PublishContentPackageUseCaseTest.java`, `application/usecase/AddContentSectionUseCaseTest.java`,
  `infrastructure/persistence/PracticeTestPersistenceTest.java` (Testcontainers: lưu và đọc lại các field mới qua adapter, không kiểm DDL)

## Implementation Steps
1. **Test đỏ**: `ContentSectionTest` (partNo 0 bị từ chối); `PublishContentPackageUseCaseTest` thêm các ca cho `PRACTICE_TEST`:
   câu hỏi `purpose` sai → `QuestionPurposeMismatchException`; hai section khác skill; section thiếu `part_no`; section có nhóm mà một câu chưa có nhóm;
   thiếu `source_code` hoặc mã ngoài `TestSource`; câu `ESSAY`; nhóm `MULTIPLE_CHOICE_MANY`; đề hợp lệ publish được.
   `AddContentSectionUseCaseTest` (`coverAssetId` không phải IMAGE → lỗi). `PracticeTestPersistenceTest`: lưu section có `partNo`/`coverAssetId`/nhóm rồi đọc lại đúng.
2. Domain + repository + adapter + mapper cho các field và entity mới.
3. Mở rộng use case/DTO/controller cho `sourceCode`, `partNo`, `coverAssetId`.
4. `PublishContentPackageUseCase`: thêm `PRACTICE_TEST` vào các nhánh switch hiện có (khóa/kiểm dùng chỗ khác, `requiredPurpose = EXAM`, kiểm essay/tự chấm), thêm luật riêng.
5. Thêm `PRACTICE_TEST` vào SQL của `JdbcLearningContentReader` và javadoc.
6. `mvn -q -pl services/content-service -am test`.

## Success Criteria
- [ ] Test mới đỏ trước khi code, xanh sau; test content cũ vẫn xanh (không sửa kỳ vọng nào vì phase này không seed).
- [ ] Không có import JPA/Spring trong `domain/`.
- [ ] Gói và section cũ vẫn đọc được qua code mới (field mới rỗng).
- [ ] Một `PRACTICE_TEST` thiếu `source_code`, hoặc trộn skill, hoặc có section không số part, **không** publish được.

## Risk Assessment
- Danh sách loại gói trong SQL của `JdbcLearningContentReader` nằm rải rác; sót một chỗ thì câu hỏi đề luyện có thể lọt vào bài ôn. Test: seed một
  `PRACTICE_TEST` rồi khẳng định `questionVersionsReservedForLearning` và `questionsUsedElsewhere` thấy câu hỏi của nó.
- DB không ép "đề luyện phải có nguồn" hay "mọi câu thuộc nhóm"; hai luật đó chỉ có ở publish, nên test publish phải phủ đủ.
- Schema và rollback do plan data-model; phase này chỉ revert code nếu cần.
