---
phase: 6
title: "Triển khai Listening"
status: pending
priority: P1
dependencies: [5]
effort: "~4 ngày"
---

# Phase 6: Triển khai Listening

## Overview

Triển khai plan [`260930-0851-listening-topic-audio-lessons`](../260930-0851-listening-topic-audio-lessons/plan.md): topic
`DEMO_LISTENING` đi trọn luồng học, audio mp3 trên cloud (DB lưu key), transcript ẩn tới khi đạt, khóa
`GET /api/content/assets/{id}` với CUSTOMER. Gói luyện không gắn độ khó (0908 hoãn).

## Requirements

- 0851 đã validate lại ở phase 2 (bỏ phụ thuộc 0908, `kind` của KP, DTO không có `transcript`).
- Migration: content V12 (seed); không migration ở assessment và ai-learning.
- Hạ tầng: bucket public-read, `CONTENT_MEDIA_BASE_URL` trong `.env` và config-repo content; mp3 seed đã upload.

## Implementation Steps

1. Upload mp3 seed lên bucket (không commit mp3 vào git).
2. `/ck:cook plans/260930-0851-listening-topic-audio-lessons/plan.md`.

## Success Criteria

- [ ] Tiêu chí nghiệm thu của 0851 pass; CUSTOMER `GET /api/content/assets/{id}` → 403.
- [ ] Đổi `CONTENT_MEDIA_BASE_URL` → URL trả ra đổi theo, không migration.

## Risk Assessment

- **Chưa có file mp3:** code và test chạy với URL giả; E2E ở phase 8 cần file thật.
