---
title: "Path cá nhân hóa theo goal và test-out từ placement"
description: "Content gắn band cho topic/KP; AI Learning chỉ đưa vào path các KP trong phạm vi band mục tiêu của goal; kết quả placement đánh dấu test-out qua override của DeepTutor; POST /paths làm mới phạm vi. Không thêm planner thứ hai."
status: pending
priority: P1
branch: "feat/ai-learning-service"
tags: [feature, backend, database, api, adaptive-learning]
blockedBy: []
blocks: []
created: "2026-09-25T04:25:00.000Z"
createdBy: "claude"
source: conversation
---

# Path cá nhân hóa theo goal và test-out từ placement

## Overview

Hiện mọi learner nhận cùng một path: `CurriculumAdapter` lấy toàn bộ topic và KP ACTIVE từ Content, không
dùng gì từ goal ngoài `goalId`. Phần adaptive duy nhất là `next_objective()` của DeepTutor chạy trên path
chung đó. Hệ quả:

- Learner mục tiêu 5.5 và learner mục tiêu 8.0 học cùng tập KP, cùng thứ tự.
- Placement gần như vô tác dụng với KP định lượng. `compute_mastery` giới hạn điểm ở 0.5 với 1 lần làm và 0.8
  với 2 lần, còn cổng là 0.9, nên learner giỏi vẫn phải `practice` lại từ KP đầu tiên.
- Path đóng băng từ lúc tạo, không nhận KP mới của Content.

Plan này tách hai loại quyết định và đặt mỗi loại đúng chỗ, giữ bất biến của spec V2 ("DeepTutor là adaptive
engine duy nhất, không có planner thứ hai"):

| Quyết định | Loại | Nằm ở |
| --- | --- | --- |
| Path gồm những KP nào | Phạm vi nội dung, giống chọn giáo trình | `CurriculumAdapter`, lọc trước khi gọi `replace_modules_for_path` |
| Learner bắt đầu từ đâu | Trạng thái học | `learner_mastery_overrides` của DeepTutor, thêm provenance `placement` |
| Học gì tiếp, ôn gì | Adaptive | `next_objective()` của DeepTutor, **không đổi** |

- Nguồn phân tích: hội thoại ngày 2026-09-25 (so sánh cách hiện tại với cách lọc bằng metadata).
- Chế độ: `--tdd`. Mỗi pha có Tests Before, Refactor, Tests After và Regression Gate như plan
  `260924-2135-main-flow-blockers`.

## Quyết định (mặc định đề xuất, cần xác nhận trước khi cook)

| # | Câu hỏi | Mặc định đề xuất | Lý do |
| --- | --- | --- | --- |
| D1 | Band gắn ở đâu? | `band_min`/`band_max` trên **topic**. KP có thể ghi đè; KP không ghi đè thì kế thừa topic. Content trả sẵn band hiệu lực của KP. | Topic = module của DeepTutor, lọc theo module rất tự nhiên. KP ghi đè cho trường hợp đặc biệt. Luật kế thừa chỉ có ở Content. |
| D2 | Lọc phạm vi theo gì? | Chỉ theo `targetBand`: KP vào path khi `effectiveBandMin ≤ targetBand` hoặc không có band. **Chưa** lọc theo skill. | Goal chưa có trường skill trọng tâm; thêm skill cần sửa User Service và UI. |
| D3 | Test-out từ placement dựa vào gì? | Chỉ bài `PLACEMENT`. (a) KP có `effectiveBandMax ≤ overallBand` của placement được test-out. (b) KP có mọi item placement map tới đều đúng (hoặc judgment PASS) được test-out. | (a) cho learner giỏi bỏ qua nhanh nguyên cụm dễ; (b) dùng bằng chứng từng KP. Cả hai không làm giả evidence. |
| D4 | Đổi goal thì sao? | Không mang mastery sang. Goal mới có path mới; placement làm dưới goal mới sẽ test-out lại. | Tránh bài toán hợp nhất state giữa hai path; làm sau nếu cần. |
| D5 | Test-out hiển thị ra sao? | `masterySource = "placement"`, khác `system` (có bằng chứng) và `learner` (tự khai). | UI và tutor phải phân biệt được "đã chứng minh" với "được miễn". |
| D6 | Khi nào làm mới phạm vi? | Chỉ `POST /api/ai-learning/paths`. GET không gọi Content lại. | Không gọi Content ở mỗi request `/status`. |

Nếu đổi D1–D3 thì pha 1, 3 và 4 thay đổi theo; các pha còn lại không đổi.

Ngoài phạm vi:
- Lọc theo skill, band theo từng skill, và điều chỉnh nhịp theo `examDate`/`availableMinutesPerDay`.
- Mang mastery giữa các goal.
- Tự chấm câu khách quan (plan riêng).
- Tutor runtime và `LearnerProfile` cho LLM.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Content band metadata](./phase-01-content-band-metadata.md) | Pending |
| 2 | [Goal-scoped curriculum](./phase-02-goal-scoped-curriculum.md) | Pending |
| 3 | [Placement band in AssessmentCompleted.v2](./phase-03-placement-band-in-event.md) | Pending |
| 4 | [Placement test-out](./phase-04-placement-test-out.md) | Pending |
| 5 | [Path refresh and E2E verification](./phase-05-path-refresh-and-e2e.md) | Pending |

## Dependencies

- Pha 2 cần pha 1 (band hiệu lực trong DTO của Content).
- Pha 3 độc lập, làm song song với pha 1–2 được.
- Pha 4 cần pha 1 (band cho luật (a)), pha 3 (`overall_band`) và pha 2 (path đã lọc).
- Pha 5 cần cả 1–4.
- Sau mỗi pha có sửa code: `graphify update .` (theo `CLAUDE.md`).
- Gate chung sau mỗi pha:
  ```powershell
  mvn -o -pl services/content-service,services/assessment-service,services/user-service -am test
  ```
  ```bash
  # từ services/ai-learning-service
  PYTHONPATH="../../third_party/deeptutor;." AI_LEARNING_TEST_DATABASE_URL=<disposable db> AI_LEARNING_TEST_AMQP_URL=<amqp> python -m pytest tests
  ```

## Rủi ro chung

- **Chất lượng dữ liệu:** band gắn sai thì path sai. Band để trống nghĩa là "mọi band" (luôn vào path, không
  bao giờ test-out theo luật (a)). Nhờ vậy nội dung chưa gắn band vẫn chạy như hôm nay.
- **Thu hẹp phạm vi làm mất trạng thái:** `replace_modules()` của DeepTutor xóa mastery, evidence và override
  của KP bị loại khỏi path. Pha 5 phải chặn việc này hoặc ghi rõ và có test.
- **Kết quả chấm cho KP ngoài phạm vi** bị bỏ qua và log `unknown_knowledge_points` như hiện nay. Chấp nhận
  được, vì learner không học KP đó trong path này.
