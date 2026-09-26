---
phase: 5
title: "Chỉnh lộ trình bằng hội thoại và hồ sơ học viên"
status: pending
priority: P2
dependencies: [4]
effort: "~1.5d"
---

# Phase 5: Chỉnh lộ trình bằng hội thoại và hồ sơ học viên

## Overview
Học viên trò chuyện với tutor để hiểu lộ trình, đổi **thứ tự** module/KP, và cập nhật hồ sơ học (trình độ hiện tại, mục
tiêu, thời gian, cách học ưa thích). Ý tưởng từ chế độ `outline` của DeepTutor (`mastery_revise`, `mastery_profile`), tự viết.

## Đọc trước
- `app/learning/path_ordering.py` (`OrderingValidator.apply`: chỉ chấp nhận hoán vị đúng tập module/KP của Content).
- `app/mastery/service.py` (`record_learner_profile`, event `path.learner_profile_recorded`).
- Tham khảo, chỉ đọc: `third_party/deeptutor/deeptutor/capabilities/mastery/tools.py` (`MasteryReviseTool`, `MasteryProfileTool`),
  khối `outline` trong `capabilities/mastery/prompts/en/mastery_loop.yaml`.

## Requirements
- Tool mới cho tutor (bật trong một chế độ `outline` của session, hoặc luôn có; chọn một, ghi trong README):
  - `path_reorder(modules: [{id, knowledgePoints: [...]}])`: validate bằng `OrderingValidator` (không thêm, bỏ, chuyển KP
    giữa module). Hợp lệ → ghi thứ tự mới trong một giao dịch, event `path.reordered_by_learner` (khác `path.ordered` của
    LLM lúc tạo path). Không hợp lệ → lỗi dạng text cho LLM, path không đổi.
  - `learner_profile(fields)`: gọi `record_learner_profile` (chỉ các trường `prior_knowledge`, `target_level`, `time_budget`,
    `preferences`, `notes`).
- **Bất biến**: tập KP của path chỉ đổi qua tạo path và refresh từ Content. Kiểm ở store hoặc service, không dựa vào prompt.
- Hồ sơ học viên được đưa vào context của tutor ở mọi lượt (pha 2).

## Tests
1. Hoán vị hợp lệ → path đổi thứ tự, `next_objective()` theo thứ tự mới, event đúng.
2. Thêm KP, bỏ KP, chuyển KP sang module khác, module lạ → lỗi, path giữ nguyên.
3. `learner_profile` → `learner_profile` đổi đúng trường; trường không cho phép bị bỏ qua.
4. Refresh từ Content sau khi học viên đổi thứ tự vẫn giữ thứ tự của học viên (hành vi refresh hiện có).

## Success Criteria
- [ ] Học viên đổi được thứ tự lộ trình bằng hội thoại; tập KP của Content không bao giờ đổi.
