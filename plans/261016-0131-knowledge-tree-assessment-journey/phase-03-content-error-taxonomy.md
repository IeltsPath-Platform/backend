---
phase: 3
title: "Error taxonomy và gắn mã lỗi cho đáp án"
status: pending
priority: P1
dependencies: []
---

# Phase 3: Error taxonomy và gắn mã lỗi cho đáp án

## Overview

Board yêu cầu mỗi câu trả lời gửi kèm "error tag", và có danh mục lỗi gồm loại, severity, Knowledge ID, gợi ý sửa.
Hiện tại:
- `error_analysis_items.error_type` là chữ tự do, chỉ giám khảo nhập.
- LLM chấm essay ở learning có 4 nhóm GRAMMAR, VOCABULARY, COHERENCE, TASK.

Phase này tạo danh mục chuẩn, gắn mã lỗi cho từng phương án sai, và cho bộ chấm tự sinh mã lỗi.

## Requirements

- Migration content `V31__error_taxonomy.sql`:

  ```sql
  CREATE TABLE error_types (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      code VARCHAR(60) NOT NULL UNIQUE CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
      name VARCHAR(255) NOT NULL,
      category VARCHAR(30) NOT NULL CHECK (category IN
          ('GRAMMAR','VOCABULARY','COHERENCE','TASK','READING_STRATEGY','LISTENING_STRATEGY','SPELLING_NUMBER')),
      severity SMALLINT NOT NULL CHECK (severity BETWEEN 1 AND 3),
      knowledge_point_id UUID REFERENCES knowledge_points(id),
      fix_hint TEXT NOT NULL,
      status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  ```

  Severity: 1 = lỗi nhỏ, 2 = làm mất điểm, 3 = cản trở hiểu hoặc sai yêu cầu đề.
- **Seed khoảng 25 mã** (nháp, cần duyệt):
  - `TFNG_NG_AS_FALSE`, `TFNG_FALSE_AS_NG`, `PARAPHRASE_TRAP`, `KEYWORD_MATCH_ONLY`, `SCOPE_OVERGENERALISED`,
    `WRONG_PARAGRAPH`, `INFERENCE_UNSUPPORTED`;
  - `SPELLING`, `NUMBER_FORMAT`, `PLURAL_FORM`, `WORD_LIMIT_EXCEEDED`, `DISTRACTOR_CORRECTED`, `WRONG_SPEAKER`;
  - `TENSE`, `ARTICLE`, `SUBJECT_VERB_AGREEMENT`, `SENTENCE_FRAGMENT`, `WORD_CHOICE`, `COLLOCATION`, `LINKER_MISUSE`,
    `NO_CLEAR_POSITION`, `UNDERDEVELOPED_IDEA`, `OFF_TOPIC`, `T1_NO_OVERVIEW`, `T1_DATA_MISREPORTED`.

  Mỗi mã gắn KP tương ứng trong cây của phase 2, nếu có.
- **API:**
  - `GET /api/content/error-types`: `ADMIN`, `CONTENT_AUTHOR`, `EXAMINER`.
  - `POST` / `PUT /api/content/error-types/{id}`: `ADMIN`, `CONTENT_AUTHOR`.
  - API nội bộ `GET /internal/learning-content/error-types` (learning, assessment cùng dùng; assessment gọi content theo
    pattern đang có).
- **Mã lỗi theo phương án sai**, trong answer spec (contract `answer-spec-v1`, thêm khóa tùy chọn, không đổi
  `schema_version`):

  ```json
  { "correct": "A", "distractors": { "B": "PARAPHRASE_TRAP", "C": "KEYWORD_MATCH_ONLY" } }
  ```

  - Content validate mọi mã trong `distractors` phải tồn tại và `ACTIVE`.
  - Answer spec vốn không trả cho học viên trước khi nộp. Kiểm lại rằng `distractors` cũng không lộ ra trong snapshot gửi
    học viên.
