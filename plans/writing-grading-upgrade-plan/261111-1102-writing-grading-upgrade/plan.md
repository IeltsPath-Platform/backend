---
title: "Writing grading upgrade: error coverage, data checks, explanations"
description: "Chấm Writing bắt đủ lỗi, kiểm số liệu Task 1, giải thích vì sao sai, bám tiêu chí bằng dẫn chứng và luật cứng; kết quả chấm dẫn tới hành động luyện"
status: in-progress
priority: P1
branch: "fix/grade-writting-llm"
tags: [learning-service, assessment-service, content-service, frontend, writing, llm, prompt]
blockedBy: []
blocks: []
created: "2026-11-11T04:02:00.000Z"
---

# Writing grading upgrade

Các quyết định đã được người dùng chốt ngày 2026-11-11 (mục "Quyết định đã chốt"). Chi tiết từng phase nằm ở file
`phase-NN-*.md`; khi lệch, file phase đúng về cách làm, file này đúng về phạm vi và quyết định. Chưa có code nào được viết.

## Vì sao làm

Examiner chấm thử một bài Task 1 cố tình sai nặng (24 lỗi) và đánh giá kết quả AI:

| Mặt | Điểm | Nhận xét |
| --- | --- | --- |
| Độ hợp lý của band | 8/10 | Overall 4.0 hợp lý; CC 4.0 hơi gắt |
| Phát hiện lỗi | 5/10 | Chỉ 9 mục sửa trên hơn 20 lỗi; sót cả lỗi ngược số liệu ("this number was going down", thực tế 11 → 12) |
| Chất lượng gợi ý sửa | 7/10 | Sửa đúng ngữ pháp nhưng vẫn sai nghĩa dữ liệu; không giải thích vì sao sai |

Ưu tiên của examiner: P0 độ bao phủ lỗi, P1 đối chiếu số liệu trước khi sửa, P2 giải thích lỗi, P3 chuẩn hóa band.

## Hiện trạng trong code (đã đọc, chưa chạy)

