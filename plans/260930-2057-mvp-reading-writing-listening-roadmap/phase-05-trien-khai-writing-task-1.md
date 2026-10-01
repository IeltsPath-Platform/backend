---
phase: 5
title: "Triển khai Writing Task 1"
status: pending
priority: P2
dependencies: [4]
effort: "~2–3 ngày"
---

# Phase 5: Triển khai Writing Task 1

## Overview

Triển khai plan [`260930-0812-lesson-writing-task1-academic`](../260930-0812-lesson-writing-task1-academic/plan.md): khối
essay nhận đề Task 1 Academic, ảnh biểu đồ gắn vào câu, grader đọc `chartFacts` (không đọc ảnh). Dùng lại luồng nộp, trừ
point, bằng chứng và cổng của Task 2.

## Requirements

- 0812 đã validate lại; hàm kiểm media dùng chung được viết ở đây (quy ước ở phase 1 của lộ trình).
- Migration: content V11 (seed); không migration ai-learning.

## Implementation Steps

1. `/ck:cook plans/260930-0812-lesson-writing-task1-academic/plan.md`.

## Success Criteria

- [ ] Tiêu chí nghiệm thu của 0812 pass; Task 2 vẫn trả `TR`, `CC`, `LR`, `GRA` (không hồi quy).
- [ ] Không response học viên nào có `chartFacts`.

## Risk Assessment

- **Hàm kiểm media là điểm chạm với Listening:** viết đủ mở rộng được (theo loại asset), không làm sẵn phần key + base URL.
