---
phase: 0
title: "Latency spike"
status: pending
priority: P1
dependencies: []
---

# Phase 0: Spike đo thời gian chấm (learning, không merge)

## Overview
Prompt v2 bắt liệt kê mọi lỗi kèm lý do, dẫn chứng từng tiêu chí và `dataChecks`. Output ước khoảng 3 đến 4 nghìn token,
trong khi Learning cắt ở 20 giây (`WritingSettings` chặn tối đa 25) và Gateway cắt route chấm bài ở 60 giây. Phase này
đo thật trước khi viết code, vì kết quả có thể buộc xem lại quyết định 2 (một request mỗi bài).

## Requirements
- Chạy bản nháp prompt v2 (đủ các phần của phase 4 và 5: corrections không trần kèm `reason` và `certainty`, `priorities`,
  `errorExamples`, `evidence` và `descriptor` từng tiêu chí, `dataChecks` cho Task 1, `taskChecks` cho Task 2 (phase 8),
  judgment KP con nếu đề có) qua
  `OpenAiCompatibleLlmClient` của Learning, với
  model và `reasoning_effort` đang dùng thật.
- Bài chạy: bài 24 lỗi của examiner, một bài Task 2 dài khoảng 300 từ nhiều lỗi (ca chậm nhất), một bài band cao ít lỗi.
  Mỗi bài 5 lần. Báo cáo tách p95 theo task.
- Ghi cho mỗi lần: thời gian, token vào và ra (`usage` của provider), JSON có hợp lệ không, số correction.
- Timeout tạm nâng lên 120 giây **chỉ trong spike** để đo được thời gian thật thay vì chỉ biết là quá giờ.
- Code spike nằm ở nhánh nháp hoặc test thủ công, không merge. Essay và output không commit; báo cáo chỉ có số.

## Implementation Steps
1. Viết bản nháp template v2 (chưa cần examiner duyệt câu chữ).
2. Chạy trên ba bài như trên với cấu hình model hiện tại.
3. Ghi `reports/latency-spike.md`: p50, p95, max thời gian; token ra; tỷ lệ JSON hỏng; so với v1 trên cùng bài.
4. Kết luận theo bảng dưới và ghi vào `plan.md` trước khi bắt đầu phase 1.

| Kết quả | Hướng |
| --- | --- |
| p95 dưới 20 giây | Giữ quyết định 2 và timeout hiện tại |
| p95 từ 20 đến 25 giây | Giữ quyết định 2, nâng `grading-timeout-seconds` trong giới hạn 25 |
| p95 trên 25 giây | Dừng, hỏi người dùng: tách request, hạ phạm vi output (ví dụ bỏ `descriptor` hoặc `errorExamples`), đổi model, hay nâng trần timeout (đổi `@Max` của `WritingSettings`, kiểm Gateway 60 giây) |

## Success Criteria
- [ ] Có số p95 và token ra của v2 trên ba bài, đo bằng client của Learning.
- [ ] Kết luận ghi vào `plan.md`; nếu rơi vào dòng cuối của bảng thì đã có câu trả lời của người dùng.

## Risk Assessment
- Gọi LLM thật tốn tiền: tối đa khoảng 30 lượt, ngoài hạn mức của học viên.
- Model có thể đổi sau spike: số đo gắn với model ghi trong báo cáo; đổi model thì đo lại.
