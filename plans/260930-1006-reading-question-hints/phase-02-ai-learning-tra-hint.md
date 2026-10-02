---
phase: 2
title: "AI Learning: trả hint"
status: completed
priority: P2
dependencies: [1]
effort: "1 ngày"
---

# Phase 2: AI Learning: trả hint

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java (plan `261001-1228`). Mọi tên file, lệnh và API Python dưới đây đọc theo [bảng ánh xạ](../260930-2057-mvp-reading-writing-listening-roadmap/python-to-java-mapping.md); luật nghiệp vụ, mã lỗi và test case giữ nguyên.

## Kết quả (2026-10-02)

- `LessonHintPolicy` thuần domain: FILL/CHOICE hợp lệ, ≥3 options hoặc TFNG thiếu/rỗng; 1–2 options và spec không chấm được không mở hint. Giữ quy ước CHOICE mặc định của grader cho spec legacy thiếu `type`.
- `LearningProgressStore`/`JdbcLearningProgressStore` đọc câu sai từ mọi `response.results` trong một query theo user/bài/khối, tách khỏi evidence lần nộp đầu. Dùng index có sẵn trong `V1__learning_schema.sql`; không migration mới, không HTTP thêm trong vòng lặp.
- `LearnLessonUseCase`, result và DTO thêm `hint` nullable luôn hiện ở GET/POST. Câu đã mở giữ hint khi lần sau đúng nhưng khối vẫn trượt; khối từng đạt không mở lại hint khi lần sau trượt. Replay giữ nguyên response cũ, kể cả sau khi đạt hoặc Content sửa.
- Port Content đọc hint bài học; mapping gói bỏ qua hint. Review dùng DTO chung, hint luôn null. Evidence chỉ lần nộp đầu và mastery giữ nguyên; không ghi `hints_used`.
- Baseline: 175 Learning + 8 common-security, 0 fail/error/skip. RED thiếu policy trước code; focused 76 pass. Regression `mvn -q -pl services/learning-service -am test`: 183 Learning + 8 common-security, 0 fail/error/skip, Docker/Testcontainers thực chạy. Assertion persistence cuối: 1 pass. Reviewer không finding; không chạy full reactor/live E2E.

<!-- Updated: Validation Session 3 - bỏ hints_used; query tập đã mở là query riêng -->

<!-- Updated: Validation Session 1 - luật "đã mở thì giữ" cho cả response nộp và GET; lọc dạng câu đủ điều kiện; index chuyển vào V11 của 1640 -->
<!-- Updated: Validation Session 2 - hint_eligible: CHOICE từ 3 phương án và TFNG → True; chỉ 2 phương án → False -->

## Context Links

- Plan 1640 phase 6: thứ tự nộp khối (`phase-06-ai-learning-api-bai-hoc-va-bai-on.md:30-42`), `GET /lessons/{id}` (`:66-69`),
  schema V11 (`:108-114`, có INDEX(`user_id`, `lesson_id`, `block_id`) của `lesson_exercise_submissions`), test allowlist (`:175`)
- Bản Java hiện tại: `V1__learning_schema.sql` đã có index `(user_id, lesson_id, block_id)`; schema mô tả tại DATABASE_V5 §7.4. Tham chiếu V11/Python phía dưới là thiết kế lịch sử.
- Không ghi `hints_used` (Validation Session 3); Learning Java dùng `kp_evidence`, không thay schema hoặc công thức mastery.

## Overview

Câu đủ điều kiện mà học viên đã làm sai trong khối chưa đạt thì có `hint`, ở cả response nộp khối và `GET /lessons/{id}`, tới khi khối
đạt. Plan này không đổi cách ghi bằng chứng.

## Requirements

**Câu đủ điều kiện** — hàm thuần `hint_eligible(question) -> bool` trong `app/lessons/hints.py`, theo contract:
- `answerSpec.type = FILL` → True;
- `answerSpec.type = CHOICE` và `len(options or []) >= 3` → True;
- `answerSpec.type = CHOICE` và `options` rỗng (True/False/Not Given, 3 lựa chọn cố định) → True (phòng thủ: seed ghi đủ 3
  `options` cho TFNG nên đã rơi vào nhánh trên);
- còn lại (chọn 2 phương án, spec không chấm được) → False.

**Tập câu đã mở gợi ý** của (user, bài, khối) = các `questionVersionId` có `correct = false` trong `response.results` của mọi lần nộp
đã lưu của khối đó. Đọc bằng một query trên connection của path transaction (dùng index V11 của 1640):
```sql
SELECT s.block_id, r->>'questionVersionId'
FROM lesson_exercise_submissions s, jsonb_array_elements(s.response->'results') r
WHERE s.user_id = %s AND s.lesson_id = %s AND (r->>'correct')::boolean = false
```
Đây là query riêng trong `app/lessons/store.py`: `wrong_kps` của 1640 chỉ đọc lần nộp đầu, còn tập này đọc mọi lần nộp.

**Gợi ý hiện cho câu** = câu đủ điều kiện, content có `hint`, khối chưa đạt, và câu nằm trong tập đã mở.

**Nộp khối** (chen vào thứ tự của 1640, không đổi thứ tự cũ):
1. Tra `requestId` như cũ. Trùng thì trả `response` đã lưu, đã chứa `hint`.
2. Trước khi chấm: đọc tập câu đã mở của khối (`opened_before`).
3. Chấm như cũ. `opened_after = opened_before ∪ câu sai lần này`.
4. Khối chưa đạt: mỗi câu có `hint` theo luật trên với `opened_after` (câu vừa sai và câu từng sai, kể cả lần này đúng); còn lại `null`.
   Khối đạt: mọi câu `hint = null`, thêm lời giải như 1640.
