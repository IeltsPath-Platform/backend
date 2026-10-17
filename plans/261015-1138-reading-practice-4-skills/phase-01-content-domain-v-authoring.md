---
phase: 1
title: "Content domain và authoring"
status: pending
priority: P1
effort: "1d"
dependencies: []
---

# Phase 1: Content domain và authoring

## Overview
Đưa code content-service lên khớp schema `V28` đã có: loại gói `PRACTICE_TEST`, nguồn tài liệu, `part_no`/ảnh bìa của section, nhóm câu hỏi, luật publish.
Sau phase này, 3 gói demo `V30` đọc được qua mọi đường (kể cả admin `GET /api/content/packages`).

**Thiết kế gốc (đọc trước, làm theo): [`../261014-1002-practice-test-library/phase-01-content-schema-and-authoring.md`](../261014-1002-practice-test-library/phase-01-content-schema-and-authoring.md).**
Mọi thứ trong đó giữ nguyên, trừ các mục dưới đây.

## Lệch / bổ sung so với phase thiết kế
- Tiền điều kiện `V28`: **đã đạt**. Kiểm bằng `flyway_schema_history` trong test Testcontainers, không cần dừng.
- Không viết DDL. Không đụng `V28`/`V29`/`V30`.
- `PackageType` thêm `PRACTICE_TEST`. Hai chỗ **bắt buộc sửa kẻo vỡ** (đã kiểm trên code):
  - `PublishContentPackageUseCase` dòng ~70 và ~77: `switch` có nhánh `case PRACTICE_SET, TOPIC_TEST, ...` và `switch` con ánh xạ `requiredPurpose`
    (`MOCK_TEST -> EXAM`, `PLACEMENT_TEST -> PLACEMENT`); thêm `PRACTICE_TEST` vào cả hai, với `requiredPurpose = EXAM`.
  - `JdbcLearningContentReader` có 3 danh sách loại gói (dòng ~74, ~497, ~710) và `PackageType.valueOf(...)` ở ~628; thêm `PRACTICE_TEST` vào ba danh sách.
- Luật publish đề luyện giữ nguyên (cùng skill, `part_no` 1-4 duy nhất, nhóm phủ kín, `source_code` thuộc `TestSource`), cho phép `READING` **và** `LISTENING`
  (code không cứng skill). Từ chối `ESSAY`/`SPEAKING` và nhóm `MULTIPLE_CHOICE_MANY` (chưa duyệt Q1).
- Test mới bắt buộc, **thêm** vào danh sách của phase thiết kế: `PracticeTestPersistenceTest` (Testcontainers, Flyway chạy tới `V30`) phải
  1. đọc `PT-READING-ISP-01` qua adapter hiện có (`ContentPackageRepositoryAdapter`) và `findAllFiltered` của admin mà **không lỗi enum**,
  2. thấy `sourceCode = IELTS_SPACE_PRO`, 3 section có `partNo` 1-3 và `coverAssetId`, nhóm và `groupId` của câu hỏi,
  3. `questionsUsedElsewhere`/`questionVersionsReservedForLearning` của `JdbcLearningContentReader` báo câu hỏi của 3 gói demo là "đã có chủ".
- Gói demo `V30` phải qua được luật publish: viết test áp `PublishContentPackageUseCase` lên dữ liệu seed (dựng `ContentPackage` từ DB hoặc fixture tương đương) và xanh.
  Nếu seed vi phạm luật (ví dụ section thiếu nhóm) thì **sửa luật hoặc báo `BLOCKED`**, không sửa `V30` đã áp dụng (muốn đổi seed → `V31` mới, cần hỏi chủ dự án).

## Related Code Files
Gốc: `services/content-service/src/main/java/com/ieltspath/content/` — danh sách Create/Modify/Tests đầy đủ ở phase thiết kế gốc.
Thêm: `src/test/.../infrastructure/persistence/PracticeTestPersistenceTest.java` đọc cả dữ liệu `V30`.

## Implementation Steps
1. Test đỏ theo phase thiết kế + ba ý "test mới bắt buộc" ở trên.
2. Domain/VO/entity/mapper/adapter, rồi use case + DTO + controller (`sourceCode`, `partNo`, `coverAssetId`).
3. `PublishContentPackageUseCase` + `JdbcLearningContentReader`.
4. `mvn -q -pl services/content-service -am test`.

## Success Criteria
- [ ] Test mới đỏ rồi xanh; test content cũ xanh, không sửa kỳ vọng nào (phase này không seed thêm).
- [ ] `GET /api/content/packages` (admin) không còn lỗi khi DB có 3 gói `PRACTICE_TEST` của `V30`.
- [ ] Không import JPA/Spring trong `domain/`.
- [ ] `PRACTICE_TEST` thiếu `source_code`, trộn skill, hoặc có section không số part **không** publish được.

## Risk Assessment
Như phase thiết kế. Thêm: `switch` thiếu nhánh `PRACTICE_TEST` sẽ compile lỗi hoặc ném `IllegalArgumentException` lúc chạy; test publish phải phủ nhánh này.
