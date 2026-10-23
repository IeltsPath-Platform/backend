# Phase 09: AI sinh câu hỏi

Người làm: **Claude Code**. Nhánh: `feat/content-ai-question-drafts` (backend), tạo từ `origin/main` sau khi phase 08 đã merge.
Trạng thái: Pending

Repo: `backend`, service `content-service`. Phụ thuộc: phase 07 (`import_ref`), phase 08 (hạ tầng nháp).

## Mục tiêu

"Thư viện đề gen bằng AI" mentor nhắc: tác giả chọn một KP và tham số, AI sinh bản nháp câu hỏi, tác giả xem và sửa từng câu, duyệt thì tạo câu hỏi thật ở `DRAFT`. Câu vẫn phải qua bước `PUBLISHED` hiện có; học viên không thấy gì trước đó.

## Đọc trước

- `docs/contracts/answer-spec-v1.md`, `docs/contracts/knowledge-tree-v1.md`, `docs/contracts/question-publication-v1.md`.
- Use case tạo câu hỏi và thêm phiên bản câu hỏi (`AddQuestionVersionUseCase`): phần kiểm `answer_spec` phải dùng lại, không viết lại.
- Danh mục mã lỗi `/api/content/error-types` (đọc trong cùng service).

## Yêu cầu

- **Yêu cầu sinh:** `POST /api/content/admin/ai-drafts/questions` (chỉ `CONTENT_AUTHOR`):

  ```json
  { "knowledgePointId": "…", "questionType": "MULTIPLE_CHOICE", "count": 10,
    "difficulty": "MEDIUM", "instructions": "tùy chọn, tối đa 500 ký tự" }
  ```

  - `count` 1..`max-items-per-request`; KP phải `ACTIVE`.
  - `questionType` thuộc loại tự chấm được: `MULTIPLE_CHOICE`, `FILL_IN_BLANK`, `TRUE_FALSE_NOT_GIVEN`, `SHORT_ANSWER`, `MATCHING`. **Không sinh `ESSAY`, `SPEAKING`.**
  - Trừ một lượt hạn mức `AI`, lưu `ai_drafts` (`QUEUED`, `kind = QUESTIONS`), trả `202` kèm id.
- **Prompt** (dựng từ dữ liệu, ghi `prompt_version`):
  - mô tả KP, band của course chứa KP, KP tiên quyết;
  - mã lỗi liên quan KP, yêu cầu gắn mã cho từng phương án sai;
  - mẫu `answer_spec` theo `answer-spec-v1.md`;
  - tối đa 3 câu `PUBLISHED` của KP làm mẫu phong cách;
  - ràng buộc: ngữ liệu tự viết, không chép đề thương mại, không đưa kiến thức ngoài mô tả KP;
  - không có dữ liệu học viên nào trong prompt.
- **Kiểm tra từng câu** trước khi vào `result` (câu hỏng bị loại kèm lý do, không làm hỏng cả bản nháp):
  - đúng schema `answer_spec` (bộ kiểm có sẵn);
  - trắc nghiệm có đúng một đáp án đúng, phương án không trùng;
  - mã lỗi trong `distractors` thuộc danh mục `ACTIVE`;
  - có `difficulty` đúng yêu cầu và có giải thích;
  - không trùng nguyên văn câu đã có của cùng KP (chuẩn hóa chữ thường, bỏ khoảng trắng thừa).
- **Sửa bản nháp:** `PATCH /api/content/admin/ai-drafts/{id}/items/{index}` sửa đề, phương án, đáp án, giải thích, mã lỗi; kiểm lại bằng cùng bộ luật. `DELETE …/items/{index}` bỏ một câu. Chỉ khi bản nháp `READY`.
- **Duyệt:** `POST /api/content/admin/ai-drafts/{id}/approve` với `{ "itemIndexes": [0, 2, 5] }`:
  - mỗi câu tạo `questions` + `question_versions` ở `DRAFT` bằng use case câu hỏi hiện có, `import_ref = "ai-<draftId>-<index>"`, gắn KP với trọng số 1.0, giữ `difficulty`, `distractors`;
  - bản nháp chuyển `APPROVED`, ghi `reviewed_by`, `reviewed_at`; tất cả trong một transaction;
  - duyệt bản nháp đã `APPROVED`/`REJECTED` → `409 DRAFT_ALREADY_REVIEWED`.
- LLM trả hỏng định dạng: thử lại tối đa 1 lần trong cùng việc, vẫn hỏng → `FAILED` `INVALID_REPLY` (hoàn hạn mức).
- Bổ sung route, body, mã lỗi vào `ai-drafts-v1.md` trong cùng commit.

## Thiết kế phía code

```text
RequestQuestionDraftUseCase ─ trừ quota, lưu ai_drafts(QUEUED, kind=QUESTIONS)
RunAiDraftJobUseCase (phase 08) ─ gọi QuestionDraftGenerator
QuestionDraftGenerator ─ QuestionPromptBuilder → ContentGenerationPort → QuestionDraftValidator → result
EditDraftItemUseCase, ApproveQuestionDraftUseCase ─ duyệt tạo câu bằng use case câu hỏi hiện có
```

Tạo: `C/application/usecase/{RequestQuestionDraftUseCase, EditDraftItemUseCase, ApproveQuestionDraftUseCase}.java`, `C/application/service/{QuestionDraftGenerator, QuestionPromptBuilder, QuestionDraftValidator}.java`, `C/api/controller/AiQuestionDraftController.java`, DTO. Sửa: tách phần kiểm `answer_spec` của use case câu hỏi thành hàm dùng lại nếu đang nằm lẫn.

## Test

- `QuestionDraftValidatorTest` (bảng tình huống): sai schema, hai đáp án đúng, mã lỗi lạ, trùng câu cũ, thiếu giải thích.
- `QuestionPromptBuilderTest`: có mã lỗi và mẫu; không có dữ liệu học viên.
- `QuestionDraftIntegrationTest` (port giả): LLM trả 10 câu, 3 câu hỏng → bản nháp còn 7 câu kèm lý do; sửa một câu; duyệt 5 câu → 5 câu `DRAFT` có KP, difficulty, `importRef`; duyệt lại → 409.
- `ESSAY` trong yêu cầu → 400; KP không `ACTIVE` → 400.

## Hoàn thành khi

`mvn -q -pl services/content-service -am test` xanh; câu sai schema hoặc mã lỗi lạ không vào được bản nháp; câu duyệt xong là `DRAFT` và học viên không thấy.

## Kết quả

(Người làm điền.)
