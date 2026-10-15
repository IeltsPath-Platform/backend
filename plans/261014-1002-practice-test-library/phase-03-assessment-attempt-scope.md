---
phase: 3
title: "Assessment attempt theo phạm vi section"
status: pending
priority: P1
dependencies: [2]
---

# Phase 3: Assessment attempt theo phạm vi section

## Overview
Cho phép bắt đầu attempt đề luyện ở hai phạm vi (D1, D2, D3): `FULL` (mọi section) hoặc `PART` (một tập con section qua `sectionIds`), ghi `package_id`
để "đã làm" không phụ thuộc phiên bản, và bật chế độ `TIMED` thật (hiện `expiresAt` luôn null).

## Requirements
- Functional:
  - Tiền điều kiện: schema `V8` đã áp dụng, do plan [`261014-1021-practice-test-data-model`](../261014-1021-practice-test-data-model/plan.md)
    (`assessment_attempts.package_id`, `scope`, `skill`; `attempt_sections.part_no`; ràng buộc và index). Phase này **không** viết DDL; nếu `V8` chưa có, dừng và ghi `BLOCKED`.
    Hệ quả cho code: schema chặn attempt `OFFICIAL_PRACTICE` thiếu `package_id` hoặc `skill`, và chặn `scope = PART`/`skill` ở loại attempt khác, nên code phải ghi đúng các cột này.
  - Ghi cột: `package_id` có ở **mọi** attempt mới (mọi loại); `scope` theo số section được chọn; `skill` chỉ cho `OFFICIAL_PRACTICE` (lấy từ section được chọn; đề luyện
    là một skill, D8) để phase 4 lọc theo skill ở DB thay vì đọc JSON snapshot; `attempt_sections.part_no` lấy từ section.
  - Attempt cũ giữ `package_id` rỗng và `scope = 'FULL'` (không suy ra được từ DB assessment; chúng là placement/gate/mock nên không nằm trong thư viện đề).
  - `AttemptType.forContentPackageType`: thêm `"PRACTICE_TEST" -> OFFICIAL_PRACTICE`.
  - `StartAssessmentAttemptRequest` thêm `sectionIds` (danh sách UUID, nullable; rỗng hoặc null = mọi section). Quy tắc:
    - chỉ cho phép với package `PRACTICE_TEST`; loại khác gửi `sectionIds` → `PackageNotAttemptableException`;
    - mọi id phải thuộc version, không trùng; sai → 4xx theo `GlobalExceptionHandler` hiện có;
    - `scope = PART` khi chọn ít hơn tổng số section, ngược lại `FULL`.
  - `ContentPackageProvider` (port) và `ContentPackageClient`: `PackageVersion` thêm `packageId`; `Section` thêm `partNo`, `timeLimitSeconds`, `groups`; `Item` thêm `groupId`
    (đọc từ payload nội bộ của phase 2). Mọi attempt mới ghi `package_id` (mọi loại, không chỉ đề luyện).
  - `AttemptCreator`: chỉ snapshot các section được chọn (giữ thứ tự `sortOrder` của content); kiểm "section không có câu hỏi" chỉ áp cho section được chọn.
    Snapshot section thêm `partNo` và `groups` (`formatCode`, `instructions`, `sortOrder`); snapshot câu hỏi thêm `groupId`. Không có đáp án trong các field này.
    `AttemptSection` (record) thêm `partNo`.
  - `TIMED`: `expiresAt = bây giờ + tổng timeLimitSeconds của các section được chọn`. Section được chọn nào thiếu `timeLimitSeconds` → từ chối `422` (Q5).
    `STANDARD` giữ `expiresAt = null`.
  - Hết giờ (Q7): với attempt `OFFICIAL_PRACTICE`, nộp bài sau hạn **vẫn được nhận và chấm** trên các đáp án đã lưu trước hạn, thay vì chuyển `EXPIRED` và mất bài.
    Lưu đáp án (`PUT .../items/{itemId}/response`) sau `expiresAt` bị từ chối bằng `AttemptExpiredException`. Loại attempt khác giữ hành vi cũ.
  - `AssessmentAttemptResponse` và `AssessmentAttemptResult` thêm `packageId`, `scope`.
- Non-functional: tất cả vẫn trong transaction của `AttemptCreator`; không gọi Content trong vòng lặp; không đổi hợp đồng của các endpoint attempt khác (chỉ thêm field).

## Architecture
```
AssessmentAttemptController.start ── StartAssessmentAttemptUseCase (kiểm sectionIds, chọn scope)
        │                                  │
        │                       ContentPackageProvider.findPackageVersion ── ContentPackageClient (HTTP, payload phase 2)
        └─ AttemptCreator.create(command, type, packageVersion, chosenSections, scope, expiresAt)
                  └─ AssessmentAttempt.start(..., packageId, scope, expiresAt) → attempt_sections (partNo) + attempt_items
```
`scope` và `packageId` là thuộc tính của aggregate `AssessmentAttempt`; luật hết giờ riêng cho `OFFICIAL_PRACTICE` nằm trong aggregate
(`submit`), không nằm trong use case hay controller.

