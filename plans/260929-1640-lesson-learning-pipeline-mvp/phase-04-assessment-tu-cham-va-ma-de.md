---
phase: 4
title: "Assessment: tự chấm, lấy đề từ content, event"
status: pending
priority: P1
dependencies: [1, 3, 5]
effort: "2 ngày"
---

# Phase 4: Assessment: tự chấm, lấy đề từ content, event

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java (plan `261001-1228`). Mọi tên file, lệnh và API Python dưới đây đọc theo [bảng ánh xạ](../260930-2057-mvp-reading-writing-listening-roadmap/python-to-java-mapping.md); luật nghiệp vụ, mã lỗi và test case giữ nguyên.

## Overview

- App chỉ gửi `packageVersionId`. Assessment lấy đề và đáp án từ content, và tự quyết `attemptType` theo loại gói.
- Nộp bài thì tự chấm câu khách quan trong cùng request, ghi result COMPLETED và outbox cùng transaction.
- Học viên có DTO result riêng: điểm, đúng/sai từng câu, lời giải khi đạt.
- Event thêm `package_version_id`, `learning_goal_id = null`.
- Sửa bug submit hết hạn.

**Không migration mới:** dùng các cột đã có.

## Requirements

- **Tạo attempt** `{ packageVersionId, mode, channel }`:
  - Gọi `GET /internal/learning-content/package-versions/{id}` **trước** khi mở transaction, để không giữ connection DB trong lúc gọi HTTP. Forward bearer và `X-Correlation-Id`.
  - Hiện `StartAssessmentAttemptUseCase.execute` mang `@Transactional` (`:32`), và gọi hàm khác trong cùng bean thì proxy không mở transaction mới. Vì vậy: bỏ `@Transactional` khỏi `execute` (gọi content), phần ghi DB chuyển sang bean riêng `AttemptCreator` có `@Transactional`.
  - `attemptType` suy từ `packageType`: `TOPIC_TEST` → `TOPIC_GATE`, `MOCK_TEST` → `MOCK`, `PLACEMENT_TEST` → `PLACEMENT`, `QUIZ` → `QUIZ`. `PRACTICE_SET` và `LESSON` → 422, vì bài ôn chấm ở ai-learning và không cho làm nháp gói ôn qua assessment.
  - `expiresAt` lấy từ `rules` của package version (không có thì null). Không nhận từ client.
  - Mỗi section lưu `section_snapshot` = JSON `{"title", "skill", "instructions", "passage"}` lấy từ section của payload
    content (thiếu `passage` thì bỏ khóa). `AttemptStructureResponse.Section.snapshot` vẫn là chuỗi JSON này, không đổi schema
    ở plan này; plan Listening thêm `audio`, `solution` và đổi sang DTO allowlist.
  - Mỗi item lưu `question_snapshot` = stem + options (**không** đáp án) và `answer_snapshot` = `{answerSpec, explanation, maxScore}`. Cột đã có (`V1:36`); sau phase 2 cột này không còn trả cho học viên. KP lấy từ payload.
- **Submit:**
  - Nếu **mọi** item có `answerSpec` chấm được: lấy câu trả lời từ `attempt_responses.payload` theo `answer-spec-v1`, rồi chấm; câu bỏ trống được 0.
  - Ghi result v1 COMPLETED và `item_results` (`score`, `max_score` là cột đã có ở `V4:18`; `feedback_snapshot = '{}'`), rồi ghi outbox qua helper dùng chung với chấm tay.
  - Có item không chấm được: giữ luồng chấm tay như cũ.
- **Bug expire:** submit sau `expiresAt` thì attempt chuyển EXPIRED và trạng thái này **được lưu** (không rollback), trả 409 có mã rõ ràng. Hiện `AssessmentAttempt.java:73-76` gọi `expire()` rồi ném lỗi, nên cả transaction rollback.
- **Result cho học viên** (`AssessmentResultController.get` dùng DTO mới `LearnerAssessmentResultResponse`):
  - `score`, `maxScore`, `percent` tính từ `item_results`;
  - `items[{attemptItemId, questionVersionId, correct}]`;
  - `solutions[]` khi `percent ≥ 70`;
  - lấy version **COMPLETED mới nhất**, bỏ qua bản DRAFT đang chấm lại.
  - DTO của người chấm (`AssessmentResultResponse`, `GradingController`) **không đổi**.
- **Event:** thêm `package_version_id`. `learning_goal_id` luôn null; xóa tra goal. Bỏ hai chỗ chặn event khi không có goal: `FinalizeAssessmentResultUseCase.java:90-94` và `AssessmentCompletedEventFactory.java:29`.

## Architecture

- Port mới `application/port/ContentPackageProvider`, adapter `infrastructure/client/ContentPackageClient` (timeout 2s/5s; lỗi thì 503).
- Bộ chấm `domain/service/AnswerSpecGrader`: Java thuần, không Spring, test bằng file vectors dùng chung.
- Helper "hoàn tất result + ghi outbox", tách từ `FinalizeAssessmentResultUseCase.java:88-104`.
- **Xóa:**
  - assessment: `KnowledgeMappingProvider`, `ContentKnowledgeMappingClient`, `LearningGoalProvider`, `UserLearningGoalClient` (goal không còn ai dùng; user-service lỗi sẽ không còn chặn tạo attempt);
  - content: `InternalAssessmentContentController`, `GetQuestionKnowledgePointMappingsUseCase` và test (người gọi duy nhất là client vừa xóa).

