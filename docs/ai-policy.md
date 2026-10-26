# Chính sách AI trong đánh giá

Cập nhật: 2026-10-22 (Learning gộp band và KP con trong một request). Quyết định D6: essay của course test được giám khảo chấm mù.
Topic test, practice và placement giữ chính sách LLM hiện có. Mở khóa topic, quyết
định điểm khóa ở Learning và cảnh báo QA không thuộc thay đổi này.

Các đường dẫn Java dưới đây tính từ `services/assessment-service/src/main/java/com/ieltspath/assessment/`;
test tính từ `services/assessment-service/src/test/java/com/ieltspath/assessment/`.

## AI được làm gì

| Quy tắc | Điểm thực thi | Test kiểm tra |
| --- | --- | --- |
| Chấm tạm essay course test, lưu band để so sánh; nhận xét theo tiêu chí từ prompt đang áp dụng. | [application/usecase/GradeGateEssayJobUseCase](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GradeGateEssayJobUseCase.java), [infrastructure/client/OpenAiCompatibleEssayGrader](../services/assessment-service/src/main/java/com/ieltspath/assessment/infrastructure/client/OpenAiCompatibleEssayGrader.java), [JdbcGateEssayJobStore.recordComparison](../services/assessment-service/src/main/java/com/ieltspath/assessment/infrastructure/persistence/adapter/JdbcGateEssayJobStore.java). | [CourseGatePolicyIntegrationTest.completedLlmGradeWaitsForExaminerAndRecordsComparison](../services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/CourseGatePolicyIntegrationTest.java), [OpenAiCompatibleEssayGraderTest.keepsTheExaminerCommentsBesideTheBand](../services/assessment-service/src/test/java/com/ieltspath/assessment/infrastructure/client/OpenAiCompatibleEssayGraderTest.java). |
| Trả nhận xét chữ tạm thời cho chính học viên, ghi rõ chờ giám khảo; không công bố band LLM ở API này. | [GetProvisionalFeedbackUseCase](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetProvisionalFeedbackUseCase.java) dùng DTO chỉ chứa summary/comments/focus; xác minh chủ attempt. | [CourseGatePolicyIntegrationTest.completedLlmGradeWaitsForExaminerAndRecordsComparison](../services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/CourseGatePolicyIntegrationTest.java). |
| Chấm topic test, practice và placement theo chính sách hiện tại. | [EnqueueGateEssayGradingService](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/EnqueueGateEssayGradingService.java), [GateResultAssembler](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GateResultAssembler.java), [PlacementGradingService](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/PlacementGradingService.java). | [GateEssayGradingIntegrationTest.submittedGateEssayIsQueuedThenCompletesResultAndOutboxAfterSuccessfulAiGrade](../services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/GateEssayGradingIntegrationTest.java), các test placement hiện có. |
| Gợi ý bài chỉ từ kho đã duyệt, không được tạo bài mới để phát trực tiếp. Đây là giới hạn của tích hợp nội dung, không phải API gợi ý mới. | [ContentPackageProvider](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/port/ContentPackageProvider.java) và [StartAssessmentAttemptUseCase](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/StartAssessmentAttemptUseCase.java) lấy package có sẵn, đóng băng câu/KP; essay grader chỉ trả điểm/nhận xét, không có cổng ghi Content. | [StartAssessmentAttemptUseCaseTest.topicTestBecomesTopicGateAndFreezesTheAnswerAwayFromTheQuestion](../services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/StartAssessmentAttemptUseCaseTest.java), `contentFailureOrUnknownVersionWritesNothing`. |
| Mã lỗi được lưu phải có trong danh mục. Hiện gắn lỗi objective là quy tắc xác định; không thêm LLM tự tạo mã lỗi. | [SaveAssessmentResultDetailsUseCase](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/SaveAssessmentResultDetailsUseCase.java) qua [ErrorTypeCatalog](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/port/ErrorTypeCatalog.java); [domain/service/ErrorTagger](../services/assessment-service/src/main/java/com/ieltspath/assessment/domain/service/ErrorTagger.java). | [SaveAssessmentResultDetailsUseCaseTest.errorAnalysisWithAnUnknownCodeIsRefusedAndNothingIsSaved](../services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/SaveAssessmentResultDetailsUseCaseTest.java), [ErrorTaggerTest](../services/assessment-service/src/test/java/com/ieltspath/assessment/domain/service/ErrorTaggerTest.java). |
| Phát hiện cờ chỉ là đầu vào gợi ý, không tự chốt kết quả. Cờ QA_SAMPLE và cảnh báo 48 giờ chưa được triển khai. | Queue reason có giá trị dự phòng trong `V11__examiner_queue.sql`; quyền chốt course nằm ở [FinalizeAssessmentResultUseCase](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/FinalizeAssessmentResultUseCase.java)/[ExaminerGradingPolicy](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/ExaminerGradingPolicy.java). | [CourseGatePolicyIntegrationTest.completedLlmGradeWaitsForExaminerAndRecordsComparison](../services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/CourseGatePolicyIntegrationTest.java) bảo vệ ranh giới chốt kết quả; chưa có test phát hiện cờ vì chức năng để sau. |

