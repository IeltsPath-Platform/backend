---
title: Writing rubric v3, mã lỗi Writing và prompt template v3
status: pending
branch: fix/writing-correction-text-and-template-v2
created: 2026-11-13
source: đề xuất "de-xuat-rubric-v3-va-ma-loi" (Phần A rubric, Phần B mã lỗi); docs/llm-grading-log/llm-writing-grading-prompt-iteration-log-261112.xlsx (C01–C12, I03, I09, I18)
---

# Writing rubric v3, mã lỗi Writing và prompt v3

Đưa đề xuất rubric v3 và mã lỗi vào DB của content-service, sửa code để model thấy định nghĩa của mã (không thấy
`fix_hint`) và chỉ thấy mã của task đang chấm. Sau đó thêm template v3 (C01–C12 của file log), đo trên bộ bài test
và chỉ đổi template mặc định khi đạt mục tiêu.

## Trạng thái thực tế (2026-11-14)

Bảng quyết định và phase bên dưới là thiết kế ban đầu. Đã làm khác như sau; code và migration là nguồn đúng.

- V132 (mã lỗi + định nghĩa + `writing_tasks`) và V133 (rubric v3 chèn **DRAFT**, v2 vẫn ACTIVE; khác D3) đã viết theo spec của
  product owner, có test Testcontainers. V134 gắn KP của đề cho 24 đề `PT-WRITING-01..12`.
- Template `v3` và `default-template` vẫn `v2`; chưa kích hoạt rubric v3 vì mới đo trên một đề Task 1.
- Thêm trần band tính từ correction (`WritingErrorDensity`, tắt mặc định bằng `band-caps-enabled`; trần GRA để trống vì
  không tách được band 4 và 5).
- Khối `judgments` chỉ dùng KP của đề (`kpj3`), thay cho KP con theo tiêu chí (`kpj2`); xem `AGENTS.md` §3.8 và
  `docs/contracts/lesson-writing-v1.md`.
- Chưa làm: đọc `model_selectable` trong code, tạo bằng chứng từ `taskChecks`, `withNextBands` bỏ band .5, đo Task 2,
  đổi `default-template` sang `v3`.

## Quyết định đã chốt (2026-11-13)

| # | Quyết định | Lệch so với đề xuất |
|---|---|---|
| D1 | Phạm vi: migration và code đọc cột mới. **Không** làm: tạo bằng chứng từ `taskChecks`, `{{word_count}}`/`{{paragraph_count}}`, `withNextBands` bỏ band .5 | B1.5, A3 (câu đếm từ), A2 (band tiếp theo) để sau |
| D2 | `SPELLING`, `PLURAL_FORM` giữ nguyên cho bài khách quan (ErrorTagger gắn tự động cho Listening/Reading); bỏ `WRITING` khỏi `skills`; thêm `WRITING_SPELLING`, `WRITING_PLURAL_FORM` | B3 đề xuất sửa thẳng mã cũ |
| D3 | Rubric @3 chèn ACTIVE, @2 RETIRED (giống V122). Nơi đã ghim prompt (`ai_prompt_activations`) vẫn dùng @2 tới khi kích hoạt lại | B6.4 muốn đo trước |
| D4 | `OFF_TOPIC` → KP `TR_W_OPINION` (`M55_WRITING_T2_BASICS_KP` không tồn tại); `LINKER_MISUSE` → `G55_LINKING_WORDS` | |
| D5 | Vì D1, `T1_NO_OVERVIEW`, `NO_CLEAR_POSITION`, `T2_PARTIAL_ANSWER` **vẫn gửi cho model** (nếu bỏ thì mất bằng chứng mastery) | B1.5 |
| D6 | C08 (ý kiến cá nhân Task 1, câu sửa rỗng) dùng `T1_IRRELEVANT_CONTENT` thay `TASK_OTHER` | sheet De_xuat_prompt C08 |
| D7 | Plan v3 gồm sửa backend I18, I03, I09; không làm I17 (lưu token) | |
| D8 | Gộp vào plan này; `default-template` giữ `v2` tới khi phase 5 đạt mục tiêu | |

