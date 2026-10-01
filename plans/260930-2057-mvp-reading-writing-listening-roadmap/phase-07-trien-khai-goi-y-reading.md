---
phase: 7
title: "Triển khai gợi ý Reading"
status: pending
priority: P2
dependencies: [6]
effort: "~2 ngày"
---

# Phase 7: Triển khai gợi ý Reading

## Overview

Triển khai plan [`260930-1006-reading-question-hints`](../260930-1006-reading-question-hints/plan.md): cột
`question_versions.hint`; câu đủ điều kiện bị làm sai trong khối chưa đạt thì hiện gợi ý tới khi khối đạt. Chỉ trong bài
học, không trong bài ôn hay đề cuối.

## Requirements

- 1006 đã validate lại: **bỏ `hints_used`** (chỉ trả gợi ý, không ghi evidence); nộp lại cả khối theo 1640.
- Migration: content V13 (cột `hint` + seed gợi ý cho câu Reading); không migration ai-learning.

## Implementation Steps

1. `/ck:cook plans/260930-1006-reading-question-hints/plan.md`.

## Success Criteria

- [ ] Tiêu chí nghiệm thu của 1006 pass (sau khi sửa ở phase 2).
- [ ] Bài ôn, đề cuối, `GET /package-versions/{id}`, game snapshot không trả `hint`.

## Risk Assessment

- **Gợi ý lộ đáp án:** đợt này chỉ test seed kiểm; chặn ở API admin để đợt sau (đã chốt trong 1006).