## AI không được làm gì

| Cấm | Điểm thực thi | Test kiểm tra |
| --- | --- | --- |
| Quyết định đạt/trượt khóa từ band essay LLM hoặc tự phát event course essay. | [GateResultAssembler](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GateResultAssembler.java) bỏ qua COURSE_GATE; [AutoGradeAttemptService](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/AutoGradeAttemptService.java) từ chối course essay; [FinalizeAssessmentResultUseCase](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/FinalizeAssessmentResultUseCase.java) chốt theo điểm của giám khảo. | [CourseGatePolicyIntegrationTest.completedLlmGradeWaitsForExaminerAndRecordsComparison](../services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/CourseGatePolicyIntegrationTest.java), `omittedEssayDoesNotAutomaticallyCompleteCourseGate`, `lateLlmGradePreservesTheExaminerScoreAndDoesNotEmitAgain`. |
| Lộ điểm LLM cho người chấm trước khi chốt. | [ExaminerQueueStore](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/port/ExaminerQueueStore.java) chỉ có projection chấm mù; [GradingJobController](../services/assessment-service/src/main/java/com/ieltspath/assessment/api/controller/GradingJobController.java) phân quyền; [GetJobEssayUseCase](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GetJobEssayUseCase.java) xác minh người nhận. | [GradingJobControllerTest.essayAndQueueUseBlindProjections](../services/assessment-service/src/test/java/com/ieltspath/assessment/api/controller/GradingJobControllerTest.java), `customerCannotListClaimOrReadExaminerEssay`, [GradingQueueIntegrationTest.queueClaimConflictAndExpiredReclaimAreBlind](../services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/GradingQueueIntegrationTest.java). |
| Sinh kiến thức ngoài syllabus hoặc đưa câu hỏi tự sinh chưa duyệt tới học viên. | [StartAssessmentAttemptUseCase](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/StartAssessmentAttemptUseCase.java) chỉ snapshot nội dung nhận từ Content; [EssayGradingPort.EssayGrade](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/port/EssayGradingPort.java) chỉ có band/feedback, không ghi câu hỏi hay KP vào kho. Đây là ranh giới API, không phải bảo đảm kiểm chứng ngữ nghĩa mọi câu nhận xét của mô hình. | [StartAssessmentAttemptUseCaseTest.contentFailureOrUnknownVersionWritesNothing](../services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/StartAssessmentAttemptUseCaseTest.java), `topicTestBecomesTopicGateAndFreezesTheAnswerAwayFromTheQuestion`; [OpenAiCompatibleEssayGraderTest.taskOneRequiresChartFactsBeforeMakingHttpRequest](../services/assessment-service/src/test/java/com/ieltspath/assessment/infrastructure/client/OpenAiCompatibleEssayGraderTest.java). |
| Ghi essay, prompt, phản hồi thô hoặc credentials vào log ứng dụng. | [OpenAiCompatibleEssayGrader](../services/assessment-service/src/main/java/com/ieltspath/assessment/infrastructure/client/OpenAiCompatibleEssayGrader.java) chuyển lỗi provider thành mã an toàn; [GradeGateEssayJobUseCase.handOver](../services/assessment-service/src/main/java/com/ieltspath/assessment/application/usecase/GradeGateEssayJobUseCase.java) chỉ log job id và mã/loại lỗi. Không bật HTTP body logging ở môi trường vận hành. | [GradeGateEssayJobUseCaseTest.providerFailureDoesNotLogEssayOrPrompt](../services/assessment-service/src/test/java/com/ieltspath/assessment/application/usecase/GradeGateEssayJobUseCaseTest.java), [OpenAiCompatibleEssayGraderTest.invalidLlmReplyIsReportedAsSafeGradingFailure](../services/assessment-service/src/test/java/com/ieltspath/assessment/infrastructure/client/OpenAiCompatibleEssayGraderTest.java). |

