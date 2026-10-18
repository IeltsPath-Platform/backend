# Bàn giao examiner queue và course essay policy

Ngày: 2026-10-16
Nhánh: feat/knowledge-tree-assessment-journey

## Thực hiện

- Thêm migration assessment V9 với assignment/reason và bảng so sánh; kiểm nâng cấp V8 với nhiều job AI trùng nguồn và essay bị bỏ trống.
- API EXAMINER/ADMIN: danh sách phân trang, claim/reclaim hơn 24 giờ, đọc essay không kèm điểm LLM. Khóa chung các job của attempt để không tranh chấm/lưu/chốt sau khi mất assignment.
- COURSE_GATE essay tạo job LLM và HUMAN reason COURSE_GATE_POLICY. Dùng cùng map submission đã chốt; nhận xét tạm của chính học viên chỉ chứa chữ. LLM không được tạo kết quả/event course essay, kể cả đường chấm tự động.
- Finalize ghi điểm giám khảo, criteria, mã lỗi, danh tính/thời điểm; hoàn thành job, result và outbox trong một transaction. AI hoàn thành trước/sau vẫn giữ dữ liệu phía còn lại.
- Mở result version mới cho phép chấm lại bởi giám khảo khác: job HUMAN hoàn tất được đưa về QUEUED chưa có assignee.
- Viết chính sách có liên kết code/test và contract grading; sửa cả đoạn contract cũ mâu thuẫn. Event schema không đổi.

## Quy ước triển khai và giới hạn đã ghi trong contract

- Nguồn nhận xét tạm: summary, criteria[].comment và focus[] từ feedback LLM đã hoàn tất; không trả raw JSON hay band.
- Band từng essay do giám khảo khai báo trong chuỗi JSON feedbackSnapshot.band, số 0–9 theo bước 0.5; không suy ra từ item points hoặc overallBand.
- Một claim giữ các essay còn chờ của cùng attempt vì kết quả được finalize theo attempt.
- Câu bỏ trống được lưu thành blank submission có omitted:true, chỉ queue HUMAN; đây là dấu vết omission, không sinh câu trả lời thay học viên.
- Rubric hiện trả null, chờ phần rubric. QA_SAMPLE và cảnh báo 48 giờ chưa có workflow.
- Nếu LLM lỗi/không cấu hình/hết quota hoặc essay bỏ trống: giám khảo vẫn chốt được, phần llm_* để null, không giả lập điểm. Khi cả hai phía chấm thành công, dòng so sánh đủ hai điểm.
- Regrade cập nhật phần examiner_* mới nhất trên cùng submission; lịch sử điểm chính thức vẫn ở result versions và events. Prompt version của dữ liệu LLM trước migration không được đoán.
- Không có thay đổi code Learning/Content, không đổi bảng phân công plan.md. Các thay đổi Learning/config/plan.md hiện có thuộc phiên Claude đang làm.

## Bằng chứng kiểm chứng

- Đỏ trước: controller CUSTOMER trả 405 thay vì 403; course LLM tự tạo result; queue chưa có endpoint. Lỗi vòng đời container giữa hai class đã được sửa bằng context cleanup, rồi queue đỏ lại đúng lỗi endpoint.
- Các test tích hợp chạy PostgreSQL Testcontainers thật; bộ outbox hiện có cũng chạy thật. Không có test skip.
- Gặp artifact IDE báo Unresolved compilation problems; đã dọn riêng assessment-service/target và build lại. Không xóa target Learning.
- Lệnh cuối: mvn -B -ntp -pl services/assessment-service -am test — BUILD SUCCESS.
- git diff --check đã qua trước commit; chỉ có line-ending notices ở các file ngoài phần này đang được Claude sửa.
- Rà soát độc lập: đã sửa trùng nguồn migration, omission ngoài hàng chờ, chấm lại, hai khe tranh chấp submission/assignment; lượt rà soát cuối không còn blocker.
- Commit chỉ dùng danh sách đường dẫn cụ thể; không push. Lượt Maven đã kết thúc, slot build được trả lại.

### Các suite trọng tâm

| Suite | Tests | Fail / Error / Skip |
| --- | ---: | --- |
| GradingJobControllerTest | 2 | 0 / 0 / 0 |
| GradingQueueIntegrationTest | 3 | 0 / 0 / 0 |
| CourseGatePolicyIntegrationTest | 10 | 0 / 0 / 0 |
| ExaminerQueueMigrationIntegrationTest | 1 | 0 / 0 / 0 |

Status: DONE
Summary: Đã hoàn tất hàng chờ chấm mù, course essay bắt buộc giám khảo, nhận xét tạm và so sánh hai đầu; phase 10 đã chuyển completed.
Concerns/Blockers: Không còn blocker. Rubric null và llm_* nullable khi model không chấm được là các giới hạn được mô tả ở trên.
Files changed:
- docs/ai-policy.md
- docs/contracts/assessment-completed-v2.md
- docs/contracts/assessment-grading-v1.md
- plans/261016-0131-knowledge-tree-assessment-journey/phase-10-examiner-queue-and-ai-policy.md
- plans/261016-0131-knowledge-tree-assessment-journey/plans/reports/from-codex-to-claude-phase-10-examiner-queue-report.md
- services/assessment-service/src/main/java/com/ieltspath/assessment/api/controller/GradingController.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/api/controller/GradingJobController.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/api/controller/ProvisionalFeedbackController.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/api/exception/GlobalExceptionHandler.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/port/EssayGradeComparisonStore.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/port/EssayGradingPort.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/port/ExaminerQueueStore.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/port/GateEssayJobStore.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/AutoGradeAttemptService.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/ClaimGradingJobUseCase.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/CreateAssessmentResultUseCase.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/EnqueueGateEssayGradingService.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/ExaminerGradingPolicy.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/FinalizeAssessmentResultUseCase.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GateResultAssembler.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetJobEssayUseCase.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetProvisionalFeedbackUseCase.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GradeGateEssayJobUseCase.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/ListGradingQueueUseCase.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/SaveAssessmentResultDetailsUseCase.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/domain/exception/JobAlreadyClaimedException.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/infrastructure/persistence/adapter/JdbcEssayGradeComparisonStore.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/infrastructure/persistence/adapter/JdbcExaminerQueueStore.java
- services/assessment-service/src/main/java/com/ieltspath/assessment/infrastructure/persistence/adapter/JdbcGateEssayJobStore.java
- services/assessment-service/src/main/resources/db/migration/V9__examiner_queue.sql
- services/assessment-service/src/test/java/com/ieltspath/assessment/api/controller/GradingJobControllerTest.java
- services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/CourseGatePolicyIntegrationTest.java
- services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/CreateAssessmentResultUseCaseTest.java
- services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/EnqueueGateEssayGradingServiceTest.java
- services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/ExaminerIntegrationSupport.java
- services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/ExaminerQueueMigrationIntegrationTest.java
- services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/FinalizeAssessmentResultUseCaseTest.java
- services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/GradeGateEssayJobUseCaseTest.java
- services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/GradingQueueIntegrationTest.java
- services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/SaveAssessmentResultDetailsUseCaseTest.java

Test result:

| Service | Tests | Fail-Error-Skip |
| --- | ---: | --- |
| common-security | 8 | 0-0-0 |
| assessment-service | 268 | 0-0-0 |
| Tổng | 276 | 0-0-0 |

Unresolved questions: Không.
