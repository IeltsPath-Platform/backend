---
phase: 4
title: "Compose, docs and E2E"
status: pending
priority: P1
dependencies: [1, 2, 3]
effort: "~3h"
---

# Phase 4: Compose, docs and E2E

## Overview
Đưa cấu hình Gemini vào compose và tài liệu, rồi kiểm chứng E2E qua Gateway với một Gemini giả chạy trong compose,
để không phụ thuộc key thật. Key thật do nhóm tự chạy thử trên máy.

## Requirements
- Functional:
  - Compose:
    - `ai-learning-api` nhận `AI_LEARNING_GEMINI_API_KEY: "${AI_LEARNING_GEMINI_API_KEY:-}"`. Trống thì tính năng tắt,
      nên compose vẫn chạy được khi chưa có key.
    - Cùng với `AI_LEARNING_GEMINI_MODEL` và `AI_LEARNING_GEMINI_TIMEOUT_SECONDS`.
  - `ai-learning-consumer` **không** nhận key.
  - README AI Learning: mục "LLM path ordering": dữ liệu nào được gửi và không gửi, hành vi khi lỗi, cách tắt.
  - README root: thêm `AI_LEARNING_GEMINI_API_KEY` vào bảng biến môi trường (chỉ tên, không ghi giá trị).
  - Postman: thêm kiểm tra `path.ordered` qua thứ tự module trả về, và một kịch bản không có key.
- Non-functional:
  - Không commit key. Report không chứa key hay nội dung prompt đầy đủ.

## Implementation Steps
1. Chạy E2E với Gemini giả (một HTTP stub trả thứ tự đảo ngược), 2 learner:
   - learner có placement: thứ tự path khác thứ tự Content, đúng theo stub; test-out vẫn đúng; `/status` theo thứ tự mới.
   - stub trả KP lạ → path theo thứ tự Content; event `source=content`, lý do `invalid_ordering`.
   - tắt stub (timeout) → path vẫn được tạo, theo thứ tự Content.
2. Chạy lại toàn bộ gate Java và Python: 0 fail, 0 skip.
3. Ghi report vào `reports/`.
4. Hướng dẫn nhóm chạy thử với key Gemini thật: đặt biến môi trường, tạo path, xem event `path.ordered`.

## Success Criteria
- [ ] Compose chạy được cả khi có và khi không có key.
- [ ] E2E với Gemini giả đạt 3 kịch bản trên.
- [ ] Tài liệu ghi rõ dữ liệu gửi ra ngoài và cách tắt tính năng.