## So sánh và kiểm toán

`essay_grade_comparisons` giữ hai phần điểm độc lập theo submission, cả khi LLM
hoàn thành sau giám khảo. Không có API đọc bảng này cho người chấm. Điểm giám khảo,
tiêu chí, mã lỗi, danh tính và thời điểm được lưu cùng transaction chốt kết quả,
hoàn tất job và ghi outbox. Khi LLM lỗi, phần LLM để null thay vì giả lập điểm.
Rubric được Content quản lý theo task, phiên bản, tiêu chí và mô tả band. Learning
và Assessment đọc dữ liệu rubric qua API nội bộ; hai bộ chấm dùng cùng template
`v1` tại [essay-prompt-template-v1](contracts/essay-prompt-template-v1.txt). Mỗi lần
chấm lưu `prompt_version` theo dạng `<rubricCode>@<rubricVersion>+tpl<templateVersion>`.
Learning trả lỗi tạm thời nếu không đọc được rubric. Với course essay, Assessment
chuyển job cho EXAMINER khi rubric hoặc prompt đã được kích hoạt không khả dụng.

LLM chỉ được gắn `errorCode` trong danh mục `ACTIVE` của Content; mã không tồn tại
hoặc đã bị loại khỏi danh mục sẽ bị bỏ khỏi kết quả của LLM. Không dùng các mã
do mô hình tự tạo làm bằng chứng lỗi.

Learning gửi đúng một request LLM cho mỗi lần thực thi chấm: giữ nguyên template band `v1`, nối hướng dẫn
`essay-kp-judgment-v2` (`kpj2`) để cùng JSON trả `criteria`, `corrections`, `summary`, `focus` và `judgments` cho các KP
con trong rubric; rubric không có KP con thì không cần `judgments`. `prompt_version` (`+tplv1`) và activation vẫn nhận
diện template band; `judgmentVersion=kpj2` lưu trong JSON grade bất cứ khi nào rubric có KP con (prompt đã nối hướng dẫn), kể cả khi
mô hình không trả mảng `judgments`; không có KP con thì version null. Không phát qua API. Mỗi lần chấm dùng một lượt quota ngày và
timeout chung `learning.writing.grading-timeout-seconds` (mặc định 20 giây); không có timeout/skip riêng cho KP.
Không gọi AI sửa JSON hoặc tự gửi lại HTTP khi provider trả 429/503. JSON hoặc band không hợp lệ gây `INVALID_GRADE`,
không thu points. Khi band hợp lệ nhưng `judgments` thiếu/sai, giữ band và để KP `NOT_ASSESSED`, trừ `FAIL` có thể suy ra
từ correction đã kiểm tra và ánh xạ error type sang KP. PASS/FAIL của mô hình chỉ được nhận khi excerpt là chuỗi nguyên
văn trong essay. Correction và judgment đến từ cùng phản hồi AI; application kiểm tra chéo chúng bằng luật xác định,
không phải hai đánh giá AI độc lập. `EssayKnowledgeJudge` không gọi LLM hay dùng clock; catalog Content được lấy khi
cần qua supplier do grader cung cấp, tối đa một lần.
Không log essay, prompt hay output LLM. `kpj2` chưa có gold set để đo độ chính xác.

Trước khi từng task có dòng đầu tiên trong `ai_prompt_activations`, cả Learning `EssayGrader` và Assessment
`GradeGateEssayJobUseCase` dùng rubric `ACTIVE` của Content để bootstrap; không báo `PROMPT_NOT_ACTIVATED` chỉ vì
chưa có dòng. Migration `V50__rubrics.sql` seed cả `TASK_1` và `TASK_2` ở trạng thái `ACTIVE`, nhưng `source_note`
vẫn ghi rõ đây là bản nháp do AI soạn, chưa được chuyên gia IELTS duyệt. `ACTIVE` không phải chứng nhận chuyên môn.

