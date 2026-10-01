# Learning lessons — kiểm chứng ngày 2026-10-01

Status: DONE

## Đã làm

- Nhánh `feat/learning-lessons` từ `feat/main-follow` tại `9d6f192`; đã fetch origin (`07324d9` là ancestor, local có thêm một commit tài liệu). Không push.
- Domain thuần: `MasteryCalculator`, `AnswerSpecGrader`, `TopicStatusDeriver`, `LessonAccessGate`, `ReviewRule`. Giữ comment nguồn DeepTutor v1.6.9 Apache-2.0.
- Port `LearningContentClient` + RestClient cho sáu route nội bộ Content, forward verified bearer và correlation ID; lỗi an toàn 404/502/503.
- Triển khai `GET /api/learning/topics`, `/topics/{id}/lessons`, `/lessons/{id}`, `POST /lessons/{id}/exercises/{blockId}/submissions`, `POST /lessons/{id}/complete`, `GET /api/learning/mastery`.
- Mọi entry point ghi mở một Spring transaction và lấy `pg_advisory_xact_lock(hashtext(user_id))` trước khi làm việc. JDBC dùng connection của transaction; refresh lồng nhau tham gia cùng transaction.
- Replay `requestId` đúng scope trả response đã lưu trước cổng hiện tại; khác user/bài/khối trả 409. Mọi truy cập mới áp đúng cổng review → topic → bài.
- Bằng chứng chỉ ở lần nộp đầu của khối; source `lesson_exercise`, reference UUIDv5. Namespace cố định được suy bằng UUIDv5 DNS từ `ielts-path:lesson_exercise`; tên reference là `{requestId}:{questionVersionId}:{kpId}`.
- Ghi bằng chứng và catalog theo lô; hoàn thành bài, lưu response và chèn review cùng transaction. Không repository/HTTP trong vòng lặp.
- Catalog upsert theo thứ tự UUID KP cố định để tránh đảo thứ tự khóa giữa user; thứ tự API topic vẫn theo Content.
- Mastery dùng một query theo user, đếm mọi evidence và lấy năm ordinal gần nhất theo thứ tự tăng dần để tính trọng số. Không dùng `created_at` quyết định thứ tự.
- DTO câu hỏi chỉ có `questionVersionId`, `sortOrder`, `stem`, `options`; fill giữ `options:null`. Lời giải riêng chỉ có khi khối đạt. Lỗi cổng qua `GlobalExceptionHandler` dưới dạng `{detail, code, reviews?}`; malformed input 422.
- README module, trạng thái contract và toàn bộ tiến độ plan đã đồng bộ. Phase 1 vẫn completed; 3–5 pending. Chưa làm API bài ôn/giao mã đề/consumer hay tài liệu kiến trúc toàn repo.

## Test đã chạy

| Lệnh | Kết quả cuối |
| --- | --- |
| `mvn -q -pl services/learning-service -am test -Dtest=MasteryCalculatorTest,AnswerSpecGraderTest,TopicStatusDeriverTest,LessonAccessGateTest,ReviewRuleTest -Dsurefire.failIfNoSpecifiedTests=false` | 52 pass, 0 fail/error/skip |
| `mvn -q -pl services/learning-service -am -Dtest=RestLearningContentClientTest -Dsurefire.failIfNoSpecifiedTests=false test` | 23 pass, 0 fail/error/skip |
| `mvn -q -pl services/learning-service -am test -Dtest=LessonSubmissionIntegrationTest,LessonLearningWebMvcTest -Dsurefire.failIfNoSpecifiedTests=false` | 39 pass: MVC 27 + PostgreSQL 12; 0 fail/error/skip |
| **`mvn -q -pl services/learning-service -am test`** | **125 pass: Learning 117 + common-security 8; 0 fail/error/skip; exit 0** |
| `graphify update .` | Exit 0; graph cuối 7,426 nodes, 25,256 edges; artifact ignored, không commit |
| `git diff --check` | Pass |

Docker khả dụng; 12 test integration và 1 test context/Flyway chạy trên PostgreSQL Testcontainers thật. Không gọi LLM hay database của service khác. Test cấu hình riêng, không import `.env`.

