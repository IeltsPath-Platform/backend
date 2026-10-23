# Phase 14: API đọc bổ sung cho hàng việc R&D, đánh giá giám khảo, đánh giá AI

Người làm: **Claude Code**. Nhánh: `feat/review-work-queue-api` (backend), tạo từ `origin/main` sau khi phase 01 đã merge.
Trạng thái: Done (2026-10-23, chưa commit/merge; xem mục "Kết quả")

Repo: `backend`, service `assessment-service`, `learning-service` (và `content-service` nếu cần endpoint internal tra cứu theo lô). Phụ thuộc: không về mặt chức năng; làm sau 01 để không chạy song song hai nhánh backend.

## Mục tiêu

Ba tính năng board giao cho `CONTENT_AUTHOR` đã có API ghi và tính toán, nhưng thiếu vài API **đọc** nên màn hình (phase 15) không dùng được trọn vẹn. Phase này chỉ thêm API đọc và trường mới **thêm vào** response (không đổi, không bỏ trường cũ). Không đổi luật nghiệp vụ.

| Tính năng | Đã có | Còn thiếu (đã kiểm code 2026-10-23) |
|---|---|---|
| Hàng việc R&D (`CONTENT_GAP`, `QUESTION_TOO_EASY`, `QUESTION_TOO_HARD`, `EDGE_SUSPECT`) | `GET`/`PATCH /api/learning/admin/work-items` (`StaffAdminController`), `WorkItemSync` | Item chỉ có `subjectRef` (UUID) và `detail` JSON; không có tên KP/câu hỏi để hiện |
| Đánh giá giám khảo (QA chấm lại mẫu) | `GET /api/assessments/qa-reviews/queue`, `POST …/{jobId}/claim`, `GET …/{jobId}/essay`, `POST /api/assessments/qa-reviews`, `GET /api/assessments/admin/examiner-quality` | Hàng chờ chỉ trả bài `QUEUED`: nhận xong rời trang là không tìm lại được bài mình đang chấm. Bảng chất lượng chỉ có `examinerId`, assessment chưa có client gọi user-service |
| Đánh giá AI (gold set) | `POST /api/assessments/admin/ai-gold-essays`, `…/import-comparisons`, `POST/GET …/ai-evaluations`, `POST …/ai-prompt-activations` (`AiEvaluationController`) | Không có danh sách bài chuẩn, danh sách lượt đánh giá, hay bản prompt đang kích hoạt mỗi Task (chỉ có route internal) |

## Đọc trước

- `docs/contracts/admin-area-v1.md` (mục Examiner quality review), `docs/contracts/assessment-grading-v1.md` (mục AI rubric evaluation and activation), `docs/contracts/learning-staff-area-v1.md` (work items).
- `services/assessment-service/.../api/controller/{ExaminerQaController, ExaminerQualityController, AiEvaluationController}.java`, `application/service/{ExaminerQaService, AiEvaluationService}.java`, `application/port/ExaminerQueueStore.java`.
- `services/learning-service/.../application/service/WorkItemSync.java`, `api/controller/StaffAdminController.java`, `infrastructure/persistence/JdbcStaffInsightRepository.java` (bảng `content_gap_signals`), client user-service và content hiện có của learning (mẫu cho assessment).

## Yêu cầu

Mọi route mới chỉ `CONTENT_AUTHOR`. Cập nhật contract tương ứng trong cùng commit. Danh sách mới đều phân trang (`page`, `size` ≤ 100), lọc và sắp ở database.

### A. Hàng việc R&D (learning)

- `GET /api/learning/admin/work-items` thêm trường `subject` cho mỗi item (giữ nguyên `subjectRef`, `detail`):
  - `CONTENT_GAP`: `{ "type": "KNOWLEDGE_POINT", "id", "code", "name", "occurrences" }` (`occurrences` lấy từ `content_gap_signals`).
  - `QUESTION_TOO_EASY` / `QUESTION_TOO_HARD`: `{ "type": "QUESTION", "questionId", "questionVersionId", "stemPreview" }` (`stemPreview` tối đa 120 ký tự).
  - `EDGE_SUSPECT`: `{ "type": "EDGE", "from": { "id", "code", "name" }, "to": { … } }`.
  - Loại của hàng EXAMINER: `subject` null (ngoài phạm vi plan).
- Tra tên **theo lô**: tối đa một call tới content cho KP và một call cho câu hỏi mỗi request (dùng endpoint internal có sẵn nếu đủ, ví dụ mở rộng `POST /internal/learning-content/questions/difficulties`, hoặc thêm một endpoint internal batch trong content-service). Không gọi trong vòng lặp. Content lỗi hoặc timeout → `subject` null, danh sách vẫn trả `200`.
- Thêm lọc tùy chọn `kind=` (cộng thêm vào `queue`, `status` đang có).

### B. Đánh giá giám khảo (assessment)

