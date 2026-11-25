# Phase 4: Template prompt v3, kpj3 và sửa backend đi kèm

## Context

- Nguồn: `docs/llm-grading-log/llm-writing-grading-prompt-iteration-log-261112.xlsx`, sheet `Phien_ban_prompt`
  (mục 3 "System — tplv3" và 4b "kpj3", nguyên văn từng dòng), `De_xuat_prompt` (C01–C12), `Van_de` (I03, I09, I18).
- Phụ thuộc phase 1: C08 dùng `T1_IRRELEVANT_CONTENT` (D6).
- `default-template` giữ `v2` (D8). v3 chỉ được dùng khi ghim qua activation, hoặc khi phase 5 cho đổi mặc định.

## File

**Template**
- Tạo `docs/contracts/essay-prompt-template-v3.txt`: chép v2 rồi áp các dòng tplv3 của sheet. Có hai chỗ sửa:
  - C08 dùng `T1_IRRELEVANT_CONTENT` thay `TASK_OTHER`;
  - giữ nguyên dấu `[[TASK_1]]`/`[[TASK_2]]` và dòng ghi chú `#` đầu file như v2.
- Tạo `services/learning-service/src/main/resources/essay-kp-judgment-v3.txt`: kpj2 với dòng đầu thay bằng C12.

**learning-service**
- `application/service/EssayGrader.java`
  - thêm `TEMPLATE_VERSION_3`, `PROMPT_TEMPLATE_3`; `isKnownTemplate` nhận `v3`;
  - thay mọi chỗ `TEMPLATE_VERSION_2.equals(templateVersion)` (cờ `secondTemplate` trong `parse`, chọn template trong
    `systemPrompt`) bằng một helper `hasTaskChecks(version)` đúng cho v2 và v3. Nếu không, v3 bị đọc như v1 và mất
    reason, certainty, taskChecks;
  - **I18** `withDataCorrections`: correction tự thêm giữ `excerpt` và mã để highlight, nhưng `suggestion` rỗng, còn
    `expected` (tiếng Việt) đưa vào `reason`. Bản đã sửa không còn chèn tiếng Việt;
  - **I03**: `suggestion` chứa ký tự tiếng Việt (chữ có dấu ngoài bảng Latin cơ bản) thì đổi thành rỗng và giữ
    correction. Áp cho mọi template;
  - **I09**: `truncate` cắt ở ranh giới câu gần nhất trước giới hạn, không có thì ở khoảng trắng. Thêm
    `MAX_SUGGESTION_CHARS = 600` cho `suggestion`, vì C02 viết lại cả đoạn số liệu. Nhận xét tiêu chí giữ 300
    (C06 đã yêu cầu dưới 250).
- `application/service/EssayKnowledgeJudge.java`: chọn `essay-kp-judgment-v3.txt` và `kpj3` khi template là v3,
  còn lại giữ kpj2. `VERSION` thành hàm theo template.
- `application/service/WritingSettings.java`: `@Pattern("v1|v2|v3")`.
- `pom.xml`: không cần sửa, đã chép mọi file `essay-prompt-template-*.txt`.

**assessment-service**
- `infrastructure/client/RubricPromptBuilder.java`: `TEMPLATE_VERSION_3`, `isKnownTemplate` nhận v3, chọn template.
  Không có phần KP con và không parse correction, nên không cần I03/I09/I18.
- Nơi kiểm `promptVersion` khi kích hoạt (`ai_prompt_activations`): kiểm lại có dùng `isKnownTemplate` không.

**Tài liệu**
- `docs/contracts/lesson-writing-v1.md`: thêm `tplv3`, `kpj3`; ghi rằng correction do backend thêm có `suggestion`
  rỗng; ghi giới hạn mới của suggestion.
- `AGENTS.md` §3.8 đang ghi "`promptVersion` vẫn `+tplv1`" và "`kpj2`": cập nhật thành v1/v2/v3 và kpj2/kpj3.

## Test

- `EssayGrader`: prompt v3 Task 1 có C02, C08, C09 và không có phần Task 2; prompt v3 Task 2 không có phần Task 1.
- Parse output v3 (JSON theo thứ tự `taskChecks → corrections → criteria`) ra đủ reason, certainty, taskChecks.
- I18: dataChecks WRONG không có correction tương ứng → correction tự thêm có `suggestion` rỗng, `reason` = expected.
- I03: suggestion "Bỏ đoạn kết bài…" → rỗng.
- I09: chuỗi 320 ký tự có dấu chấm ở ký tự 250 → cắt còn 250; suggestion 550 ký tự giữ nguyên.
- `EssayKnowledgeJudge`: v3 nối kpj3 và lưu `judgmentVersion = kpj3`; v2 vẫn kpj2.
- `WritingSettings` nhận v3; `RubricPromptBuilder` dựng được v3.
- Chạy `mvn -q -pl services/learning-service,services/assessment-service -am test`.

## Rủi ro

- I03 và I09 đổi hành vi cho cả bài chấm bằng v2. Đây là sửa lỗi hiển thị, không đổi band. Ghi rõ trong PR.
- C11 (đưa `criteria` ra sau `corrections`) có thể làm band dịch. Phase 5 đo trước khi đổi mặc định.
- Đổi file v2 hoặc kpj2 sẽ đổi prompt của bài đang ghim. Chỉ tạo file mới.