Phân bố Learning: mastery 4; grader 31 (đủ 27 vector JSON + 4 ca bổ sung); trạng thái topic 5; cổng 6; ReviewRule 6; Content client 23; MVC mới 27; security cũ 2; PostgreSQL integration 12; context/Flyway 1. Tổng 117.

Số Lan giữ nguyên, so với số làm tròn trong seed (sai số cho phép 0.0005):

| KP/tình huống | Java tính thật | Seed |
| --- | --- | --- |
| KP3, L1 đúng/đúng/sai/đúng | 0.7285714285714285 | 0.729 |
| KP1, sai rồi bốn câu ôn đúng | 0.875 | 0.875 |
| KP2, đúng rồi sai | 0.48717948717948717 | 0.487 |

Test PostgreSQL chứng minh replay, request conflict, retry không tăng evidence, metadata không phụ thuộc GET trước, bài xong chèn review, ordinal/count/cách ly user, khóa user khi nộp đồng thời, rollback khi chèn review lỗi và catalog refresh đồng thời ngược thứ tự. MVC kiểm tra allowlist, lời giải, cổng, identity, 422/409 và sáu route protected.

Hai lỗi đã sửa và kiểm chứng: binding path variable khi compiler không giữ tên tham số; thứ tự khóa catalog giữa user. Một lượt test catalog chạy bytecode cũ sau khi source sửa trong lúc compile; `javap` xác nhận, cập nhật timestamp source buộc compile lại, test nguyên vẹn rồi pass. [Code review](./learning-lessons-code-review.md) không còn blocker. Không có linter/coverage gate trong repo, không tuyên bố đã chạy chúng.

## File đổi

Tiền tố `services/learning-service/src/main/java/com/group01/learning/`:

- `api/controller/LessonLearningController.java`
- `api/dto/{LessonResponse,MasteryResponse,SubmissionResponse,SubmitExerciseRequest,TopicLessonsResponse,TopicResponse}.java`
- `api/exception/{GlobalExceptionHandler,LearningErrorResponse}.java`
- `application/{LessonEvidenceReference,ReviewReevaluation}.java`
- `application/command/SubmitExerciseCommand.java`
- `application/exception/LearningRequestException.java`
- `application/port/{LearningContentClient,LearningProgressStore}.java`
- `application/result/{LessonResult,MasteryResult,SubmissionResult,TopicLessonsResult,TopicResult}.java`
- `application/usecase/{GetMasteryUseCase,LearnLessonUseCase,RefreshLearningTopicsUseCase}.java`
- `domain/exception/LearningGateException.java`
- `domain/service/{AnswerSpecGrader,LessonAccessGate,MasteryCalculator,ReviewRule,TopicStatusDeriver}.java`
- `domain/vo/{KnowledgePointCatalogEntry,LessonProgress,PendingReview,TopicProgress,TopicStatus}.java`
- `infrastructure/client/RestLearningContentClient.java`
- `infrastructure/persistence/JdbcLearningProgressStore.java`

Tiền tố `services/learning-service/src/test/java/com/group01/learning/`:

- `api/LessonLearningWebMvcTest.java`
- `domain/service/{AnswerSpecGraderTest,LessonAccessGateTest,MasteryCalculatorTest,ReviewRuleTest,TopicStatusDeriverTest}.java`
- `infrastructure/client/RestLearningContentClientTest.java`
- `infrastructure/persistence/LessonSubmissionIntegrationTest.java`

Tài liệu:

- `services/learning-service/README.md`
- `docs/contracts/lesson-learning-v1.md`
- `docs/journals/261001-learning-lessons-transaction-checks.md`
- `plans/261001-1228-learning-service-java/{plan,codex-handoff,phase-02-bai-hoc-va-mastery}.md`
- `plans/261001-1228-learning-service-java/reports/{learning-lessons-code-review,learning-lessons-verification}.md`

Không sửa V1, POM, Gateway, common-security, config dùng chung hay `.env`.

Lệch plan: không.

Cần người duyệt: review code trước khi merge. Không còn quyết định nghiệp vụ hoặc blocker kỹ thuật; catalog là snapshot theo lần refresh, đúng phạm vi MVP.
