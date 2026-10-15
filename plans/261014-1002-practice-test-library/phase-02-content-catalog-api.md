---
phase: 2
title: "Content catalog API và payload nội bộ"
status: pending
priority: P1
dependencies: [1]
---

# Phase 2: Content catalog API và payload nội bộ

## Overview
Hai việc ở content-service (D2, D4, D5):
1. Catalog cho học viên: `POST /api/content/practice-tests/search` trả thẻ đề đã lọc, phân trang, ở hai chế độ `PART` (mỗi thẻ là một section) và `FULL` (mỗi thẻ là một đề).
2. Payload nội bộ `GET /internal/learning-content/package-versions/{id}` mang thêm số part, giới hạn thời gian và nhóm câu hỏi để Assessment chụp vào attempt.

Dùng `POST` vì danh sách loại trừ có thể dài (tiền lệ: `POST /practice-sets/search`). Route `/api/content/**` đã tới content-service nên
**không đổi Gateway**.

## Requirements
- Functional:
  - `POST /api/content/practice-tests/search`, quyền như `TopicController` đọc (`ADMIN`, `CONTENT_AUTHOR`, `CUSTOMER`, `EXAMINER`; sao chép đúng tập role của controller đó).
  - Request: `skill` (bắt buộc), `scope` (`FULL`|`PART`, bắt buộc), `partNo` (1-4, chỉ với `PART`; gửi kèm `FULL` → 422), `sources` (mã nguồn, ≤ 10),
    `query` (≤ 100 ký tự, trim; khớp `ILIKE` trên tiêu đề gói và tiêu đề section với `%` và `_` đã escape), `exclude` và `only`
    (mỗi cái ≤ 500 phần tử `{packageId, partNo?}`; gửi cả hai → 422; `partNo` bỏ trống nghĩa là cả đề), `page` (≥ 0, mặc định 0), `size` (1-50, mặc định 12).
  - `skill` chỉ có dữ liệu cho `READING` và `LISTENING` (D7); `WRITING`/`SPEAKING` trả trang rỗng, không lỗi (sidebar vẫn gọi được).
  - Chỉ lấy gói `PRACTICE_TEST` `PUBLISHED`, có `current_published_version_id`, version `PUBLISHED`. `PART` bỏ section không có `part_no`.
  - Thứ tự: `published_at` của version giảm dần, rồi `package.code`, rồi `part_no`.
  - Khóa loại trừ/chỉ lấy: `FULL` dùng `(package_id, 0)`, `PART` dùng `(package_id, part_no)`. Gợi ý SQL:
    `(p.id, coalesce(s.part_no, 0)) [NOT] IN (SELECT * FROM unnest(:ids::uuid[], :parts::int[]))`.
  - Thẻ trả về (cùng một dạng cho hai chế độ): `packageId`, `packageCode`, `packageVersionId`, `scope`, `title` (tiêu đề section với `PART`, tiêu đề gói với `FULL`),
    `skill`, `coverUrl`, `source {code, name}` hoặc null (tên lấy từ `TestSource`), `formats` (các `format_code` khác nhau theo thứ tự nhóm), `questionCount`,
    `timeLimitSeconds` (null nếu thiếu), `requiredFeatureKey` (null = miễn phí), `parts[]` với `{sectionId, partNo, title, questionCount, formats}`.
    `PART` có đúng một phần tử trong `parts`. `FULL`: `questionCount` là tổng, `timeLimitSeconds` là tổng khi **mọi** section có giới hạn, `coverUrl` là ảnh của part nhỏ nhất.
  - Ảnh bìa: asset `IMAGE` `VALID`. `MediaReferencePolicy.resolve` hiện chỉ nhận URL `https` hoặc data URI cho ảnh; mở rộng để ảnh cũng nhận object key
    ghép với `CONTENT_MEDIA_BASE_URL` như audio. Ảnh lỗi/không hợp lệ **không** làm hỏng cả trang: `coverUrl = null` và ghi log chỉ kèm id asset.
  - Payload nội bộ (additive): `Section` thêm `partNo`, `timeLimitSeconds`, `groups[] {groupId, sortOrder, formatCode, instructions}`; `Item` thêm `groupId`.
    `packageId` **đã có** trong payload này.
- Non-functional: danh sách phân trang và lọc ở DB (`AGENTS.md` §3.7); số query cố định theo kích thước trang, không N+1 (một query trang + đếm, một query nhóm cho cả trang,
  một query đếm câu hỏi, một query ảnh); không `findAll()` rồi lọc trong bộ nhớ. Học viên không nhận `answerSpec` (catalog không đọc nó).

## Architecture
```
api/controller/PracticeTestController ── SearchPracticeTestsUseCase ── application/port/PracticeTestCatalogReader
                                                                          │
                                  infrastructure/persistence/adapter/JdbcPracticeTestCatalogReader (NamedParameterJdbcTemplate)
```
Reader mới là file riêng vì `JdbcLearningContentReader` đã dài 736 dòng và phục vụ luồng học. Use case kiểm hợp lệ request và phân quyền theo role ở controller,
rồi gọi reader; ghép `coverUrl` qua `MediaReferencePolicy` ở use case (reader trả `media_reference` thô).
Payload nội bộ: mở rộng `PackageVersionContentResult`/`Response` và `JdbcLearningContentReader.publishedPackageVersion` bằng thêm một query nhóm theo danh sách section của version (không query theo từng section).

