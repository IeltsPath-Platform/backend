---
phase: 4
title: "Prompt v2"
status: pending
priority: P1
dependencies: [1, 2, 3]
---

# Phase 4: Prompt v2 (learning, assessment, docs)

## Overview
Template mới yêu cầu liệt kê mọi lỗi kèm lý do, và bắt dẫn chứng trước khi cho band. Đây là phase đổi hành vi chấm nhiều
nhất, nên đi qua đúng cửa đã có: chạy eval, đạt ngưỡng, rồi mới kích hoạt.

## Requirements
- **Template** `docs/contracts/essay-prompt-template-v2.txt`, một file duy nhất, Maven chép vào hai service lúc build
  (phase 0b). File v1 **giữ nguyên**. Khác v1:
  - Corrections: "liệt kê mọi lỗi, theo thứ tự xuất hiện, mỗi lỗi một mục, không gộp, không chọn lọc"; thêm `reason`
    (một câu tiếng Việt nêu quy tắc bị vi phạm, thuật ngữ IELTS giữ tiếng Anh); lỗi không có mã cụ thể dùng mã dự phòng
    của nhóm thay vì bỏ.
  - Mỗi correction có `certainty`: `ERROR` khi sai quy tắc, `SUGGESTION` khi câu gốc chấp nhận được nhưng có cách diễn đạt
    tốt hơn (quyết định 16). Prompt yêu cầu không gán `ERROR` cho chỗ có nhiều cách viết hợp lệ.
  - `priorities` (tối đa 3, quyết định 14): `{title, explanation, excerpts (1 đến 3 trích dẫn nguyên văn), action,
    errorCodes}`, chọn theo tác động tới band chứ không theo số lần mắc; được phép là vấn đề nội dung (sai xu hướng, thiếu
    overview) chứ không chỉ ngữ pháp. `title`, `explanation`, `action` bằng tiếng Việt.
  - `errorExamples` (quyết định 15): một mục cho mỗi mã lỗi có trong `corrections`, `{errorCode, wrong, right}`: một câu
    ngắn **khác câu của học viên** minh họa cùng lỗi và cách sửa.
  - Mỗi tiêu chí trong JSON theo thứ tự: `code`, `evidence` (1 đến 3 trích dẫn nguyên văn), `descriptor` (band descriptor
    khớp nhất, chép từ rubric), rồi mới `band`, `comment`.
  - Giữ nguyên: câu coi `<essay>` là dữ liệu, danh sách mã cho phép, quy tắc `chart_facts`, ngôn ngữ phản hồi.
  - `essay-kp-judgment-v2.txt` không đổi (vẫn nối sau).
- **Chọn template theo phiên bản đang kích hoạt**, ở cả `EssayGrader` (Learning) và `RubricPromptBuilder` (Assessment):
  - activation `…+tplv1` → template v1 và parser v1; `…+tplv2` → v2. Phiên bản lạ → `PROMPT_VERSION_UNAVAILABLE` như hiện nay.
  - Chưa có activation (`expectedVersion == null`) → giữ v1.
  - Eval của Assessment nhận tham số `templateVersion` để chạy v2 trước khi nó được kích hoạt.
- **Parser v2 (Learning):**
  - Trần correction lấy từ `learning.writing.max-corrections` (mặc định **30**, `@Min(1) @Max(50)`), thay hằng số 10.
  - `Correction.reason` (≤ 300 ký tự, tùy chọn). Thiếu `reason` không làm hỏng correction.
  - `evidence` của tiêu chí: trích dẫn không có nguyên văn trong bài thì bỏ trích dẫn đó, **vẫn giữ band**. Thiếu hẳn
    `evidence` không phải `INVALID_GRADE` (để một lỗi định dạng nhỏ không làm học viên mất lượt), nhưng đếm vào metric
    `learning.writing.evidence_missing`.
  - Lưu `evidence` và `descriptor` trong JSON bài chấm; đưa ra API ở phase 7.
  - `certainty` thiếu hoặc lạ → `ERROR` (giữ hành vi v1). Correction `SUGGESTION` được lưu và trả cho học viên nhưng
    **không** tạo `FAIL` cho KP con, không vào `error_code` của event essay, không tính vào `error_code_false_positive`
    hay `error_recall` của eval.
  - `priorities`: bỏ trích dẫn không có nguyên văn; bỏ mã lỗi không có trong `corrections`; mục không còn trích dẫn nào
    thì bỏ; quá 3 thì cắt. Thiếu hoặc rỗng không phải `INVALID_GRADE`; phase 7 dùng phương án dự phòng.
  - `errorExamples`: bỏ mục có mã không xuất hiện trong `corrections`, trùng mã thì giữ mục đầu; mỗi câu ≤ 200 ký tự.
    Thiếu không phải `INVALID_GRADE`.