5. Ghi bằng chứng như 1640 (chỉ lần nộp đầu của khối), không đổi.
6. Lưu `response` (có `hint`) như cũ.

**`GET /lessons/{id}`:**
- Câu hỏi thêm `hint` vào danh sách trường cho phép, **luôn có key**, giá trị theo luật trên (khối chưa đạt, tập đã mở của khối).
  Key luôn có mặt để tập key cố định; `null` không cho biết câu có gợi ý hay không.
- Khối đã đạt: `hint = null`, có `solutions` như 1640.

**Bài ôn:** nếu `results[]` của bài ôn dùng chung DTO với nộp khối thì `hint` luôn `null`; nếu DTO riêng thì không có key. Không đọc
gợi ý cho gói.


## Architecture

```text
POST /lessons/{id}/exercises/{blockId}/submissions
  path tx: requestId đã có? → trả response cũ
           opened_before ← submissions cũ của khối (1 query)
           chấm → opened_after → results + hint (khối chưa đạt, câu đủ điều kiện)
           bằng chứng như 1640 → lưu submission → commit
GET /lessons/{id}
  payload content (có hint, options, answerSpec) + tập đã mở theo khối (1 query) → câu hỏi có hint hoặc null
```

## Related Code Files

- Java hiện thực dưới `services/learning-service/src/main/java/com/group01/learning/`:
  - Create: `domain/service/LessonHintPolicy.java`.
  - Modify: `application/port/{LearningContentClient,LearningProgressStore}.java`, `application/usecase/LearnLessonUseCase.java`,
    `application/result/{LessonResult,SubmissionResult}.java`, `api/dto/{LessonResponse,SubmissionResponse}.java`,
    `infrastructure/persistence/JdbcLearningProgressStore.java`.
- Tests dưới package tương ứng: `LessonHintPolicyTest`, `LessonLearningWebMvcTest`, `RestLearningContentClientTest`,
  `LessonSubmissionIntegrationTest`, `ReviewAndTestAssignmentIntegrationTest`. Tên Python trong bước thiết kế dưới đây đọc theo mapping đã chấp nhận.

## Implementation Steps

**Tests Before:**
1. Chạy toàn bộ test ai-learning sau 1640, ghi baseline. Khóa response hiện tại của nộp khối và `GET /lessons/{id}` trong kịch bản
   Lan (trừ key `hint` mới). Xác nhận V11 đã có INDEX(`user_id`, `lesson_id`, `block_id`).

**Tests After** (viết trước code, DB thật, content giả trả `hint`):
2. `test_lesson_hints.py`:
   - `hint_eligible`: FILL → True; CHOICE 3 phương án → True; TFNG (`options` rỗng) → True; CHOICE 2 phương án → False; spec `{}` → False;
   - lần nộp đầu, Q12 (FILL) sai và có gợi ý → `hint` của Q12 đúng chuỗi content; câu chưa từng sai `null`;
   - câu 3 phương án (Q11) sai và có gợi ý → có `hint`; câu 2 phương án (Q3) sai, content có gợi ý → `null`;
   - câu sai mà content không có gợi ý → `null`;
   - khối đạt → mọi `hint` `null`, có lời giải;
   - nộp lại cùng `requestId` → response y hệt;
   - Q12 sai lần 1, đúng lần 2, khối chưa đạt → response lần 2 và `GET /lessons/{id}` đều có `hint` của Q12;
   - `GET /lessons/{id}` trước lần nộp → mọi `hint` `null`; sau khi đạt → `null`;
   - học viên B sai Q12 → `GET` của học viên A vẫn `null`.
3. Bằng chứng không đổi: Q12 sai lần 1 (gợi ý mở), đúng lần 2 → chỉ lần 1 có bằng chứng; mastery bằng đúng giá trị khi content
   không có gợi ý.
4. `test_lesson_api.py`: câu hỏi có key `hint` ở mọi response; không có key cấm (`answerSpec`, `explanation` trước khi đạt) —
   kiểm "không chứa key cấm" theo quy ước lộ trình.
5. `test_review_sets.py`: `results[]` của bài ôn không có gợi ý (`hint` vắng mặt hoặc `null`).

**Implement:** `hints.py` → store → service → DTO → content client.

**Regression Gate:**
```powershell
mvn -q -pl services/learning-service -am test
```

## Success Criteria

- [x] Gợi ý chỉ ra cho câu đủ điều kiện, đã sai ít nhất một lần trong khối chưa từng đạt; server quyết định.
- [x] Mỗi lần nộp mới hoặc `GET` thêm tối đa một query; replay không đọc Content/hint; không migration Learning.
- [x] Không ghi `hints_used`; evidence lần nộp đầu và mastery không đổi; không sửa `MasteryCalculator`.
- [x] Kịch bản Lan của 1640 vẫn pass trong regression Learning (183 test).

## Risk Assessment

- **Đọc `response` JSONB:** đã kiểm bằng persistence Testcontainers trên response thật, cả câu sai ở lần nộp sau và cách ly user/bài/khối. Đổi key DTO tương lai phải cập nhật query/test cùng lúc.
- **Luật đủ điều kiện:** đã khớp contract, seed Content và `LessonHintPolicy` Java; policy test kiểm TFNG fallback/spec không hợp lệ, review integration kiểm hint null.
- **Chuỗi gợi ý bị chép vào `response`:** là nội dung học, không phải dữ liệu cá nhân hay bí mật; cần để replay trả y hệt.

## Security Considerations

- Log chỉ ghi id (user, bài, khối, câu), không ghi nội dung gợi ý hay câu trả lời (AGENTS §5).
- Tập câu đã mở luôn lọc theo `user_id` của token; không nhận danh sách câu từ client.
