---
phase: 4
title: "Assessment lịch sử, tiến độ, lượt làm"
status: pending
priority: P1
dependencies: [3]
---

# Phase 4: Assessment lịch sử, tiến độ, lượt làm

## Overview
Dữ liệu của người học cho thư viện đề (D4, D6): học viên đã làm part/đề nào (để frontend loại trừ hoặc chỉ lấy khi gọi catalog), số lượt làm của từng đề/part,
và lịch sử làm bài có phân trang. Assessment không biết tiêu đề đề (nằm ở content); frontend lấy tiêu đề bằng catalog với `only`.

## Requirements
- Functional (controller mới `PracticeLibraryController`, `@RequestMapping("/api/assessments/practice")`; quyền như `AssessmentAttemptController`; danh tính qua `CurrentUserProvider`, không tin header `X-User-*`):
  - `GET /progress?skill=&scope=`: các khóa `{packageId, partNo|null}` mà học viên đã **nộp** ở attempt `OFFICIAL_PRACTICE`, kèm
    `attempts`, `lastSubmittedAt`, `bestPercent` (null nếu chưa có kết quả hoàn tất). Tối đa 500 khóa, mới nhất trước.
    - `scope=FULL`: khóa `(packageId, null)` từ attempt `scope = FULL`.
    - `scope=PART`: khóa `(packageId, partNo)` cho mọi part mà học viên đã nộp, kể cả khi nằm trong attempt `FULL`.
    - Attempt `IN_PROGRESS`, `EXPIRED`, `CANCELLED` không tính là đã làm. Đề ra version mới vẫn tính (theo `package_id`, D3).
  - `POST /stats` `{packageIds: [≤100]}`: mỗi gói `{packageId, attemptCount, parts: [{partNo, attemptCount}]}` trên **mọi học viên**, chỉ attempt `OFFICIAL_PRACTICE` đã nộp.
    `attemptCount` đếm attempt `FULL`; `parts[].attemptCount` đếm mọi attempt (kể cả `FULL`) có part đó. Không trả dữ liệu cá nhân.
  - `GET /history?skill=&page=&size=`: attempt `OFFICIAL_PRACTICE` của học viên, mới nhất trước, mỗi dòng
    `{attemptId, packageId, packageVersionId, scope, partNos, status, startedAt, submittedAt, percent}`; `page` ≥ 0, `size` 1-50 (mặc định 12); trả `totalElements`.
  - `skill` bắt buộc ở `/progress` và tùy chọn ở `/history`; `READING`/`LISTENING`, giá trị khác trả danh sách rỗng.
- Non-functional: mỗi endpoint một số query cố định, lọc và phân trang ở DB, không N+1 (`AGENTS.md` §3.7); không `findByUserId` rồi lọc trong bộ nhớ; không lộ dữ liệu của học viên khác.
  `percent` lấy từ phiên bản kết quả mới nhất của attempt (`result_version` lớn nhất, `status = COMPLETED`).

## Architecture
```
api/controller/PracticeLibraryController ── GetPracticeProgressUseCase / GetPracticeStatsUseCase / ListPracticeHistoryUseCase
                                                        │
                                       application/port/PracticeAttemptReader (read model)
                                                        │
                        infrastructure/persistence/adapter/JdbcPracticeAttemptReader (NamedParameterJdbcTemplate)
```
Đọc theo read model riêng như `JdbcLearningContentReader` của content, **không** thêm phương thức phân trang vào `domain/repository/AssessmentAttemptRepository`
(giữ repository cho aggregate, không trộn projection).

Phase 3 cần thêm một cột để lọc theo skill: attempt không lưu skill (chỉ có trong snapshot JSON của section). Đề luyện là một skill (D8) nên
`assessment_attempts.skill` được ghi khi bắt đầu attempt `OFFICIAL_PRACTICE` (xem phase 3).

