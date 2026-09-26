---
phase: 6
title: "Study và review: luyện KP của path"
status: pending
priority: P1
dependencies: [5]
effort: "~2d"
---

# Phase 6: Study và review, luyện KP của path

## Overview
Hoàn thiện trải nghiệm chính: học viên mở Tutor, và DeepTutor dạy đúng KP mà `next_objective()` chọn. Hỏi bằng
`mastery_quiz`, chấm bằng `mastery_grade` cho MEMORY/PROCEDURE, đánh giá định tính bằng `mastery_assess` cho
CONCEPT/DESIGN (Writing, Speaking). Evidence từ Tutor và evidence từ bài thi chính thức cùng cập nhật một path. Review
dùng lịch ôn sẵn có của DeepTutor.

## Đọc trước khi code
- `capabilities/mastery/tools.py` (`MasteryQuizTool`, `MasteryGradeTool`, `MasteryAssessTool`, `MasterySkipQuestionTool`, `MasteryStatusTool`).
- `capabilities/mastery/prompts/en/mastery_loop.yaml` (khối `study`, `review`).
- `learning/policy.py` (`next_objective`, cổng), `learning/assessment.py` (ghi question notebook sau khi chấm).
- `app/learning/formal_provenance.py`, `override_provenance.py` (evidence chính thức và override placement).

## Requirements
- Functional:
  - Mode mặc định của session là `study`. Khi `next_objective()` trả `review` thì DeepTutor tự chuyển; không viết logic
    riêng.
  - Evidence từ Tutor có `source = mastery_path` (của DeepTutor) và không bị nhầm với evidence chính thức
    (`formal_provenance`). `/map` hiển thị được nguồn.
  - KP đã được miễn bằng placement không được Tutor hỏi lại. `next_objective` đã bỏ qua KP mastered; cần test khóa điều này.
  - Sau mỗi lần chấm, DeepTutor ghi `notebook_entries` (pha 3).
  - Ngôn ngữ trả lời: tiếng Anh cho nội dung IELTS; giải thích theo `preferences` của `learner_profile` nếu có (pha 7).
- Non-functional: kết quả thi chính thức tới **trong lúc** đang có turn Tutor: consumer chờ lease, hoặc retry theo cơ chế
  hiện có. Không mất evidence, không double-count.

## Implementation Steps
### Tests Before
1. E2E trong suite (LLM giả kịch bản hóa):
   - KP PROCEDURE: quiz → trả lời đúng nhiều lần → mastered theo cổng 0.9 → `/status` sang KP tiếp theo;
   - KP CONCEPT: `mastery_assess` PASS → mastered;
   - trả lời sai → error record, lịch ôn;
   - KP đã test-out không bị hỏi.
2. Đồng thời: consumer áp kết quả chính thức cho cùng path trong lúc turn Tutor đang giữ lease → cả hai evidence còn, revision đúng.
### Refactor
3. Chỉ cấu hình và nối; không viết lại tool.
### Tests After
4. Gate đầy đủ.

## Success Criteria
- [ ] Học viên học được KP của path qua Tutor, và mastery cập nhật như evidence chính thức.
- [ ] Không có logic dạy học nào tự viết ngoài DeepTutor.
