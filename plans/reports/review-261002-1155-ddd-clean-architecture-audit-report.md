# Audit DDD và Clean Architecture các service Java — 2026-10-02

Phạm vi: 8 business service (bỏ notification vì chỉ là khung). Chuẩn so sánh: `AGENTS.md` §3.1–3.2, §3.1.1.
Cách kiểm: cây package và số file, grep import theo hướng phụ thuộc, heuristic hành vi aggregate, đọc mẫu code
learning-service. Chưa đọc hết từng file của service khác; các con số "anemic" là heuristic (aggregate không có
public method nào ngoài getter), cần đọc lại trước khi sửa.

## Kết luận nhanh

- **Không service nào vi phạm hướng import**: domain không import Spring/JPA/HTTP hay layer ngoài; application không
  import api/infrastructure; api không gọi JPA repository/JdbcTemplate.
- **learning-service lệch nặng nhất về mô hình**: viết theo kiểu transaction script, domain không có aggregate hay
  repository; chuyển trạng thái nằm trong SQL của adapter và trong use case dài.
- Các service khác đúng khung, có vài lệch nhỏ đến vừa (outbox trong domain, assessment một aggregate mười ba entity,
  user trả aggregate ra api, Lombok builder trên aggregate).

## learning-service

| Vấn đề | Bằng chứng | Mức |
| --- | --- | --- |
| Không có aggregate, không có `domain/repository` | `domain/` chỉ có `service/` (7 hàm thuần), `vo/` (6 record dữ liệu), `exception/` | Cao |
| Persistence contract nằm ở `application/port` dạng "Store" thao tác từng dòng | `LearningProgressStore` (18 method: `passBlock`, `completeLesson`, `appendEvidence`…), `ReviewStore` (`finishReview(UUID, String status)`, `closeSet`), `WritingSubmissionStore` (`markFailed`, `saveGrade`, `markGraded`, `recordPaymentFailure`…) | Cao |
| Luật chuyển trạng thái nằm trong SQL adapter | `JdbcLearningProgressStore.passBlock` (`array_append … AND NOT … = ANY`), `completeLesson` (`WHERE completed_at IS NULL`), `JdbcReviewStore.finishReview` (`WHERE status = 'PENDING'`) | Cao |
| Use case dài, gộp nhiều trách nhiệm | `LearnLessonUseCase` 263 dòng (`submit` chấm, cổng bài, bằng chứng, gợi ý, hoàn thành, bài ôn), `LessonEssayUseCase` 261, `ReviewUseCase` 177 | Vừa |
| Trạng thái là chuỗi, không có kiểu | `finishReview(…, String status)`, `"EXERCISE".equals(blockType)`, `"COMPLETED"`/`"AVAILABLE"` trong use case | Vừa |
| Port persistence phụ thuộc DTO của application | `LearningProgressStore.saveSubmission(…, SubmitExerciseCommand, SubmissionResult)`, `StoredSubmission.response: SubmissionResult` | Vừa |
| Package lạc chỗ | 4 class ở gốc `application/` (`LessonAccess`, `ReviewReevaluation`, `AnswerSheet`, `LessonEvidenceReference`); `application/writing/` (grader LLM, prompt, settings); `api/dto` phẳng 11 file thay vì `request/response` | Thấp |

Điểm đúng nên giữ: port cho client ngoài (`LearningContentClient`, `AccessClient`, `LlmClient`) đúng chuẩn; luật thuần
(`MasteryCalculator`, `ReviewRule`, `AnswerSpecGrader`, `WritingScore`) đã ở domain và không phụ thuộc framework;
adapter dùng JDBC là chi tiết hạ tầng hợp lệ (advisory lock, `ON CONFLICT`, mảng PostgreSQL là chủ đích), không cần
đổi sang JPA để đạt Clean Architecture.

## Service khác

| Service | Vấn đề | Bằng chứng | Mức |
| --- | --- | --- | --- |
| content, access, assessment | Outbox (concern kỹ thuật) nằm trong domain | content `domain/aggregate/OutboxEvent` + `domain/repository/OutboxEventRepository`; access, assessment `domain/entity/OutboxEvent` + repository | Vừa |
| assessment | Một aggregate, 13 entity, 14 repository: repository theo bảng thay vì theo aggregate, ranh giới aggregate không rõ | `domain/aggregate` 1 file, `domain/entity` 13, `domain/repository` 14 | Vừa (sửa tốn công) |
| user | Use case trả aggregate ra api | 3 file `api/` import `domain.aggregate` (ví dụ `UserManagementController`) | Thấp–vừa |
| user, library | Lombok `@Builder`/`@AllArgsConstructor` trên aggregate cho phép dựng đối tượng bỏ qua invariant | user 9 file, library 5 file trong `domain/aggregate` | Thấp |
| user, content, access, library, game | Một số aggregate chỉ có getter (heuristic) | user 3/9, content 2/6, access 2/5, library 2/8, game 3/7 | Thấp, cần đọc lại |
| user, library, community | Vị trí adapter không thống nhất | `infrastructure/adapter` và `infrastructure/persistence/adapter` cùng tồn tại (library có cả hai) | Thấp; `AGENTS.md` cho phép theo convention service |

## Đề xuất thứ tự

1. learning-service: đưa trạng thái và luật chuyển trạng thái vào aggregate, đổi Store thành repository theo
   aggregate, giữ JDBC adapter và contract HTTP/event. Có 183 test và kịch bản E2E 95 check làm lưới an toàn.
2. Outbox ra khỏi domain ở content/access/assessment (port ở application, entity ở infrastructure).
3. user: use case trả `Result` thay vì aggregate.
4. assessment: xác định lại ranh giới aggregate (việc lớn, nên có plan riêng).
5. Lombok builder trên aggregate, aggregate thiếu hành vi: sửa dần khi chạm vào.

## Câu hỏi mở

- Mức refactor learning-service (toàn bộ hay theo từng aggregate) — cần quyết định trước khi lập plan.
- Có đưa mục 2–5 vào cùng đợt hay để sau MVP.