## Related Code Files
Gốc: `services/assessment-service/src/main/java/com/ieltspath/assessment/`
- Create: `api/controller/PracticeLibraryController.java`, `api/dto/request/PracticeStatsRequest.java`,
  `api/dto/response/{PracticeProgressResponse,PracticeStatsResponse,PracticeHistoryPageResponse}.java`,
  `application/usecase/{GetPracticeProgressUseCase,GetPracticeStatsUseCase,ListPracticeHistoryUseCase}.java`,
  `application/port/PracticeAttemptReader.java`, `application/result/{PracticeProgressResult,PracticeStatsResult,PracticeHistoryResult}.java`,
  `infrastructure/persistence/adapter/JdbcPracticeAttemptReader.java`
- Modify: `infra/config-server/config-repo/assessment-service.yaml` (`springdoc.paths-to-match` thêm `/api/assessments/practice/**` nếu file đang liệt kê route)
- Tests (gốc `src/test/java/com/ieltspath/assessment/`): `api/controller/PracticeLibraryControllerTest.java` (WebMvcTest: quyền, 422),
  `application/usecase/PracticeLibraryUseCasesTest.java`, `infrastructure/persistence/JdbcPracticeAttemptReaderTest.java` (Testcontainers)

## Implementation Steps
1. **Test đỏ** (`JdbcPracticeAttemptReaderTest`, tự `INSERT` attempt, section, item, result bằng `JdbcTemplate`):
   - progress `PART`: attempt `FULL` ba part làm xong cho ra ba khóa; attempt `PART` part 2 chỉ ra part 2; `IN_PROGRESS`/`EXPIRED` không ra;
     hai version khác nhau của cùng `package_id` gộp một khóa; học viên khác không lẫn vào;
   - progress `FULL`: chỉ attempt `scope = FULL`;
   - `bestPercent`: lấy `result_version` mới nhất, bỏ kết quả chưa `COMPLETED`;
   - stats: đếm mọi học viên, `attemptCount` chỉ `FULL`, part đếm cả attempt `FULL`; gói chưa có lượt nào vẫn có dòng với số 0;
   - history: phân trang, thứ tự, `partNos` đủ, `size` > 50 bị từ chối.
   `PracticeLibraryControllerTest`: thiếu `skill` → 422; `packageIds` > 100 → 422; chưa đăng nhập → 401; role ngoài tập cho phép → 403.
2. Viết port, reader JDBC, use case, controller, DTO.
3. Cập nhật `springdoc.paths-to-match` nếu cần.
4. `mvn -q -pl services/assessment-service -am test`.

## Success Criteria
- [ ] Test mới đỏ rồi xanh; test assessment cũ vẫn xanh.
- [ ] Học viên A không thấy tiến độ hay lịch sử của học viên B.
- [ ] Số query mỗi endpoint không đổi khi dữ liệu tăng (ghi cách kiểm trong Verification).
- [ ] Với dữ liệu ví dụ của `plan.md`: làm part 1 của `R-A`, publish version 2, `progress` vẫn có `(R-A, 1)` và không có `(R-A, 2)`.

## Risk Assessment
- Giới hạn 500 khóa của `/progress`: học viên làm nhiều hơn sẽ không thấy hết ở tab "chưa làm" (một số đề đã làm hiện lại). Chấp nhận giai đoạn đầu; nếu thực tế vượt, đổi cách ghép (bản chiếu trạng thái đồng bộ từ event), ngoài phạm vi.
- Truy vấn `bestPercent` per part nối `attempt_items → attempt_sections → item_results` của version kết quả mới nhất; cần index hỗ trợ có sẵn (`idx_item_results_attempt_item`, `idx_attempt_items_section_order`).
  Nếu `EXPLAIN` cho thấy chậm trên dữ liệu thử (≥ 100.000 attempt giả lập), ghi vào Verification và đề xuất index thay vì tự thêm.
- `/stats` công khai số lượt của mọi học viên cho học viên khác: chỉ số đếm, không định danh. Đã chấp nhận (D6).
- Rollback: chỉ thêm endpoint và read model; gỡ bằng revert, không đổi dữ liệu.
