---
phase: 5
title: "Task 1 data checks"
status: code-done-pending-eval
priority: P1
dependencies: [4]
---

# Phase 5: Kiểm số liệu Task 1 (learning, template dùng chung)

## Overview
Câu đúng ngữ pháp nhưng sai số liệu đang bị sót, và gợi ý sửa đôi khi vẫn sai nghĩa dữ liệu. Phase này bắt model duyệt
từng câu có số liệu so với `chart_facts`, và hiện số liệu đúng cho học viên sau khi nộp (quyết định 1).

## Requirements
- **Prompt** (đoạn chỉ nối khi `TASK_1`, là một phần của template v2 nên thuộc cùng `+tplv2` nếu phase này kịp trước lần
  kích hoạt đầu; nếu không thì là `v3`, đi lại quy trình eval):
  - Mảng `dataChecks`: một mục cho **mỗi câu** của bài có số liệu, so sánh hoặc xu hướng:
    `{"excerpt","verdict":"CORRECT|WRONG|UNVERIFIABLE","expected"}`; `expected` là một câu tiếng Việt ngắn nêu điều
    đúng theo `chart_facts`, chỉ điền khi `WRONG`.
  - Cờ cấp bài: `hasOverview`, `overviewAccurate`, `hasPersonalOpinion`, mỗi cờ kèm `excerpt` khi áp dụng.
  - Luật cho corrections: `suggestion` phải đúng cả ngữ pháp lẫn `chart_facts`; một đoạn vừa sai ngữ pháp vừa sai số liệu
    được có hai mục.
- **Parser (Learning):**
  - `excerpt` không có nguyên văn trong bài → bỏ mục đó. `dataChecks` thiếu hoặc sai định dạng → `[]`, **không** `INVALID_GRADE`.
  - `WRONG` hợp lệ mà chưa có correction nào chồng lên với mã `T1_DATA_MISREPORTED` → tự thêm correction mã đó,
    `suggestion` = `expected`.
  - `hasOverview = false` → correction/ghi nhận `T1_NO_OVERVIEW`; `hasPersonalOpinion = true` → mã mới từ phase 3 (chưa
    duyệt thì `TASK_OTHER`).
  - Giới hạn: tối đa 20 mục, `expected` ≤ 200 ký tự.
- **Hiển thị cho học viên (quyết định 1):**
  - Response trả `dataChecks` chỉ gồm mục `WRONG` (`excerpt`, `expected`) và ba cờ. Mục `CORRECT` chỉ trả số lượng.
  - Chỉ có ở bài `GRADED`. `chartFacts` nguyên văn vẫn không bao giờ rời service.
  - `expected` do LLM viết từ `chart_facts`: prompt cấm chép lại toàn bộ dữ kiện; parser cắt 200 ký tự mỗi mục.
- **Sửa quy tắc:** `AGENTS.md` §3.8 và `lesson-writing-v1.md` thêm ngoại lệ: "sau khi bài Task 1 được chấm, học viên nhận
  phát biểu đúng cho từng câu số liệu họ viết sai; `chartFacts` nguyên văn vẫn không được trả". Cập nhật dòng ngày ở đầu `AGENTS.md`.
- Assessment: template chung nên prompt có đoạn này; parser của Assessment bỏ qua `dataChecks` (quyết định 5).

## Related Code Files
- Learning: `application/service/EssayGrader.java` (hoặc tách `Task1DataChecker` thuần trong `application/service/` theo mẫu
  `EssayKnowledgeJudge`), `domain/vo/WritingGrade.java`, `api/dto/response/WritingSubmissionResponse.java`, template v2
- Docs: `AGENTS.md`, `docs/contracts/lesson-writing-v1.md`, template trong `docs/contracts/`
- Tests: parse `dataChecks` (hợp lệ, trích dẫn sai, thiếu mảng, quá 20 mục); tự thêm `T1_DATA_MISREPORTED` không trùng;
  Task 2 không có `dataChecks`; response không bao giờ chứa `chartFacts`

## Implementation Steps
1. Test đỏ parser và luật ghép correction.
2. Sửa template; examiner duyệt câu chữ.
3. Code, cập nhật contract và `AGENTS.md` trong cùng PR.
4. Eval: `data_error_recall` so với số nền; examiner kiểm tay 10 bài Task 1 rằng không `suggestion` nào mâu thuẫn bảng.

## Success Criteria
- [ ] Bài của examiner: cả hai lỗi ngược số liệu được nêu, kèm số đúng.
- [ ] `how many people going to library` được sửa theo nghĩa của bảng (số lượt trung bình mỗi người), không chỉ sửa ngữ pháp.
- [ ] Không response nào chứa `chartFacts`.

## Risk Assessment
- `chart_facts` của một số đề có thể viết sơ sài, khiến model đánh `WRONG` oan: có `UNVERIFIABLE`, và rà chất lượng
  `chartFacts` của các đề Task 1 hiện có (việc nội dung, liệt kê đề thiếu trong báo cáo).
- Thêm một mảng nữa làm output dài thêm: tính vào phép đo p95 của phase 4.
- Evidence: `T1_DATA_MISREPORTED` tự thêm có thể tạo `FAIL` cho KP gắn với mã đó. Đây là hành vi mong muốn, nhưng ghi rõ trong contract.