- `GET /api/assessments/qa-reviews/mine?page=&size=`: bài `QA_SAMPLE` người gọi đã nhận và chưa nộp đánh giá, cùng shape `QueueEntry` như hàng chờ (kể cả `overdue`).
- `GET /api/assessments/admin/examiner-quality` thêm `examinerName` (nullable) cho mỗi dòng.
  - Assessment thêm port `UserDirectory` (application) + adapter HTTP gọi `POST /internal/users/display-names` của user-service, kèm bearer của request và `X-Correlation-Id`, theo đúng mẫu learning → user-service. Một call cho cả bảng; lỗi/timeout → `examinerName` null, bảng vẫn trả.
  - Cấu hình (cần cho tính năng, ghi rõ trong PR): `config-repo/assessment-service.yaml` `assessment.user.base-url: ${USER_SERVICE_URL:http://localhost:8085}`; `docker-compose.yml` service `assessment-service` đặt `USER_SERVICE_URL: http://user-service:8085`; README của assessment ghi biến mới.

### C. Đánh giá AI (assessment)

- `GET /api/assessments/admin/ai-gold-essays?task=&page=&size=`: `{ content: [{ id, task, examinerBand, source, essayPreview, createdAt }], page, size, totalElements }`. `source` là `MANUAL` hoặc bài nhập từ so sánh; `essayPreview` tối đa 200 ký tự. Sắp `createdAt` giảm dần.
- `GET /api/assessments/admin/ai-evaluations?task=&page=&size=`: danh sách lượt chạy, mỗi dòng `{ id, task, model, promptVersion, status, n, withinHalfBand, mae, qwk, passed, startedAt, finishedAt }`, mới nhất trước. Chi tiết từng lượt vẫn dùng `GET …/ai-evaluations/{id}` đã có.
- `GET /api/assessments/admin/ai-prompt-activations`: `[{ "task": "TASK_1", "promptVersion": "…|null", "runId": "…|null", "activatedAt": "…|null" }, { "task": "TASK_2", … }]`. `promptVersion` null nghĩa là Task đó chưa từng kích hoạt và đang chạy chế độ khởi động (chấm bằng rubric `ACTIVE` của Content).
- `GET …/ai-evaluations/{id}` trả thêm `thresholds: { withinHalfBand, qwk }` (giá trị cấu hình đang dùng) để màn hình không phải ghi cứng 0.80 / 0.60.
- Không log nội dung essay hay prompt.

## Test

- `@WebMvcTest`: mọi route mới trả `403` cho `CUSTOMER`, `EXAMINER`, `ADMIN`; `200` cho `CONTENT_AUTHOR`.
- Learning: work items có `subject` đúng cho từng loại; content giả lập lỗi → `subject` null và vẫn `200`; kiểm chỉ một call tra KP và một call tra câu hỏi cho một trang (`MockRestServiceServer` đếm request).
- Assessment QA: nhận một bài → có trong `mine`, không còn trong `queue`; nộp đánh giá → không còn trong `mine`. Bảng chất lượng có `examinerName` khi user-service trả tên; user-service lỗi → null, vẫn `200`.
- Assessment AI (Testcontainers): danh sách bài chuẩn lọc theo task và phân trang; danh sách lượt chạy mới nhất trước; activations trả null khi chưa kích hoạt và đúng `promptVersion` sau khi kích hoạt một lượt đạt.
- Context assessment khởi động khi thiếu `USER_SERVICE_URL` (dùng mặc định).

## Hoàn thành khi

`mvn -q -pl services/assessment-service,services/learning-service -am test` xanh (thêm `services/content-service` nếu có sửa); ba contract cập nhật; không trường cũ nào đổi nghĩa.

## Kết quả

**2026-10-23 (Claude Code), chưa commit.** Trạng thái: Done (chờ merge).

- A (learning): `GET /admin/work-items` thêm `subject` và lọc `kind`. Tên KP/câu hỏi lấy bằng đúng một call tới endpoint internal mới của content `POST /internal/learning-content/work-item-subjects` (tối đa 500 id mỗi loại); content lỗi → `subject` null, vẫn 200. `occurrences` đọc từ `content_gap_signals`. `PATCH` giữ nguyên (không có `subject`).
- B (assessment): `GET /qa-reviews/mine` (bài đã nhận, chưa nộp đánh giá; phân trang). `examiner-quality` thêm `examinerName` qua port `UserDirectory` + `RestUserDirectory` (gọi user-service theo lô 50, bearer của người gọi; lỗi → null). Cấu hình `assessment.user.base-url` (`USER_SERVICE_URL`), compose đặt `http://user-service:8085`.
- C (assessment): `GET /admin/ai-gold-essays`, `/ai-evaluations`, `/ai-prompt-activations`; `GET …/ai-evaluations/{id}` thêm `thresholds`. Controller riêng `AiEvaluationReadController` để không đổi controller ghi.
- Contract cập nhật: `admin-area-v1`, `assessment-grading-v1`, `learning-staff-area-v1`, `learning-content-internal-v1`.
- Test: learning 902, content 725, assessment 381+ (mới: `ReviewReadsIntegrationTest`, `ReviewReadsAuthorizationTest`, `RestUserDirectoryTest`, `ExaminerQualityNamesTest`, `WorkItemSubjectResolverTest`, `JdbcWorkItemSubjectsTest`, mở rộng `ExaminerQaIntegrationTest` và `StaffAdminControllerTest`). Mọi suite xanh.
- Không làm: phân trang cho `work-items` (route cũ không phân trang, giữ nguyên để không đổi hợp đồng); ghi tên giám khảo vào `QueueEntry`.