| Dữ kiện | Nơi | Hệ quả |
| --- | --- | --- |
| Tối đa 10 correction | `EssayGrader.MAX_CORRECTIONS`, contract `lesson-writing-v1.md` ("at most 10") | Lỗi thứ 11 trở đi bị bỏ |
| Prompt không yêu cầu liệt kê hết lỗi; "If no listed code applies, omit the correction" | `essay-prompt-template-v1.txt` | Model chỉ chọn lỗi tiêu biểu; lỗi không có mã thì mất |
| Danh mục lỗi gửi cho LLM là mọi mã active đã duyệt, gồm cả Reading, Listening và `SPK_*` | `InternalLearningContentController#errorTypes`, `EssayGrader.systemPrompt` | Nhiễu prompt |
| Mã ngữ pháp cho Writing chỉ có `TENSE`, `ARTICLE`, `SUBJECT_VERB_AGREEMENT`, `SENTENCE_FRAGMENT` (+ `PLURAL_FORM` thuộc `SPELLING_NUMBER`) | Content V45 | Thiếu mã cho "should going", thiếu động từ, giới từ… |
| `MAX_TOKENS = 4096`, timeout 20 s (validation tối đa 25) | `OpenAiCompatibleLlmClient`, `WritingSettings` | Tăng số lỗi dễ cắt JSON → `INVALID_GRADE`, hoặc quá timeout |
| `Correction` chỉ có `excerpt`, `suggestion`, `category`, `errorCode`; không có lý do | `WritingGrade.Correction` | Không giải thích được từng lỗi |
| API không trả `errorCode` của correction dù contract ghi có | `WritingSubmissionResponse.Correction` vs `lesson-writing-v1.md` | Frontend không gom lỗi theo dạng, không hiện `fixHint` được |
| `chart_facts` có trong prompt nhưng không có bước duyệt từng câu số liệu | template v1 | Câu đúng ngữ pháp mà sai số liệu bị sót |
| Prompt chỉ yêu cầu sửa từng đoạn trích; không bắt đối chiếu yêu cầu đề Task 2, quan điểm hay cấu trúc đoạn. Mã `NO_CLEAR_POSITION` có nhưng không gắn KP; không có mã "trả lời thiếu phần đề", "thiếu kết bài" | template v1, Content V45 | Lỗi cả bài của Task 2 (làm tụt TR nặng nhất) dễ bị sót |
| Band do LLM cho, code chỉ kiểm định dạng; số từ chỉ ghi vào prompt | `EssayGrader.parse` | Không có luật cứng (thiếu từ, lạc đề) |
| Hạn mức ngày không được trả lại khi LLM lỗi | `EssaySubmissionFlow.grade` (Speaking có `quota.release`) | Lần chấm lỗi vẫn tính vào 10 lần/ngày |
| Bài chấm không lưu tên model; không có metric | `promptVersion = rubric@version+tplv1` | Đổi nhà cung cấp không để lại dấu vết |
| Đã có bộ đo: `ai_gold_essays`, `ai_eval_runs` (model, within-half-band, MAE, QWK, per-criterion, false positive mã lỗi), `ai_prompt_activations` | Assessment V12, `AiEvaluationService` | Không cần dựng công cụ đo mới; chỉ thiếu chỉ số **độ bao phủ lỗi** |
| Eval chạy bằng grader của Assessment, không phải `EssayGrader` của Learning | `AiEvaluationService.evaluate` | Hai grader phải đổi cùng lúc, nếu không số đo không phản ánh Learning |
| Learning từ chối chấm khi phiên bản kích hoạt khác phiên bản code tính ra | `EssayGrader.grade` → `PROMPT_VERSION_UNAVAILABLE` | Deploy template v2 trước khi kích hoạt v2 sẽ làm **mọi bài không chấm được** |
| Template có 3 bản giống nhau | Learning, Assessment, `docs/contracts/`; `RubricPromptBuilderTest` so bản Assessment với contract | Sửa phải sửa cả ba (phase 0b gom về một file) |
| Ba client LLM riêng: Learning `OpenAiCompatibleLlmClient`, Assessment `OpenAiCompatibleEssayGrader`, Content `OpenAiCompatibleContentGenerator`; chỉ Learning xử lý model reasoning; không client nào đọc `usage`; Content tự retry chồng lên `ResilientCall` | ba service | Không đo được chi phí; đổi sang model reasoning làm hỏng Assessment và Content (phase 0b) |
| Grader của Assessment khác Learning ngoài template: model riêng (`ASSESSMENT_LLM_MODEL`), read timeout 45 s, không gửi `max_tokens`, không có `reasoning_effort`, danh mục lỗi không lọc | `OpenAiCompatibleEssayGrader`, `assessment-service.yaml` | Số đo eval (band, recall, p95) không phản ánh Learning |
| Eval fail cả run khi một bài ném lỗi | `AiEvaluationService.evaluate` (catch ngoài vòng lặp) | Không có tỷ lệ bài chấm hỏng |
| `activeErrorTypes()` có năm chỗ gọi: `EssayGrader`, `ErrorTypeTexts`, `SpeakingGradingContextCapture` (lọc tiền tố `SPK_`), `ApplyHumanReviewResultUseCase`, và `ContentErrorTypeClient` của Assessment | Learning, Assessment | Đổi chữ ký phải rà cả năm |
| Speaking đã có trần band: `SpeakingPracticeSettings.*BandCap`, response `bandLimits[] {reason, cap, message}` và `limited` trên tiêu chí | `SpeakingResultAssembler` | Writing theo cùng mẫu (quyết định 10) |
| Content `V122` đã dùng (`writing_rubric_low_band_descriptors`) | migration Content | Migration mới của phase 3 từ V123 |

## Mục tiêu đo được

Trên bộ bài có đánh dấu lỗi (`examiner_errors`): 5 đến 10 bài, gồm Task 1 và Task 2, nhiều mức band; bài 24 lỗi của
examiner là một bài trong bộ (quyết định 9).