- **Luật phân loại cho câu điền** (completion), chỉ áp khi câu sai:
  - cả hai cùng là số, giá trị khác nhau hoặc khác định dạng → `NUMBER_FORMAT`;
  - khoảng cách Levenshtein ≤ 2 với đáp án đúng → `SPELLING`;
  - chỉ khác "s" cuối → `PLURAL_FORM`;
  - vượt giới hạn số từ → `WORD_LIMIT_EXCEEDED`.
- **Bộ chấm:**
  - Learning (`ItemGrading`) và assessment (`AnswerSpecGrader`) trả thêm `errorCode` cho câu sai: lấy từ `distractors`,
    nếu không có thì áp luật completion, không ra mã thì null.
  - Không đưa logic này vào `shared/`, vì AGENTS.md cấm đặt nghiệp vụ dùng chung ở đó. Mỗi service có domain service
    nhỏ riêng, test bằng cùng bộ vector.
- **Assessment:**
  - `AutoGradeAttemptService` tự ghi `error_analysis_items` cho câu sai có mã: mỗi KP của câu một dòng.
  - `SaveAssessmentResultDetailsUseCase` (giám khảo nhập) chỉ nhận mã có trong danh mục, mã lạ → `422 UNKNOWN_ERROR_CODE`.

## Related Code Files

- content: migration V31; `domain/aggregate/ErrorType`, repository, use case CRUD, `ErrorTypeController`,
  validate trong `AddQuestionVersionUseCase`; `InternalLearningContentController`
- learning: `application/service/ItemGrading.java`, `domain/service/ErrorTagger.java` (mới)
- assessment: `domain/service/AnswerSpecGrader.java`, `domain/service/ErrorTagger.java` (mới),
  `application/usecase/AutoGradeAttemptService.java`, `application/usecase/SaveAssessmentResultDetailsUseCase.java`,
  client gọi content lấy danh mục
- Contract: `docs/contracts/answer-spec-v1.md`, `answer-spec-v1-vectors.json` (thêm vector có `distractors`)
- Test: `ErrorTaggerTest` ở cả hai service (chung vector), `ErrorTypeControllerTest`, `AddQuestionVersionUseCaseTest`
  (mã lạ → 422), `AutoGradeAttemptServiceTest`

## Implementation Steps

1. Duyệt danh sách mã lỗi với người hiểu IELTS.
2. **Đỏ trước:** vector trong `answer-spec-v1-vectors.json` và `ErrorTaggerTest` cho cả hai service.
3. Migration và CRUD danh mục ở content.
4. Validate `distractors` khi tạo version câu hỏi. Gắn `distractors` cho câu trắc nghiệm trong seed (ít nhất câu của
   các KP Reading và Listening).
5. Viết `ErrorTagger` ở learning và assessment, nối vào bộ chấm.
6. Assessment ghi `error_analysis_items` tự động và chặn mã lạ khi giám khảo nhập.
7. `mvn -q -pl services/content-service,services/learning-service,services/assessment-service -am test`.

## Success Criteria

- [ ] Chọn phương án đã gắn mã → kết quả chấm có `errorCode` đúng.
- [ ] Câu điền sai chính tả nhẹ → `SPELLING`; sai số → `NUMBER_FORMAT`.
- [ ] `distractors` không xuất hiện trong response gửi học viên trước khi nộp.
- [ ] Giám khảo không lưu được mã ngoài danh mục.

## Risk Assessment

- Luật Levenshtein có thể gán nhầm `SPELLING` cho một từ khác nghĩa nhưng gần chữ (ví dụ "fell"/"fill"). Chấp nhận:
  đây chỉ là gợi ý nhóm lỗi, không ảnh hưởng đúng/sai.
- Danh mục đổi (`INACTIVE`) mà câu cũ còn trỏ tới thì vẫn đọc được, chỉ không cho gán mới.

## Bàn giao FE

- Màn danh mục lỗi cho R&D.
- Ô chọn mã lỗi ở màn chấm của EXAMINER.
- Form câu hỏi cho chọn mã lỗi cho từng phương án sai.