Sau activation đầu tiên của task, cả hai bộ chấm chỉ dùng `promptVersion` đã kích hoạt, kể cả rubric `RETIRED`.
Learning đọc activation qua API nội bộ Assessment bằng internal JWT, rồi lấy đúng phiên bản rubric từ Content.
Không đọc được activation hoặc rubric là lỗi dịch vụ; không quay về rubric `ACTIVE` để bootstrap.

Lần đánh giá ứng viên luôn dùng rubric `ACTIVE` hiện tại, độc lập phiên bản đang dùng để chấm. Đổi phiên bản đã
kích hoạt chỉ sau khi có lần đánh giá AI đạt ngưỡng và ADMIN kích hoạt; API từ chối bằng `409 EVALUATION_NOT_PASSED`
nếu lần chạy chưa có `passed=true`.
Lần chạy ghi số bài, within-half-band, MAE, QWK, per-criterion và false-positive
error-code; mặc định yêu cầu within-half-band ít nhất `0.80` và QWK ít nhất `0.60`.
QWK dùng band làm tròn theo bước `0.5`. Trước khi prompt mới đạt, Assessment tiếp
tục dùng prompt version đã kích hoạt gần nhất. API gold set, đánh giá và kích hoạt
chỉ dành cho ADMIN; gold set có thể nhập thủ công hoặc lấy từ comparison đã đủ
điểm giám khảo. Chi tiết request/response: [assessment-grading-v1](contracts/assessment-grading-v1.md).

Phase 11 còn `in_progress`: cần gold set do chuyên gia chấm và lần đánh giá thật có chỉ số lưu lại. Seed bootstrap
và test tự động không hoàn thành tiêu chí nghiệm thu này.

## AI sinh nội dung (khu author của Content)

Phần này nói về content-service. Đường dẫn Java tính từ
`services/content-service/src/main/java/com/ieltspath/content/`, test từ `services/content-service/src/test/java/com/ieltspath/content/`.
Chi tiết route: [ai-drafts-v1](contracts/ai-drafts-v1.md), [question-drafts-v1](contracts/question-drafts-v1.md),
[course-drafts-v1](contracts/course-drafts-v1.md), [course-import-v1](contracts/course-import-v1.md).

- **Mọi thứ AI sinh là bản nháp.** Câu hỏi và course AI viết nằm trong `ai_drafts` ở trạng thái `READY`; không có đường nào
  từ bản nháp tới học viên. Người duyệt (CONTENT_AUTHOR sở hữu bản nháp) chọn câu hoặc duyệt course rồi mới có nội dung
  thật: câu hỏi `DRAFT`, course/topic `INACTIVE`, bài `DRAFT`, đề cuối rỗng `DRAFT`. Nội dung đó còn phải xuất bản qua các
  kiểm tra thường lệ.
- **Sinh course chỉ chọn từ kho.** Ứng viên là KP, câu, bài và gói đã có, chọn bằng luật xác định. Mô hình chỉ nhóm và sắp
  xếp trong tập ứng viên; mã nào ngoài tập hoặc lặp bị bỏ. Chỗ thiếu được báo là `gaps`/`unmatchedTopics`, không sinh nội dung
  để lấp.
- **Mô hình không quyết định giá trị.** `answer_spec` do server dựng, không lấy từ mô hình; mã lỗi gắn phương án sai phải
  `ACTIVE` và đã `APPROVED` trong danh mục; câu trùng nguyên văn câu đã có của KP bị loại; độ khó là độ khó đã yêu cầu.
- **Không dữ liệu học viên trong prompt**, không log prompt hay câu trả lời thô; lỗi gọi mô hình thành mã an toàn, hoàn
  hạn mức ngày và bản nháp `FAILED`. Thiếu cấu hình (`CONTENT_LLM_*`) là `NOT_CONFIGURED`, service vẫn chạy.
- **Hạn mức theo author và ngày** (`ai_usage_daily`): yêu cầu sinh và import Excel đếm riêng.