- **Giới hạn đầu ra:** `learning.llm.max-tokens` và `assessment.llm-grading.max-tokens` (trường `maxTokens` của
  `LlmEndpoint`, phase 0b; mặc định 8192, điều chỉnh theo token ra đo ở phase 0), hai giá trị bằng nhau. Timeout theo
  kết luận của phase 0.
- **Assessment (theo quyết định 5):** chỉ đổi template và phần parse tối thiểu để chấm được v2 (đọc `band` ở vị trí
  mới, bỏ qua `reason`/`evidence`). Response của bài thi gate không đổi.
- **Ngưỡng kích hoạt v2** (ngoài within-half-band ≥ 0.80, QWK ≥ 0.60 và `invalid_grade_rate` ≤ `max-invalid-rate`):
  `error_recall` **trên cả bộ 5 đến 10 bài** cao hơn số nền v1 rõ rệt, và `error_code_false_positive` không tăng quá mức
  thỏa thuận. Run phải cùng model và `reasoning_effort` với Learning. Con số cụ thể điền vào đây sau phase 1; chưa đủ bộ
  bài thì chưa đặt.

## Related Code Files
- Learning: `application/service/EssayGrader.java`, `application/service/WritingSettings.java`, `domain/vo/WritingGrade.java`,
  `infra/config-server/config-repo/learning-service.yaml`
- Assessment: `infrastructure/client/RubricPromptBuilder.java`, `OpenAiCompatibleEssayGrader.java`,
  `application/service/AiEvaluationService.java`
- Docs: `docs/contracts/essay-prompt-template-v2.txt`, `lesson-writing-v1.md` (trần 10 → cấu hình, `reason`), `essay-revision-v1.md` nếu nhắc trần
- Tests: snapshot prompt v1 không đổi một ký tự; parse v2 (đủ trường, thiếu `reason`, `evidence` sai; `priorities` có
  trích dẫn giả, quá 3 mục, mã lỗi lạ; `errorExamples` trùng mã; `certainty` thiếu); `SUGGESTION` không sinh evidence; JSON bài chấm v1 vẫn
  đọc được; chọn template theo activation; classpath của cả hai service có đúng file v2 của `docs/contracts/`

## Implementation Steps
1. Baseline test hai module.
2. Test đỏ cho chọn template theo activation (đây là phần chống ngừng chấm; làm trước).
3. Viết template v2; người dùng và examiner đọc duyệt câu chữ **trước** khi code parser.
4. Test đỏ parser v2 → code Learning → code tối thiểu Assessment.
5. Deploy code (activation vẫn v1: hành vi chấm không đổi, kiểm bằng metric phase 2).
6. Chạy eval v2 trên bộ gold. So với số nền: band, `error_recall`, false positive, `invalid_grade_rate`, p95.
7. Đạt → admin kích hoạt v2 qua `POST /api/assessments/admin/ai-prompt-activations`. Không đạt → sửa template, lặp bước 6.
8. Theo dõi metric 48 giờ; quay lui bằng cách kích hoạt lại run v1.

## Success Criteria
- [ ] Deploy code v2 khi activation còn v1 không đổi kết quả chấm.
- [ ] `error_recall` trên cả bộ đạt ngưỡng; riêng bài của examiner bắt ≥ 18 trên 24 lỗi.
- [ ] Ngưỡng band vẫn đạt; `invalid_grade_rate` và p95 không xấu hơn v1, p95 dưới timeout.
- [ ] Mỗi correction v2 có `reason` ở ≥ 95% trường hợp trên bộ gold.
- [ ] `priorities` hợp lệ ở ≥ 90% bài trên bộ gold; examiner xác nhận Top 3 hợp lý trên bài của mình (có vấn đề sai xu hướng).

## Risk Assessment
- **Quá timeout hoặc JSON bị cắt:** đã đo ở phase 0. Nếu template sau khi examiner sửa dài hơn bản nháp đáng kể, hoặc
  p95 của eval (cắt theo timeout của Learning) vượt timeout, thì quay lại hỏi người dùng như bảng của phase 0.
- **Evidence và mastery đổi:** nhiều correction hơn nghĩa là nhiều `FAIL` từ correction hơn cho KP con. So phân bố evidence
  trước và sau trên bộ gold; mã dự phòng không gắn KP nên không góp phần.
- **Model yếu không theo nổi v2:** activation gắn với run có ghi `model`; đổi `LEARNING_LLM_MODEL` mà không chạy lại eval là
  ngoài quy trình. Ghi rõ điều này vào `docs/ai-policy.md`.
- **Hai service lệch template:** cùng một file nguồn nên không lệch nội dung; vẫn deploy Learning và Assessment cùng đợt
  vì bản build cũ của một bên vẫn mang template cũ.