## Related Code Files

- Create: `application/port/ContentPackageProvider.java`, `infrastructure/client/ContentPackageClient.java`, `domain/service/AnswerSpecGrader.java`, `application/usecase/AutoGradeAttemptService.java`, helper hoàn tất result, `api/dto/response/LearnerAssessmentResultResponse.java`, result tương ứng
- Modify:
  - `api/dto/request/StartAssessmentAttemptRequest.java`
  - `api/controller/AssessmentAttemptController.java:45-77`
  - `application/usecase/StartAssessmentAttemptUseCase.java:24,32-59`
  - `application/usecase/SubmitAssessmentAttemptUseCase.java`
  - `domain/aggregate/AssessmentAttempt.java:68-89`
  - `application/usecase/FinalizeAssessmentResultUseCase.java`
  - `application/event/AssessmentCompletedV2.java`, `application/event/AssessmentCompletedEventFactory.java`
  - `application/usecase/GetAssessmentResultUseCase.java`
  - `api/controller/AssessmentResultController.java:40-43`
  - `api/exception/GlobalExceptionHandler.java:24`: hiện `InvalidAssessmentStateException` → 400. Thêm exception riêng cho attempt hết hạn → 409 kèm mã, gói không cho làm → 422, content lỗi → 503.
  - Create thêm: `application/usecase/AttemptCreator.java` (phần ghi `@Transactional` tách khỏi `execute`); query lấy result COMPLETED mới nhất cho `GetAssessmentResultUseCase` (hiện chỉ có `findLatestByAttemptId`).
- Delete: `application/port/{KnowledgeMappingProvider,LearningGoalProvider}.java`, `infrastructure/client/{ContentKnowledgeMappingClient,UserLearningGoalClient}.java`; content: `api/controller/InternalAssessmentContentController.java`, `application/usecase/GetQuestionKnowledgePointMappingsUseCase.java` + test + `api/dto/internal/KnowledgePointMappingResponse`. **Không** xóa `QuestionKnowledgePointResponse/Result`: `GetQuestionDetailUseCase` còn dùng.
- Tests:
  - sửa: `StartAssessmentAttemptUseCaseTest` (`:32-33,45`), `FinalizeAssessmentResultUseCaseTest` (test "không có goal" ở `:168` đảo kỳ vọng), `AssessmentOutboxIntegrationTest`, `AssessmentAttemptTest`;
  - giữ nguyên: `GradingControllerTest` (`:159,200`); phần người chấm của `CreateAssessmentResultUseCaseTest` (test `execute` của học viên đã xóa ở phase 2).
<!-- Updated: Validation Session 1 - khớp phase 2 về CreateAssessmentResultUseCaseTest; tách transaction khi tạo attempt; mapping lỗi -->


## Implementation Steps

**Tests Before:**
1. Chạy toàn bộ test assessment, ghi baseline.
2. Khóa luồng chấm tay: `GradingController` create → save → finalize ra COMPLETED và outbox; response của người chấm giữ nguyên key.

**Tests After** (viết trước code):
3. `AnswerSpecGraderTest`: mọi vector trong `docs/contracts/answer-spec-v1-vectors.json` (đọc theo đường dẫn tương đối từ root repo).
4. Start:
   - gói `TOPIC_TEST` → `TOPIC_GATE`, dù client gửi `attemptType` khác;
   - gói `PRACTICE_SET` → 422;
   - `question_snapshot` không chứa đáp án; `expiresAt` lấy từ `rules`;
   - content lỗi → 503, không insert gì;
   - không còn gọi user-service.
5. Submit tự chấm:
   - 3/4 → result v1 COMPLETED, 4 `item_results` có `max_score`, đúng 1 outbox;
   - câu bỏ trống = 0;
   - submit lần hai idempotent, không thêm outbox.
6. Có item spec `{}` → giữ SUBMITTED, không result, không outbox.
7. Submit sau `expiresAt` → attempt EXPIRED **được lưu**, response 409.
8. Result học viên:
   - 75% có `solutions`, 50% không có;
   - đang có bản DRAFT v2 thì vẫn trả v1 COMPLETED;
   - DTO của người chấm không đổi.
9. Outbox integration (Testcontainers): event có `package_version_id`, `learning_goal_id: null`.
10. Request cũ có `sections`/`attemptType`: bị bỏ qua, attempt tạo từ payload content.

**Implement:** grader → port và client → start (gọi content ngoài transaction) → helper hoàn tất → tự chấm khi submit → sửa expire → DTO result học viên → event → xóa code chết.

**Regression Gate:**
```powershell
mvn -q -pl services/assessment-service -am test
mvn -q -pl services/content-service -am test
```

## Success Criteria

- [ ] Test bước 2–10 pass.
- [ ] Học viên không làm nháp được mã đề hay gói ôn bằng loại attempt khác; không đọc được đáp án trước khi đạt.
- [ ] Event khớp `assessment-completed-v2.md`; không migration mới.

## Risk Assessment

- **Triển khai trước consumer chịu được null:** phase 5 phải chạy trước (adapter nhận `learning_goal_id` null). Ghi trong `plan.md`.
- **Chấm hai nơi lệch nhau** (Java và Python): cùng file vectors, gồm cả cách lấy câu trả lời từ payload.
- **Mã đề cũ dùng lại được qua assessment:** ai-learning chỉ tính PASSED cho lần giao chưa dùng (phase 7), nên làm lại đề cũ không mở được topic.
