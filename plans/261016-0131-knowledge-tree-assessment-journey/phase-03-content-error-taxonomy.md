---
phase: 3
title: "Error taxonomy và gắn mã lỗi cho đáp án"
status: completed
priority: P1
dependencies: []
---

# Phase 3: Error taxonomy và gắn mã lỗi cho đáp án

## Trạng thái và tách phần

- **3a (kỹ thuật): xong, chưa commit.** Bảng `error_types` (V34) với 25 mã nháp, CRUD và endpoint nội bộ, kiểm `distractors`
  khi tạo phiên bản câu, `ErrorTagger` ở learning và assessment dùng chung bộ vector, assessment ghi `error_analysis_items`
  tự động và chặn mã lạ khi giám khảo nhập. Test: content 312, learning 280, assessment 243, đều xanh, không skip.
- **3b (nội dung): xong. Quyết định Q1–Q4 đã chốt (chưa có chuyên gia IELTS duyệt từng dòng).** V35 gắn `distractors` cho 19 câu trắc nghiệm Reading và Listening
  (mục đích `LEARNING`), V36 thêm 3 mã (`CONTRADICTED_BY_PASSAGE`, `DETAIL_AS_MAIN_IDEA`, `WRONG_TIME_FRAME`) và gắn 6
  phương án còn trống. Tổng: 20 câu, 31 phương án sai, 28 mã. Còn 139 câu trắc nghiệm Reading/Listening chưa gắn (Q4: giữ
  phạm vi này). Gói duyệt: [plans/reports/review-error-taxonomy-pack-261016-2141-report.md](../reports/review-error-taxonomy-pack-261016-2141-report.md).

### Kết quả duyệt (Q1–Q4)

- **Q1 (thêm 3 mã): đã thêm** ở V36, nhóm READING_STRATEGY, severity 2. Gắn cho 6 phương án (mục C của gói duyệt).
- **Q2 (severity): đồng ý.** Người duyệt lưu ý `KEYWORD_MATCH_ONLY` (mức 1) xuất hiện nhiều nhất (8 lần trong 19 câu). Chưa
  đổi mức; nếu muốn nâng, sửa ở migration mới.
- **Q3 (gắn vào phiên bản đã publish): chấp nhận, kèm ba điều kiện đã kiểm:**
  - (a) Bộ chấm bỏ qua khóa lạ: đã kiểm. Learning và Assessment chỉ đọc `type`, `correct`, `accepted`, `maxWords` (không
    duyệt khóa lạ). Game-service chỉ đọc danh sách khóa cố định (`answer`, `correctAnswer`, ...). Library tự ghi spec riêng,
    không đọc spec của content. **Trong repo không có bộ chấm answer-spec bằng Python**: thư mục `ai-learning-service/app` không
    có file `.py` nào.
  - (b) Không có checksum hay hash của phiên bản câu hỏi. Checksum chỉ có ở `content_assets` (file âm thanh, ảnh), không liên
    quan đến `answer_spec`.
  - (c) `distractors` không lộ cho học viên: đã kiểm trong code. DTO học viên của learning (`LessonResponse`,
    `PracticeSubmissionResponse`, `SubmissionResponse`) chỉ có `correctAnswer` và `explanation`. Assessment chỉ trả
    `questionSnapshot` (stem, options) và `Solution` (correctAnswer, explanation, chỉ khi đạt ngưỡng). Game-service không
    trả `answerSpecJson` (`GameItemResponse` không có trường này). Đây là kiểm tra đọc code, chưa có test tự động riêng.
- **Q4 (phạm vi): giữ 19 câu** (sau V36 là 20 câu, do thêm câu 20/003).

### Ghi chú ngoài phạm vi phase 3 (cần xem riêng)

- `game-service/JacksonGameAnswerEvaluator` không đọc khóa `correct` của spec CHOICE, nên theo đọc code thì mọi câu CHOICE
  đều bị chấm sai trong game. Lỗi này có trước phase 3 và không do `distractors` gây ra. Chưa kiểm thử, chưa sửa.
- Chưa có người hiểu IELTS duyệt từng dòng trong gói (mục A và B). Quyết định Q1–Q4 là quyết định về thiết kế; nội dung từng
  mã và từng cặp câu–mã vẫn là nháp nếu chưa có người duyệt riêng.

### Phạm vi 3b và cách lấy dữ liệu

- Chọn câu: giải thích phải nêu rõ vì sao một phương án sai là sai (ví dụ "Library là bẫy", "C không có căn cứ").
  Không đoán phương án sai từ đáp án đúng.
- Mỗi dòng trong V35 có khóa `correct` để chắc chắn không gắn nhầm vào đáp án đúng. Migration thêm khóa vào `answer_spec`
  mà không đổi đáp án, nên không làm thay đổi kết quả chấm các lần làm đã có.
- Một phương án sai không có mã nào khớp được để trống. Nhóm này đề xuất để người duyệt quyết định:
  - `CONTRADICTED_BY_PASSAGE`: phương án nói ngược lại bài (4 phương án: 26/001 A, 33/001 A, 27/015 A, 33/002 B).
  - `DETAIL_AS_MAIN_IDEA`: lấy một chi tiết làm ý chính (1 phương án: 20/003 B).
  - `WRONG_TIME_FRAME`: gắn ý với một mốc thời gian mà bài không nói (1 phương án: 27/010 B).
  Chưa thêm các mã này vào danh mục vì đó là quyết định về taxonomy.
- Với các câu có giải thích không nêu bẫy, không gắn gì. Người duyệt có thể bổ sung sau bằng migration mới.

## Sai khác so với bản gốc (đã quyết định khi làm 3a)

- Số migration là V34, không phải V31: V31 đến V33 đã dùng cho backfill độ khó và bộ câu luyện tập.
- Bộ vector mới là `docs/contracts/answer-spec-error-tags-v1-vectors.json`, không thêm vào `answer-spec-v1-vectors.json`.
  Lý do: giữ nguyên bộ vector grading đã duyệt, các test cũ không phải đổi.
- Từ giới hạn số từ là khóa tùy chọn `maxWords` trên spec `FILL`, không đổi việc chấm đúng sai.
- Không thêm migration cho assessment: bảng `error_analysis_items` đã có sẵn, chỉ thêm cách ghi.
- Learning tính `errorCodes` trong `ItemGrading` nhưng chưa trả cho học viên. Phần đó thuộc phase 4 (event log).
- `ErrorTagger` được viết riêng ở từng service như plan yêu cầu, không đưa vào `shared/`.

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