## Phases

| # | Phase | Service | Phụ thuộc | Trạng thái |
|---|---|---|---|---|
| 1 | [Migration mã lỗi Writing](./phase-01-error-code-migration.md) | content | — | pending |
| 2 | [Migration rubric @3](./phase-02-rubric-v3-migration.md) | content | — | pending |
| 3 | [Code đọc định nghĩa và task của mã](./phase-03-graders-read-definitions.md) | content, learning, assessment | 1 | pending |
| 4 | [Template v3, kpj3, sửa backend I18/I03/I09](./phase-04-prompt-template-v3.md) | learning, assessment, docs | 1, 3 | pending |
| 5 | [Đo trên bộ bài test, đổi mặc định](./phase-05-measure-and-switch-default.md) | scripts, xlsx, config-repo | 1–4 | pending |

Phase 1 và 2 độc lập (hai file migration). Phase 3 cần cột của phase 1. Nên release 2 và 3 cùng lúc, vì phần chung của
rubric (A3) nằm trong `system_prompt` và chỉ được đưa vào prompt khi có code phase 3. Phase 4 cần mã
`T1_IRRELEVANT_CONTENT` (phase 1). Phase 5 đo từng bước một (mã lỗi → rubric → template) theo quy tắc của file log.

## Tiêu chí hoàn thành

- `mvn -pl services/content-service verify` xanh, gồm migration test mới cho V132, V133.
- Test đơn vị của `EssayGrader` và `RubricPromptBuilder`: prompt Task 1 không chứa mã chỉ dùng cho Task 2 và ngược lại;
  dòng mã dùng `model_definition` khi có, `fix_hint` khi trống.
- Listening/Reading: `SPELLING`, `PLURAL_FORM` giữ nguyên KP, fix_hint và nhóm (test khẳng định).
- Rubric @3: 4 tiêu chí × 18 band cho mỗi task; đúng một rubric ACTIVE cho mỗi task.
- Template v3 và kpj3 dựng được, parse ra đủ reason, certainty, taskChecks; v1 và v2 không đổi prompt.
- Phase 5 đạt mục tiêu của sheet `Tong_hop` thì mới đổi `default-template` sang `v3`.

## Rủi ro chính

- @3 ACTIVE thay đổi kết quả chấm ở môi trường không ghim prompt. Muốn quay lại thì viết migration đặt @2 ACTIVE.
- Câu rubric và định nghĩa mã chưa qua examiner (đề xuất A9 còn 4 câu hỏi mở). Sửa sau bằng @4 / migration mới.
- `WritingBandCaps` (mặc định tắt) cũng áp trần: bài ngắn 5.0, thiếu overview, thiếu phần đề, không có lập trường.
  Số trong rubric @3 phải khớp với cấu hình đó.

## Câu hỏi còn mở

1. Mastery (B6.3): bằng chứng cũ của `TENSE` nằm ở `G60_PERFECT_TENSES`. Plan mặc định giữ nguyên, không rebuild.
2. Bốn câu hỏi A9 cho examiner (trần số từ, ý kiến cá nhân Task 1 là 6 hay 5, trần CC Task 1, General Training).
   Plan chèn đúng như đề xuất.
3. Admin UI chưa sửa hay kích hoạt được rubric Writing 18 band (`validateForActivation` chỉ nhận 4.0–9.0). Ngoài phạm vi.
4. Bài Task 2 và bài band 6–7 cho `Bai_test` lấy từ đâu (bài nộp thật hay examiner cung cấp).
5. I05, I06 (frontend: correction chồng nhau, dòng "3 câu số liệu đúng") nằm ở repo frontend, không thuộc plan này.