1. `error_recall` trên cả bộ đạt ngưỡng đặt sau số nền (phase 1); riêng bài của examiner bắt ≥ 18 trên 24 lỗi, cả hai
   lỗi ngược số liệu đều được nêu. Bài 24 lỗi là ca kiểm, không phải thước đo duy nhất.
2. Mọi `suggestion` của Task 1 không mâu thuẫn `chartFacts` (examiner kiểm tay trên 10 bài).
3. Ngưỡng eval hiện có vẫn đạt: within-half-band ≥ 0.80, QWK ≥ 0.60.
4. `invalid_grade_rate` và p95 thời gian chấm không xấu hơn v1; p95 dưới `learning.writing.grading-timeout-seconds`.
   p95 đo bằng eval đã khớp client với Learning (quyết định 8), xác nhận lại bằng metric của Learning sau khi kích hoạt.
5. Task 2: trên 10 bài examiner kiểm tay, kiểm yêu cầu đề và quan điểm khớp nhận định của examiner ở ≥ 8 bài; mọi bài cố
   ý trả lời thiếu phần đề đều bị gắn cờ (quyết định 22).

## Phases

| Phase | Việc | Service | Phụ thuộc | Đổi contract | Status |
| --- | --- | --- | --- | --- | --- |
| 0 | [Spike đo thời gian chấm](./phase-00-latency-spike.md) | learning (không merge) | - | Không | BLOCKED: chưa có 3 bài examiner và bản nháp v2 (xem báo cáo) |
| 0b | [Thư viện LLM dùng chung, template một nguồn](./phase-00b-shared-llm-client.md) | shared, learning, assessment, content | 0 | Không (request Assessment đổi có chủ đích) | In progress |
| 1 | [Đo độ bao phủ lỗi](./phase-01-error-coverage-metric.md) | assessment, frontend | 0b | Có (admin API) | Code xong, chờ bộ bài examiner (số nền) |
| 2 | [Sửa nhỏ không đổi prompt](./phase-02-quick-fixes-without-prompt-change.md) | learning | 0b | Không (khớp lại contract) | Xong, chờ 0b cho metric LLM |
| 3 | [Danh mục lỗi Writing](./phase-03-writing-error-catalog.md) | content, learning, assessment | duyệt chuyên gia | Có | Code xong, chờ chuyên gia duyệt mã mới |
| 4 | [Prompt v2](./phase-04-prompt-v2.md) | learning, assessment, docs | 1, 2, 3 | Có | Pending |
| 5 | [Kiểm số liệu Task 1](./phase-05-task-1-data-checks.md) | learning, assessment (template) | 4 | Có | Pending |
| 6 | [Luật cứng cho band](./phase-06-band-caps.md) | learning (domain) | 4, examiner duyệt ngưỡng | Có | Pending |
| 7 | [Response cho giao diện kết quả](./phase-07-result-view-response.md) | learning, frontend | 2, 4, 5, 8 | Có | Pending |
| 8 | [Kiểm yêu cầu đề Task 2](./phase-08-task-2-requirement-checks.md) | learning, assessment (template) | 4 | Có | Pending |

Thứ tự: 0 → 0b → 1 và 2 song song → 3 → 4 → 5 và 8 song song → 7; 6 độc lập sau 4 (hai luật Task 2 cần cờ của phase 8). Phase 0 có thể đổi quyết định 2, nên phase 1 trở đi
chỉ bắt đầu khi đã ghi kết luận của phase 0. Phase 1, 3 và 6 có phần chờ người làm nội dung (đánh dấu bộ bài, duyệt mã
lỗi, duyệt ngưỡng); code của chúng làm trước được, dữ liệu vào sau.

Ngoài phạm vi đợt này (plan riêng nếu cần): chấm lại bài sát ngưỡng `passBand` và chuyển Mentor khi hai lần chấm lệch
trên 1 band; chấm bất đồng bộ theo mẫu hàng job của Assessment; hiển thị `reason`/`dataChecks` cho bài thi gate; theo dõi
tiến bộ Writing qua nhiều bài (quyết định 19; plan `plans/261111-1557-writing-progress-tracking/`); câu
luyện do AI tự tạo (quyết định 18, không làm).

