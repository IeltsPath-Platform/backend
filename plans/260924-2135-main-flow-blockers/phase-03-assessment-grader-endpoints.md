---
phase: 3
title: "Assessment grader endpoints"
status: completed
priority: P1
dependencies: []
effort: "~4.5h"
---

# Phase 3: Assessment grader endpoints

## Overview
Mở HTTP cho người chấm (`EXAMINER`/`ADMIN`) để hoàn thành vòng đời kết quả: mở version (chấm lại) → lưu chi tiết chấm → finalize. Đây là thứ đầu tiên thực sự gọi `FinalizeAssessmentResultUseCase` và phát `AssessmentCompleted.v2`.

## Context
- Tầng application đã có đủ vòng đời, nhưng chưa có trigger:
  - `CreateAssessmentResultUseCase`: tạo DRAFT, theo learner sở hữu.
  - `SaveAssessmentResultDetailsUseCase`: lưu chi tiết theo learner sở hữu, **không có controller nào gọi**.
  - `FinalizeAssessmentResultUseCase`: làm việc theo `resultId`.
- Role có sẵn trong `CanonicalRoles`: `ADMIN`, `CUSTOMER`, `CONTENT_AUTHOR`, `EXAMINER`, `SALES_STAFF`. JWT được map thành `ROLE_<role>`, nên convention là `@PreAuthorize("hasAnyRole(...)")` (xem `AdminAccessController`, `TopicController`).
- Gateway đã route `/api/assessments/**` tới `ASSESSMENT-SERVICE`, không cần route mới.
- `GlobalExceptionHandler` đã map `AccessDeniedException` sang 403.

## Requirements
- Functional:
  - Toàn bộ endpoint nằm dưới `/api/assessments/grading`, dùng `@PreAuthorize("hasAnyRole('EXAMINER','ADMIN')")` ở cấp class.
  - `POST /api/assessments/grading/attempts/{attemptId}/results`:
    - Body `{overallBand?}`. Mở version kế tiếp ở trạng thái DRAFT.
    - 201 kèm `AssessmentResultResponse`.
    - Trả 400 khi version mới nhất vẫn đang chấm, hoặc khi attempt chưa submit.
  - `PUT /api/assessments/grading/results/{resultId}/details`:
    - Body gồm `overallBand` (nullable), `skillScores[]`, `itemResults[]` (`attemptItemId`, `score`, `maxScore`, `correct`, `durationMilliseconds`, `feedbackSnapshot`), `errors[]`, `knowledgeJudgments[]` (`attemptItemId`, `knowledgePointId`, `judgment`). Trả 204.
    - `overallBand` luôn ghi đè band hiện có của result, kể cả khi gửi `null`. Band của result đã finalize chỉ do grader quyết định. Validate `overallBand` dùng lại đúng ràng buộc của request tạo result hiện có.
    - Chỉ nhận version mới nhất đang ở DRAFT hoặc PROCESSING.
    - Dùng lại toàn bộ validate hiện có: item thuộc attempt, `score ≤ maxScore`, judgment chỉ cho KP có trong snapshot, không cho sửa sau khi COMPLETED.
  - `POST /api/assessments/grading/results/{resultId}/finalize`:
    - 200 kèm `AssessmentResultResponse`.
    - Gọi lại được (idempotent): kết quả đã COMPLETED thì trả nguyên, không sinh event mới.
  - Endpoint learner `POST/GET /api/assessments/attempts/{id}/result` giữ nguyên contract (đã chốt).
    - Learner vẫn tạo được DRAFT với band tự khai. Khi đó grader không mở được version mới (version mới nhất còn đang chấm), nên lưu chi tiết và finalize trên chính DRAFT này.
    - Vì band luôn bị ghi đè khi grader lưu chi tiết, band learner tự khai không bao giờ nằm trong result đã finalize.
- Non-functional:
  - Không đổi hành vi các use case hiện có với luồng learner.
  - Không lặp logic validate: dùng chung một hàm lõi.

## Architecture
```text
GradingController (api)  ── @PreAuthorize EXAMINER|ADMIN
  ├─ CreateAssessmentResultUseCase.executeForGrader(attemptId, overallBand)
  │     findById(attempt) + cùng quy tắc version/DRAFT với luồng learner (hàm lõi chung)
  ├─ SaveAssessmentResultDetailsUseCase.executeForGrader(resultId, GradingDetails)
  │     findForUpdateById(result) → phải là version mới nhất và còn đang chấm → hàm lõi saveDetails(attempt, result, details)
  │     → result.withGradedBand(overallBand) → lưu
  └─ FinalizeAssessmentResultUseCase.execute(FinalizeAssessmentResultCommand)   (đã có)
```
- Refactor `SaveAssessmentResultDetailsUseCase`: tách lõi `saveDetails(attempt, result, details)`. `execute(...)` (theo learner sở hữu) và `executeForGrader(...)` cùng gọi lõi này.
- `AssessmentResult` là record bất biến. Thêm `withGradedBand(Double)` theo đúng kiểu `complete(Instant)`: chỉ cho phép khi `isGradable()`, trả về bản ghi mới. Chỉ `executeForGrader` gọi method này; luồng learner `execute` giữ nguyên.
- DTO request (bean validation) nằm trong `api/dto/request`, map sang command trong controller theo đúng kiểu `AssessmentAttemptController`.