## Related Code Files
Gốc: `services/content-service/src/main/java/com/ieltspath/content/`
- Create: `api/controller/PracticeTestController.java`, `api/dto/request/SearchPracticeTestsRequest.java`, `api/dto/response/{PracticeTestPageResponse,PracticeTestCardResponse}.java`,
  `application/command/SearchPracticeTestsCommand.java`, `application/usecase/SearchPracticeTestsUseCase.java`,
  `application/port/PracticeTestCatalogReader.java`, `application/result/PracticeTestCardResult.java`,
  `infrastructure/persistence/adapter/JdbcPracticeTestCatalogReader.java`
- Modify: `domain/vo/MediaReferencePolicy.java` (ảnh nhận object key), `application/result/PackageVersionContentResult.java`,
  `api/dto/internal/PackageVersionContentResponse.java`, `infrastructure/persistence/adapter/JdbcLearningContentReader.java` (`publishedPackageVersion`),
  `infra/config-server/config-repo/content-service.yaml` (thêm `/api/content/practice-tests/**` vào `springdoc.paths-to-match` nếu file đang liệt kê route)
- Tests (gốc `src/test/java/com/ieltspath/content/`): `api/controller/PracticeTestControllerTest.java` (WebMvcTest: quyền, 422),
  `application/usecase/SearchPracticeTestsUseCaseTest.java`, `infrastructure/persistence/JdbcPracticeTestCatalogReaderTest.java` (Testcontainers),
  `domain/vo/MediaReferencePolicyTest.java` (thêm ca ảnh), `api/controller/InternalLearningContentControllerTest.java` (field mới)

## Implementation Steps
1. **Test đỏ**:
   - `SearchPracticeTestsUseCaseTest`: `FULL` kèm `partNo` bị từ chối; `exclude` và `only` cùng lúc bị từ chối; `size` 0 hoặc 51 bị từ chối; `query` 101 ký tự bị từ chối; skill `WRITING` trả trang rỗng.
   - `JdbcPracticeTestCatalogReaderTest` (tự `INSERT` fixture bằng `JdbcTemplate`, không phụ thuộc seed phase 8): lọc theo `skill`, `partNo`, `sources`, `query` có ký tự `%`;
     `PART` không trả section thiếu `part_no`; `FULL` trả một thẻ mỗi đề với `parts` đủ; `exclude` theo `(package, part)` chỉ loại đúng part đó, part khác của cùng đề vẫn hiện;
     `only` chỉ trả đúng các khóa; gói `DRAFT`/chưa có version published bị loại; `sources` chỉ trả đề đúng nguồn; `formats` đúng thứ tự nhóm; phân trang đúng `totalElements`; ảnh lỗi cho `coverUrl = null`.
   - `MediaReferencePolicyTest`: ảnh nhận object key khi có base URL; vẫn từ chối `javascript:`, đường dẫn tuyệt đối, `..`.
   - `InternalLearningContentControllerTest`: payload có `partNo`, `timeLimitSeconds`, `groups`, `groupId`; payload cũ (không nhóm) trả `groups: []` và `groupId: null`.
2. Mở rộng `MediaReferencePolicy` cho ảnh.
3. Viết reader, use case, controller, DTO.
4. Mở rộng payload nội bộ (result, response, query nhóm batched).
5. Cập nhật `springdoc.paths-to-match` nếu cần.
6. `mvn -q -pl services/content-service -am test`.

## Success Criteria
- [ ] Test mới đỏ rồi xanh; test content cũ vẫn xanh.
- [ ] Số query của reader không đổi khi tăng `size` từ 1 lên 50 (kiểm trong test hoặc ghi rõ cách đếm trong Verification).
- [ ] Response không chứa `answerSpec`, `explanation` hay transcript.
- [ ] Người gọi ngoài tập role bị 403; request sai hợp lệ bị 422 theo `GlobalExceptionHandler`.
- [ ] Consumer cũ của payload nội bộ (Learning, Game) vẫn đọc được vì chỉ thêm field.

## Risk Assessment
- `exclude`/`only` giới hạn 500 phần tử: đủ cho lịch sử một học viên trong giai đoạn đầu; khi vượt, phải chuyển sang cách ghép khác (ghi vào Verification nếu thực tế vượt).
- `ILIKE '%…%'` không dùng index; chấp nhận khi số đề cỡ vài nghìn. Nếu chậm thì mới thêm `pg_trgm` (cần duyệt vì là extension mới).
- Tìm theo tên không bỏ dấu tiếng Việt; chưa hỗ trợ.
- Mở rộng `MediaReferencePolicy` là thay đổi ở `domain/vo`; giữ tính thuần (không Spring), chỉ thêm nhánh `IMAGE` giống `AUDIO`.