## Quyết định đã chốt (không đảo; muốn đổi phải hỏi người dùng)

1. **Số liệu đúng của Task 1 được hiện cho học viên sau khi nộp**, chỉ cho câu họ viết sai (`dataChecks.expected`).
   `chartFacts` nguyên văn vẫn không bao giờ được trả. Phase 5 sửa câu tương ứng ở `AGENTS.md` §3.8 và
   `lesson-writing-v1.md` thành ngoại lệ có điều kiện.
2. **Giữ một request LLM mỗi bài.** Tăng giới hạn token, đo p95 trên bộ gold; chỉ quay lại hỏi người dùng về việc tách
   request nếu số đo cho thấy vượt timeout.
3. **Lọc mã lỗi theo kỹ năng bằng cột `skill` ở Content** (migration mới), không lọc theo quy ước đặt tên.
4. **Có áp trần band bằng luật cứng trong code**; bảng ngưỡng do examiner duyệt trước khi bật. Lưu cả band gốc của LLM.
5. **Thi gate của Assessment chỉ dùng chung template v2** (để bộ đo phản ánh đúng). Không thêm `reason`, `dataChecks`
   hay trần correction mới vào response của bài thi trong đợt này.
6. **Mã dự phòng (`*_OTHER`) hiện cho học viên** với nhãn nhóm chung, nhưng không gắn KP: không ghi evidence, không tạo bài ôn.

Chốt thêm sau review cùng ngày:

7. **Đo thời gian chấm trước mọi phase** (phase 0). Kết quả spike quyết định giữ hay xem lại quyết định 2.
8. **Eval phải khớp Learning:** Assessment lọc danh mục theo skill như Learning; grader của Assessment gửi request cùng
   cách với Learning (`max_tokens`/`max_completion_tokens`, `reasoning_effort`) và cắt theo cùng timeout; run eval ghi
   `model` và `reasoning_effort`; Learning so với cấu hình của mình và báo khi lệch.
9. **Bộ bài đo có 5 đến 10 bài được examiner đánh dấu lỗi**, gồm Task 1 và Task 2. Ngưỡng `error_recall` tính trên cả bộ.
10. **Trả lại hạn mức ngày chỉ khi lỗi phía hệ thống** (`LLM_UNAVAILABLE`, `AI_BUSY`, `RUBRIC_UNAVAILABLE`,
    `PROMPT_VERSION_UNAVAILABLE`). `INVALID_GRADE` vẫn tính lượt, để bài cố ý làm hỏng JSON không gọi LLM vô hạn.
11. **Eval không fail cả run vì một bài lỗi:** bài lỗi đếm vào `invalid_grade_rate`, và `passed` thêm điều kiện tỷ lệ này
    không vượt ngưỡng.
12. **Giữ đủ 7 phase** trong đợt này.
13. **Trần band của Writing theo mẫu Speaking:** cùng cách cấu hình, cùng hình dạng response `bandLimits` và `limited`.

Chốt thêm sau góp ý về phản hồi cho học viên (cùng ngày):

14. **Top 3 vấn đề cần sửa trước, kèm "Cần làm": AI chọn, code dự phòng.** Prompt v2 trả tối đa 3 `priorities`, trích dẫn
    phải có thật trong bài. Thiếu hoặc sai thì code lấy 3 nhóm lỗi theo số lượng × `severity`, "Cần làm" lấy từ `fixHint`.
