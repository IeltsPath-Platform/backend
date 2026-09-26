---
phase: 7
title: "Outline có ràng buộc và hồ sơ học viên"
status: pending
priority: P2
dependencies: [6]
effort: "~1d"
---

# Phase 7: Outline có ràng buộc và hồ sơ học viên

## Overview
Bật chế độ `outline` của DeepTutor. Học viên trò chuyện để Tutor giải thích lộ trình, chỉnh **thứ tự** (`mastery_revise`)
và cập nhật hồ sơ học (`mastery_profile`). Đây là "cách B" trong phần làm sau của plan `260925-1547`.
`mastery_build` bị chặn, vì path đã được dựng từ Content.

## Đọc trước khi code
- `capabilities/mastery/tools.py` (`MasteryBuildTool`, `MasteryReviseTool`, `MasteryProfileTool`, `MasteryModeTool`),
  `capabilities/mastery/mode.py` (`TOOL_MODES`), khối `outline` trong `mastery_loop.yaml`.
- Pha 2: invariant T5 trong adapter.

## Requirements
- Functional:
  - `mastery_revise` đổi thứ tự module hoặc KP → adapter chấp nhận. Thêm, bỏ, đổi loại hay chuyển KP → adapter từ chối,
    tool trả lỗi cho LLM, path không đổi.
  - `mastery_build` → luôn bị từ chối, vì path đã có. Nếu DeepTutor cho phép tắt riêng tool này trong registry thì tắt luôn.
  - `mastery_profile` ghi qua `record_learner_profile`. Mọi thay đổi có event `path.learner_profile_recorded` như hiện nay.
  - Thứ tự do học viên chỉnh có event riêng (event `path.*` của DeepTutor), để phân biệt với `path.ordered` do Gemini sắp.
- Non-functional: không cho LLM đổi tên hay objective của KP Content. Nếu `mastery_revise` đổi `objective` của module, cho
  phép vì đó là mô tả hiển thị, không phải dữ liệu Content. Ghi rõ quyết định này trong README.

## Implementation Steps
### Tests Before
1. LLM giả gọi `mastery_revise` với:
   - (a) hoán vị hợp lệ → path đổi;
   - (b) thêm KP → lỗi, path giữ nguyên;
   - (c) chuyển KP sang module khác → lỗi.
2. LLM giả gọi `mastery_build` → lỗi.
3. `mastery_profile` → `learner_profile` đổi đúng trường.
### Refactor
4. Cấu hình tool và thông báo lỗi.
### Tests After
5. Gate đầy đủ.

## Success Criteria
- [ ] Học viên chỉnh được thứ tự lộ trình bằng hội thoại; tập KP của Content không bao giờ đổi.