## Related Code Files
- Create: `services/assessment-service/src/main/java/com/group01/assessment/api/controller/GradingController.java`
- Create: `.../api/dto/request/OpenResultVersionRequest.java`, `.../api/dto/request/SaveGradingDetailsRequest.java`
- Create: `.../application/command/SaveGradingDetailsCommand.java` (hoặc record `GradingDetails` dùng chung)
- Modify: `.../application/usecase/CreateAssessmentResultUseCase.java` (thêm `executeForGrader`, hàm lõi chung)
- Modify: `.../application/usecase/SaveAssessmentResultDetailsUseCase.java` (tách lõi; thêm `executeForGrader`, ghi band của grader)
- Modify: `.../domain/entity/AssessmentResult.java` (`withGradedBand`)
- Create: `src/test/java/.../api/controller/GradingControllerTest.java` (slice test có bật method security)
- Modify: `src/test/java/.../application/usecase/CreateAssessmentResultUseCaseTest.java`, `SaveAssessmentResultDetailsUseCaseTest.java`
- Modify: `src/test/java/.../infrastructure/messaging/AssessmentOutboxIntegrationTest.java` (chuỗi mở version → lưu chi tiết → finalize → relay → message)
- Modify: `services/assessment-service/README.md` (bảng endpoint grader), `docs/contracts/assessment-completed-v2.md` (event được phát khi grader finalize)

## Implementation Steps
### Tests Before (khóa hành vi đang giữ)
1. Characterization test cho luồng learner trước khi refactor:
   - `SaveAssessmentResultDetailsUseCase.execute` vẫn đòi attempt thuộc đúng learner (learner khác → `AssessmentNotFoundException`).
   - Các test hiện có (từ chối kết quả đã finalize, `score > maxScore`, judgment ngoài snapshot) giữ nguyên.
2. Viết test mới, phải **fail** trước khi code:
   - `GradingControllerTest`: `@WebMvcTest` + test config `@EnableMethodSecurity`; `@WithMockUser(roles=...)`; `.with(csrf())` hoặc tắt CSRF trong test config.
     - `CUSTOMER` → 403 ở cả 3 endpoint.
     - `EXAMINER` và `ADMIN` → 201/204/200.
     - Body sai → 400.
   - Use case: `executeForGrader` của create và save hoạt động không cần `userId`; kết quả không phải version mới nhất → 400.
   - Band (trong `SaveAssessmentResultDetailsUseCaseTest`):
     - Learner tạo DRAFT với band 9.0, grader lưu chi tiết với band 6.5, rồi finalize: result COMPLETED có band 6.5.
     - Grader gửi `null` thì band của result là `null`.
     - Luồng learner `execute` không đổi band.
### Refactor
3. Tách hàm lõi ở hai use case; thêm các entry `executeForGrader`.
4. Viết `GradingController` và DTO; map sang command.
### Tests After
5. Chạy lại toàn bộ test use case, controller và mapper.
6. Thêm vào `AssessmentOutboxIntegrationTest` (Testcontainers, chạy thật ở pha 4) test chuỗi `executeForGrader` (create) → `executeForGrader` (save) → finalize → relay → message trên queue test.
### Regression Gate
7. `mvn -o -pl services/assessment-service -am test`: 0 fail. Không có Docker thì các test Testcontainers skip; pha 4 bắt buộc chạy.
8. `git diff --check`, `graphify update .`

## Success Criteria
- [ ] CUSTOMER bị 403; EXAMINER và ADMIN đi được hết vòng đời qua HTTP.
- [ ] Finalize qua HTTP tạo đúng 1 dòng outbox; gọi lại không tạo thêm.
- [ ] Luồng learner cũ không đổi hành vi (có characterization test chứng minh).
- [ ] Result đã finalize luôn mang band do grader gửi, không bao giờ mang band learner tự khai.
- [ ] README có bảng endpoint grader.

## Risk Assessment
- **Mọi EXAMINER chấm được mọi kết quả**, vì chưa có mô hình phân công (`human_reviews`). Đã chốt chấp nhận trong đợt này; ghi trong README.
- **CSRF trong slice test**: app thật là API stateless dùng JWT; test chỉ cần tránh 403 giả do CSRF.
- **Hai lối vào một use case** có thể lệch nhau theo thời gian: một hàm lõi duy nhất cộng test cho cả hai lối.

## Security Considerations
- Phân quyền theo role ở cấp class controller. Không có đường nào cho learner tự finalize.
- Grader không được đổi `learning_goal_id` hay snapshot KP (không có field nào cho việc đó).
- Log không chứa token hay Authorization header (giữ convention hiện có).
