---
phase: 4
title: "Triển khai Writing Task 2"
status: completed
priority: P1
dependencies: [3]
effort: "~4–5 ngày"
---

# Phase 4: Triển khai Writing Task 2

## Overview

Triển khai plan `260930-0737-lesson-writing-task2-essay`: khối essay
trong bài học, ai-learning chấm bằng LLM, chấm xong trừ 3 point qua access, ghi bằng chứng. Khối essay không chặn học tiếp.

## Requirements

- 0737 đã validate lại ở phase 2 của lộ trình.
- Migration: content V10 (seed), ai-learning V12 (`lesson_writing_submissions`).
- Access: debit (và entitlement nếu cần) sang `/internal/access/**`, debit kiểm `userId` = subject; refund **không** mở cho
  service (chỉ `ADMIN` hoặc xóa route); Gateway không route `/internal/**`.

## Implementation Steps

1. `/ck:cook plans/260930-0737-lesson-writing-task2-essay/plan.md`.
2. Test không gọi LLM thật (`OpenAiStub`/`ScriptedChat`).

## Success Criteria

- [ ] Tiêu chí nghiệm thu của 0737 pass (402 khi không đủ point, 503 khi LLM lỗi và ví không đổi, `PAYMENT_PENDING` rồi gửi lại
      cùng `requestId` chỉ trừ một lần).
- [ ] Regression: `mvn -q -pl services/access-service -am test`, content, ai-learning pytest.

## Risk Assessment

- **Request đồng bộ 20–45 giây** (đã chấp nhận ở 0737): Gateway không đặt response timeout; kiểm lại khi có frontend.
- **Lỗ refund của access:** đã chốt không mở refund cho service gọi (Validation Session 1).
