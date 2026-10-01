---
phase: 3
title: "Bài ôn và giao mã đề"
status: completed
priority: P1
dependencies: [2]
effort: "1.5 ngày"
---

# Phase 3: Bài ôn và giao mã đề

## Overview

Làm phần "6b" của `260929-1640/phase-06` bằng Java: `GET /reviews/{id}`, `POST /reviews/{id}/submissions`,
`POST /topics/{id}/test-assignments`. Luật giữ nguyên phase 6 (một set mở mỗi review, hết gói lấy gói lâu nhất, không có gói
→ `SKIPPED`, trượt set thứ 3 → `SKIPPED` với hằng số `MAX_FAILED_REVIEW_SETS = 3`, mã đề idempotent, mã chưa dùng trước, hết
thì mã dùng lâu nhất, version đang publish, không có mã → 409 `TEST_UNAVAILABLE`).

## Kết quả (2026-10-01, nhánh `feat/learning-reviews-tests`)

- `ReviewUseCase` (GET/POST review), `AssignTopicTestUseCase`, `ReviewStore` + `JdbcReviewStore`, `ReviewController`. Gói chưa giao (tính trên mọi review của user) đi trước; hết thì lấy gói giao lâu nhất; trượt set thứ 3 → `SKIPPED`; không còn gói nào → `SKIPPED`. Mã đề: `REVIEW_REQUIRED` → `TEST_LOCKED` → trả lần giao còn mở → gói chưa dùng (theo `code`) → gói dùng lâu nhất.
- Sửa thêm trong `LearnLessonUseCase` (content V10–V12 đã merge): khối `blockKind = ESSAY` không tính hoàn thành bài, nộp bài tập vào đó → 409 `ESSAY_BLOCK`, `GET` trả đề essay theo `lesson-writing-v1` (`latestSubmission` chưa có, chờ 0737 phase 4); asset audio trả `mediaUrl`, transcript chỉ khi bài xong; set ôn Listening có `audio`, nộp đạt thì trả `transcript`. `AnswerSheet` dùng chung luật kiểm câu trả lời và mốc 70%.
- Test: 126 pass, 0 skip (6 ca Testcontainers mới trong `ReviewAndTestAssignmentIntegrationTest`). Chưa có `@WebMvcTest` riêng cho 3 route mới.

## Requirements

- Mọi endpoint ghi chạy trong một `@Transactional` có `pg_advisory_xact_lock` theo user (như phase 2).
- Review: `WHERE id AND user_id`, không thấy → 404. Search gói qua content `practice-sets/search` với `excludePackageIds`.
  Nộp: `SELECT … FOR UPDATE` set đang mở khớp `reviewSetId`, không khớp → 409 `REVIEW_SET_CLOSED`; chấm; bằng chứng
  `source = review_set`; đóng set; ≥ 70% → `DONE`. Không chạy `ReviewRule`.
- Giao mã đề: cổng topic `IN_PROGRESS`, mọi bài xong, không review PENDING; lấy gói qua content `topics/{id}/test-packages`.

## Tests (nhẹ)

- Testcontainers: hai GET review → một set; trượt 3 set → `SKIPPED` và cổng hết `REVIEW_REQUIRED`; review user khác → 404.
- Testcontainers: POST mã đề hai lần → cùng lần giao; không có mã → 409.

## Success Criteria

- [ ] 3 endpoint đúng `lesson-learning-v1.md`; test trên pass.
