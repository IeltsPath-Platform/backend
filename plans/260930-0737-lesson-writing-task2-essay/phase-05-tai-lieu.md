---
phase: 5
title: "Tài liệu"
status: pending
priority: P3
dependencies: [4]
effort: "0.5 ngày"
---

# Phase 5: Tài liệu

## Overview

Đưa quyết định và schema mới vào tài liệu nguồn. Chỉ ghi điều code đã làm; không ghi giá trị secret.

## Requirements

- `.sdd/database/DATABASE_V5.md`:
  - lịch sử phiên bản thêm dòng V5.5 (Writing Task 1 + Task 2 trong bài học);
  - §5.5 bảng `answer_spec` thêm dòng `ESSAY`, ghi "không tự chấm";
  - §5.16 luật khối essay (đúng 1 câu ESSAY, không tính vào hoàn thành bài);
  - §7 thêm §7.27 `lesson_writing_submissions` (cột, CHECK, index, trạng thái, luật trừ point);
  - §7.15/§7.23 ghi source evidence `lesson_writing`;
  - §13 ghi: Writing trong bài học chấm ở ai-learning, không qua `grading_jobs`; giá `AI_LEARNING_WRITING_POINT_COST`
    lặp giá trị với `grading_point_costs.WRITING`.
- `docs/system-architecture.md`:
  - flow nộp essay (sơ đồ ngắn): ai-learning → content, access, LLM;
  - bảng điểm chưa nhất quán thêm: giá point lặp ở hai nơi; hai nơi lưu bài Writing (bài học ở ai-learning, đề sau này
    ở assessment).
- `services/ai-learning-service/README.md` và `CLAUDE.md`: biến env mới, endpoint, không tính `llm_daily_usage`.
- `services/access-service/README.md` (tạo ở phase 1): kiểm lại khớp code sau khi merge.
- `docs/contracts/lesson-writing-v1.md`: kiểm lại mã lỗi (`ESSAY_TOO_SHORT`, `INVALID_LESSON_BLOCK` là lỗi nội bộ content).
- `CLAUDE.md` gốc §5 (cạm bẫy): ai-learning cần `AI_LEARNING_ACCESS_SERVICE_BASE_URL` để chấm essay.
- `plans/reports/brainstorm-260930-0737-writing-speaking-practice-db-readiness-report.md` đã ghi quyết định chấm trong request
  (sửa lúc lập plan); kiểm lại khớp code.

## Related Code Files

- Modify: `.sdd/database/DATABASE_V5.md`, `docs/system-architecture.md`, `services/ai-learning-service/README.md`,
  `services/ai-learning-service/CLAUDE.md`, `services/access-service/README.md`, `CLAUDE.md`

## Implementation Steps

1. Đọc từng tài liệu trước khi sửa; đối chiếu với code và migration thật (ai-learning V12, content V10, controller access).
2. Sửa theo danh sách trên; giữ giọng văn và cấu trúc của từng file.
3. Chạy `graphify update .` sau khi code đã merge.

## Success Criteria

- [ ] Mọi bảng, cột, endpoint, biến env trong tài liệu khớp code.
- [ ] Không có giá trị secret; không nhắc số phase hay mã plan trong tài liệu sản phẩm.

## Risk Assessment

- `docs/system-architecture.md`: chỉ thêm mục riêng, không sửa mục của plan khác cùng lúc (plan kiến trúc chia service đã xóa 2026-09-30).