| Quy tắc | Điểm thực thi | Test kiểm tra |
| --- | --- | --- |
| Câu AI sai schema, đáp án không thuộc phương án, mã lỗi lạ hoặc trùng câu cũ không vào bản nháp; sửa tay cũng phải qua luật đó. | [application/service/QuestionDraftValidator](../services/content-service/src/main/java/com/ieltspath/content/application/service/QuestionDraftValidator.java), [QuestionDraftGenerator](../services/content-service/src/main/java/com/ieltspath/content/application/service/QuestionDraftGenerator.java), [EditDraftItemUseCase](../services/content-service/src/main/java/com/ieltspath/content/application/usecase/EditDraftItemUseCase.java). | [QuestionDraftValidatorTest](../services/content-service/src/test/java/com/ieltspath/content/application/service/QuestionDraftValidatorTest.java), [QuestionDraftGeneratorTest](../services/content-service/src/test/java/com/ieltspath/content/application/service/QuestionDraftGeneratorTest.java), [EditDraftItemUseCaseTest](../services/content-service/src/test/java/com/ieltspath/content/application/usecase/EditDraftItemUseCaseTest.java). |
| Duyệt câu hỏi tạo câu `DRAFT` qua use case câu hỏi thường, tất cả hoặc không. | [ApproveQuestionDraftUseCase](../services/content-service/src/main/java/com/ieltspath/content/application/usecase/ApproveQuestionDraftUseCase.java). | [ApproveQuestionDraftUseCaseTest](../services/content-service/src/test/java/com/ieltspath/content/application/usecase/ApproveQuestionDraftUseCaseTest.java). |
| Course AI chỉ dùng mã trong tập ứng viên; không sinh nội dung bù chỗ thiếu. | [CourseCandidateSelector](../services/content-service/src/main/java/com/ieltspath/content/application/service/CourseCandidateSelector.java), [CourseDraftValidator](../services/content-service/src/main/java/com/ieltspath/content/application/service/CourseDraftValidator.java), [CourseDraftGenerator](../services/content-service/src/main/java/com/ieltspath/content/application/service/CourseDraftGenerator.java). | [CourseDraftValidatorTest](../services/content-service/src/test/java/com/ieltspath/content/application/service/CourseDraftValidatorTest.java), [CourseDraftGeneratorTest.noQuestionIsGeneratedToFillAShortage](../services/content-service/src/test/java/com/ieltspath/content/application/service/CourseDraftGeneratorTest.java). |
| Duyệt course tạo course `INACTIVE` và đề cuối rỗng `DRAFT`; lỗi giữa chừng không để lại gì; tên/mã trùng hoặc tiên quyết sai thứ tự bị chặn. | [ApproveCourseDraftUseCase](../services/content-service/src/main/java/com/ieltspath/content/application/usecase/ApproveCourseDraftUseCase.java), [CourseMaterializer](../services/content-service/src/main/java/com/ieltspath/content/application/service/CourseMaterializer.java). | [ApproveCourseDraftIntegrationTest](../services/content-service/src/test/java/com/ieltspath/content/application/usecase/ApproveCourseDraftIntegrationTest.java). |
| Hạn mức ngày được hoàn khi sinh thất bại; việc treo trở lại hàng đợi. | [RunAiDraftJobUseCase](../services/content-service/src/main/java/com/ieltspath/content/application/usecase/RunAiDraftJobUseCase.java). | [RunAiDraftJobUseCaseTest](../services/content-service/src/test/java/com/ieltspath/content/application/usecase/RunAiDraftJobUseCaseTest.java). |
| Import Excel không phải AI nhưng đi qua cùng luật: kiểm trước không ghi gì, ghi tất cả hoặc không, mọi thứ tạo ra là bản nháp/`INACTIVE`, ô công thức và macro bị từ chối. | [CourseImportUseCases](../services/content-service/src/main/java/com/ieltspath/content/application/usecase/CourseImportUseCases.java), [PoiCourseWorkbookReader](../services/content-service/src/main/java/com/ieltspath/content/infrastructure/excel/PoiCourseWorkbookReader.java). | [CourseImportIntegrationTest](../services/content-service/src/test/java/com/ieltspath/content/application/usecase/CourseImportIntegrationTest.java), [PoiCourseWorkbookReaderTest](../services/content-service/src/test/java/com/ieltspath/content/infrastructure/excel/PoiCourseWorkbookReaderTest.java). |

Giới hạn: kiểm tra tự động bảo đảm hình dạng, đáp án và mã lỗi, không bảo đảm đúng chuyên môn IELTS của nội dung mô hình
viết. Đó là việc của người duyệt; chưa có gold set hay chỉ số chất lượng cho phần sinh câu hỏi.
