---
phase: 3
title: "Triển khai nền 1640"
status: pending
priority: P1
dependencies: [2]
effort: "~2 tuần (theo 8 phase của 1640)"
---

# Phase 3: Triển khai nền 1640

## Overview

Triển khai plan [`260929-1640-lesson-learning-pipeline-mvp`](../260929-1640-lesson-learning-pipeline-mvp/plan.md): path theo
user, bài học, bài ôn, đề cuối, Reading. Mọi plan sau dựa trên phase này.

## Requirements

- Làm đúng thứ tự trong plan 1640: phase 1 (dừng duyệt contract) → 2, 3, 5 song song → 6 → 4 → 7 → 8. Consumer chịu event
  không có goal (phase 5) phải lên trước assessment phát event mới (phase 4).
- Migration: content V8, V9 (V7 là của chia service S1); ai-learning V10, V11. Duyệt V10 trước khi chạy trên DB dev dùng chung.

## Implementation Steps

1. Giao cho agent code theo [`codex-handoff.md`](../260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md): mỗi PR một
   phiên mới, đúng thứ tự 10 PR (hoặc `/ck:cook plans/260929-1640-lesson-learning-pipeline-mvp/plan.md` nếu làm bằng Claude Code).
2. Hai điểm dừng: duyệt contract (cuối phase 1 của 1640) và duyệt V10 (phase 5 của 1640).
3. Sau khi merge: chạy regression gate của phase 8 trong 1640.

## Success Criteria

- [ ] Tiêu chí nghiệm thu của 1640 pass, gồm kịch bản Lan và các test đồng thời, lỗi giữa chừng.
- [ ] Test tutor hiện có vẫn pass (tutor giữ nguyên, app không gọi).

## Risk Assessment

- **Phase lớn nhất:** 1640 đã chia PR (5a/5b, 6a/6b). Không bắt đầu phase 4 của lộ trình trước khi 1640 merge.