## Related Code Files
Gốc: `services/assessment-service/src/main/java/com/ieltspath/assessment/`
- Create: `domain/vo/AttemptScope.java`
- Modify: `domain/vo/AttemptType.java`, `domain/aggregate/AssessmentAttempt.java`, `domain/entity/AttemptSection.java`,
  `application/command/StartAssessmentAttemptCommand.java`, `application/usecase/{StartAssessmentAttemptUseCase,AttemptCreator,SaveAttemptResponseUseCase,ExpireAssessmentAttemptUseCase}.java`,
  `application/port/ContentPackageProvider.java`, `infrastructure/client/ContentPackageClient.java`,
  `application/result/AssessmentAttemptResult.java` (`ExpireAssessmentAttemptUseCase` dựng record bằng tay: đổi sang `AssessmentAttemptResult.from`),
  `api/dto/request/StartAssessmentAttemptRequest.java`, `api/dto/response/AssessmentAttemptResponse.java`, `api/controller/AssessmentAttemptController.java`,
  `infrastructure/persistence/entity/{AssessmentAttemptJpaEntity,AttemptSectionJpaEntity}.java`,
  `infrastructure/persistence/mapper/AssessmentPersistenceMapper.java`, `infrastructure/persistence/adapter/{AssessmentAttemptRepositoryAdapter,AttemptSectionRepositoryAdapter}.java`
- Tests (gốc `src/test/java/com/ieltspath/assessment/`): `domain/vo/AttemptTypeTest.java`, `domain/aggregate/AssessmentAttemptTest.java`,
  `application/usecase/StartAssessmentAttemptUseCaseTest.java`, `application/usecase/AttemptCreatorTest.java`,
  `infrastructure/client/ContentPackageClientTest.java`, `infrastructure/persistence/PracticeAttemptScopeIntegrationTest.java` (Testcontainers)

## Implementation Steps
1. **Test đỏ**:
   - `AttemptTypeTest`: `PRACTICE_TEST → OFFICIAL_PRACTICE`; `PRACTICE_SET` vẫn bị từ chối.
   - `StartAssessmentAttemptUseCaseTest`: `sectionIds` cho package khác `PRACTICE_TEST` bị từ chối; id lạ, id trùng bị từ chối; chọn một phần cho `PART`, chọn đủ cho `FULL`;
     attempt `OFFICIAL_PRACTICE` có `skill` của section được chọn, loại khác để rỗng;
     `TIMED` cộng đúng tổng giây, thiếu giới hạn → 422; `STANDARD` có `expiresAt` null; `packageId` luôn có.
   - `AttemptCreatorTest`: chỉ section được chọn có `attempt_sections`/`attempt_items`; snapshot có `partNo`, `groups`, `groupId`, **không** có `answerSpec`; section được chọn rỗng bị từ chối nhưng section không chọn rỗng thì không.
   - `AssessmentAttemptTest`: `OFFICIAL_PRACTICE` nộp sau hạn → `SUBMITTED`, không ném lỗi; loại khác nộp sau hạn → `EXPIRED` + `AttemptExpiredException` như cũ.
   - Test lưu đáp án: sau `expiresAt` bị từ chối.
   - `ContentPackageClientTest`: đọc payload mới; payload cũ (thiếu field mới) vẫn đọc được.
   - `PracticeAttemptScopeIntegrationTest`: lưu và đọc lại `packageId`, `scope`, `skill`, `partNo` qua adapter; attempt cũ (chèn bằng SQL không có cột mới) đọc ra `scope = FULL`, `package_id` rỗng.
2. Xác nhận `V8` đã có (chạy test hiện có trên schema mới trước khi code); không viết migration ở phase này.
3. Domain: `AttemptScope`, thêm `packageId`/`scope` vào `AssessmentAttempt.start`, luật nộp sau hạn cho `OFFICIAL_PRACTICE`.
4. Port và client đọc field mới; use case chọn section, tính `expiresAt`; `AttemptCreator` lọc và snapshot.
5. Cập nhật persistence và DTO; sửa `ExpireAssessmentAttemptUseCase` dùng `AssessmentAttemptResult.from`.
6. `mvn -q -pl services/assessment-service -am test`.

## Success Criteria
- [ ] Test mới đỏ rồi xanh; test assessment cũ vẫn xanh (chỉ sửa kỳ vọng theo luật số 6 trong `plan.md`, ghi từng test vào Verification).
- [ ] Attempt `PART` chỉ chứa đúng section được chọn; `scope` và `package_id` đúng.
- [ ] Nộp bài `TIMED` quá hạn của đề luyện vẫn ra kết quả; đáp án lưu sau hạn bị từ chối.
- [ ] Không còn nơi nào dựng `AssessmentAttemptResult` bằng constructor dài ngoài `from`.

## Risk Assessment
- Hiện nộp quá hạn **mất toàn bộ bài** (attempt thành `EXPIRED`, không chấm) và lưu đáp án không kiểm hạn. Bật `TIMED` mà không sửa hai điều này sẽ làm học viên mất bài ở đề 60 phút; vì vậy luật Q7 nằm trong phase này, không để sau.
- `ExpireAssessmentAttemptUseCase` vẫn chuyển `EXPIRED` không chấm. Frontend phải gọi `submit` khi hết giờ chứ không gọi `expire` với đề luyện (ghi trong `docs/fe-main-flow-guide.md`, phase 8).
- Đồng hồ máy chủ quyết định hạn; client chỉ hiển thị. Lệch giờ giữa nơi tính `expiresAt` và `Instant.now()` khi nộp là vài mili giây, chấp nhận.
- Thứ tự deploy: content (phase 2) trước assessment; `ContentPackageClient` phải chịu được payload cũ để rollback an toàn.
- Rollback: revert code; schema do plan data-model (chỉ thêm, nullable hoặc có default).