15. **Ví dụ tương tự: mỗi dạng lỗi một ví dụ**, gắn với mã lỗi có trong bài, không lặp cho mỗi lần mắc.
16. **Mỗi correction có `certainty`** (`ERROR` hoặc `SUGGESTION`). Gợi ý diễn đạt không ghi evidence và không tính báo sai trong eval.
17. **Không dùng số lỗi đếm được để tính band:** bỏ luật `GRAMMAR_DENSITY` ở phase 6.
18. **Không làm câu luyện do AI tự tạo.** Nút luyện vẫn dẫn tới bộ luyện đã được người duyệt theo mã lỗi.
19. **Theo dõi tiến bộ qua nhiều bài** (lỗi hay gặp, xu hướng band từng tiêu chí, số lỗi đã khắc phục): plan riêng.

Chốt thêm về Task 2 (cùng ngày; bài examiner thử là Task 1 nên các phần trên thiên về Task 1):

20. **Thêm kiểm yêu cầu đề Task 2 đủ ba phần** (phase 8): tách đề và đối chiếu từng yêu cầu, quan điểm (có, nhất quán),
    dàn ý từng đoạn. Giao diện có danh sách kiểm và "dàn ý bài của bạn".
21. **Hai luật trần Task 2 vào bảng ứng viên của phase 6:** trả lời thiếu phần đề, không có quan điểm khi đề yêu cầu.
    Dựa vào cờ có trích dẫn, không dựa vào số lỗi; examiner duyệt, tắt mặc định.
22. **Chất lượng kiểm Task 2 do examiner kiểm tay 10 bài**, không thêm chỉ số tự động.

Chốt thêm về tầng LLM (cùng ngày):

23. **Thêm module `shared/common-llm`** (phase 0b): một client OpenAI-compatible cho Learning, Assessment và Content, có
    metric thời gian và token theo service/model. Mỗi service giữ port và biến môi trường riêng. Prompt, rubric, parser
    nghiệp vụ không vào shared.
24. **Template chấm essay một nguồn:** file duy nhất ở `docs/contracts/`, Maven chép vào resources của Learning và
    Assessment lúc build.
25. **Làm phase 0b sau phase 0 và trước phase 1**, để phase 1 không phải chép luật dựng request sang Assessment.
26. **Content bỏ vòng retry riêng** khi chuyển sang `common-llm`; lỗi thì bản nháp `FAILED`, author chạy lại.

## Rủi ro

| Rủi ro | Giảm thiểu |
| --- | --- |
| Deploy v2 khi activation còn v1 làm ngừng chấm | Grader chọn template theo activation (phase 4) |
| Output dài bị cắt hoặc quá timeout | Đo ngay ở phase 0; tăng `MAX_TOKENS`; eval cắt theo timeout của Learning ở phase 1 và 4 |
| Eval đo một đường chấm khác Learning | Quyết định 8: cùng danh mục, cùng cách gửi request, cùng timeout, ghi model |
| Prompt chỉ tốt cho bài của examiner | Quyết định 9: ngưỡng trên bộ 5 đến 10 bài |
| Nhiều correction làm đổi evidence (`FAIL` từ correction) và mastery | Mã dự phòng không gắn KP; so phân bố evidence trước và sau trên bộ gold |
| Bắt nhiều lỗi hơn kéo theo báo sai | Theo dõi `error_code_false_positive` đã có cùng `error_recall` |
| Ba bản template lệch nhau | Phase 0b: một file ở `docs/contracts/`, chép vào hai service lúc build |
| Đổi request của chấm gate (Assessment) khi gom client | Phase 0b: run eval v1 trước và sau; `max_tokens` đặt theo số đo phase 0 |
| Model rẻ (DeepSeek) không theo nổi prompt v2 | Eval ghi `model`; mỗi lần đổi model phải chạy eval và kích hoạt lại |

## Pattern áp dụng (`AGENTS.md` §3.9)

- LLM vẫn qua `ResilientCall("llm", WRITE)`, ngoài transaction và khóa; không thêm retry.
- Hạn mức ngày giữ nguyên: vẫn một lượt mỗi bài.
- Đổi response và admin API: sửa `docs/contracts/lesson-writing-v1.md`, `essay-revision-v1.md` và template trong cùng PR.
- Migration mới, không sửa migration cũ; log chỉ có id và mã lỗi, không có essay hay prompt.
- Test không gọi LLM thật.
