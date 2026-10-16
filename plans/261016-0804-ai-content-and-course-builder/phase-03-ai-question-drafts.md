---
phase: 3
title: "AI sinh câu hỏi"
status: pending
priority: P2
dependencies: [2, "plan 261016-0131 phase 1, 2, 3"]
---

# Phase 3: AI sinh câu hỏi

## Overview

Chức năng "thư viện đề gen bằng AI" mentor nhắc: R&D chọn một KP và các tham số, AI sinh bản nháp câu hỏi, R&D xem và
sửa, duyệt thì tạo câu hỏi thật ở trạng thái `DRAFT`. Mỗi câu được kiểm tra trước khi vào bản nháp.

## Requirements

- **Yêu cầu:** `POST /api/content/admin/ai-drafts/questions` (`CONTENT_AUTHOR`, `ADMIN`):

  ```json
  { "knowledgePointId": "…", "questionType": "MULTIPLE_CHOICE", "count": 10,
    "difficulty": "MEDIUM", "instructions": "tùy chọn, tối đa 500 ký tự" }
  ```

  - `count` tối đa `max-items-per-request`; KP phải `ACTIVE`; `questionType` thuộc loại câu tự chấm được
    (`MULTIPLE_CHOICE`, `FILL_IN_BLANK`, `TRUE_FALSE_NOT_GIVEN`, `SHORT_ANSWER`, `MATCHING`). **Không sinh `ESSAY`,
    `SPEAKING`** (chấm theo rubric, ngoài phạm vi).
  - Trả `202` kèm id bản nháp.
- **Prompt (dựng từ dữ liệu, `prompt_version` ghi lại):**
  - mô tả KP, band của course chứa KP, KP tiên quyết (phase 1 của plan cây);
  - danh mục mã lỗi liên quan KP (phase 3 của plan cây) và yêu cầu gắn mã cho phương án sai;
  - mẫu `answer_spec` theo `docs/contracts/answer-spec-v1.md`;
  - tối đa 3 câu mẫu đã `PUBLISHED` của KP để giữ phong cách;
  - ràng buộc: ngữ liệu tự viết, không chép đề thương mại, không đưa kiến thức ngoài mô tả KP.
- **Kiểm tra từng câu** trước khi đưa vào `result` (câu hỏng bị loại và ghi lý do, không làm hỏng cả bản nháp):
  - đúng schema `answer_spec` (dùng đúng bộ kiểm tra của use case thêm phiên bản câu hỏi, không viết lại);
  - có đúng một đáp án đúng (với trắc nghiệm), phương án không trùng nhau;
  - mã lỗi trong `distractors` có trong danh mục `ACTIVE`;
  - có `difficulty` khớp yêu cầu, có giải thích;
  - không trùng nguyên văn câu đã có của cùng KP (so sánh chuẩn hóa chữ thường, bỏ khoảng trắng).
- **Xem và sửa bản nháp:** `PATCH /api/content/admin/ai-drafts/{id}/items/{index}` sửa từng câu (đề, phương án, đáp án,
  giải thích, mã lỗi); mỗi lần sửa kiểm tra lại cùng bộ luật trên; `DELETE` bỏ một câu.
- **Duyệt:** `POST /api/content/admin/ai-drafts/{id}/approve` với danh sách chỉ số câu muốn nhận:
  - mỗi câu tạo `questions` + `question_versions` (trạng thái `DRAFT`, `import_ref` = `ai-<draftId>-<index>`), gắn
    `question_knowledge_points` với trọng số 1.0;
  - bản nháp chuyển `APPROVED`; ghi `reviewed_by`;
  - câu vẫn phải qua bước `PUBLISHED` riêng. Học viên không thấy.
- Duyệt lại một bản nháp đã `APPROVED` → `409 DRAFT_ALREADY_REVIEWED`.

## Architecture

```text
RequestQuestionDraftUseCase  ─ trừ quota, lưu ai_drafts(QUEUED)
RunAiDraftJobUseCase (phase 2) ─ gọi QuestionDraftGenerator (application service)
QuestionDraftGenerator ─ dựng prompt (QuestionPromptBuilder) → ContentGenerationPort → QuestionDraftValidator → result
ApproveQuestionDraftUseCase ─ tạo Question/QuestionVersion bằng use case hiện có (AddQuestionVersion…)
```

## Related Code Files

(`C` = content package)

- Tạo: `C/application/usecase/{RequestQuestionDraftUseCase, EditDraftItemUseCase, ApproveQuestionDraftUseCase}.java`,
  `C/application/service/{QuestionDraftGenerator, QuestionPromptBuilder, QuestionDraftValidator}.java`,
  `C/api/controller/AiQuestionDraftController.java`, DTO
- Sửa: use case thêm phiên bản câu hỏi (tách phần kiểm tra `answer_spec` thành hàm dùng lại, nếu đang nằm lẫn)
- Test: `QuestionDraftValidatorTest` (bảng tình huống: đáp án sai schema, hai đáp án đúng, mã lỗi lạ, trùng câu cũ),
  `QuestionPromptBuilderTest` (có mã lỗi và mẫu; không chứa nội dung học viên), `QuestionDraftIntegrationTest` (cổng giả
  trả JSON mẫu → duyệt → câu hỏi `DRAFT` có KP và difficulty)

## Implementation Steps

1. **Đỏ trước:** `QuestionDraftValidatorTest`.
2. Validator, prompt builder, generator.
3. Use case yêu cầu, sửa, duyệt; controller.
4. Integration test với cổng giả; thêm kịch bản: LLM trả 3/10 câu hỏng → còn 7 câu trong bản nháp kèm danh sách lý do.
5. `mvn -q -pl services/content-service -am test`.

## Success Criteria

- [ ] Câu AI trả sai schema hoặc mã lỗi lạ không vào được bản nháp.
- [ ] Duyệt tạo đúng câu hỏi `DRAFT`, có KP, difficulty, `distractors`; học viên không thấy.
- [ ] Không có tham chiếu tới dữ liệu học viên trong prompt.

## Risk Assessment

- Chất lượng chuyên môn của câu do AI sinh không kiểm tự động được (đáp án có thể sai về ngữ nghĩa). Người duyệt chịu
  trách nhiệm; giao diện duyệt phải hiện rõ đáp án và giải thích. Đây là lý do mọi câu vẫn phải qua `PUBLISHED`.
- Bản nháp lớn làm `result` JSONB dài. Giới hạn `max-items-per-request`.

## Bàn giao FE

Màn tạo yêu cầu (chọn KP, loại câu, số lượng), màn xem và sửa từng câu, nút nhận hoặc bỏ từng câu rồi duyệt.
