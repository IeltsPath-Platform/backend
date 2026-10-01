---
phase: 3
title: "Tài liệu"
status: pending
priority: P3
dependencies: [2]
effort: "0,5 ngày"
---

# Phase 3: Tài liệu

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java (plan [`261001-1228`](../261001-1228-learning-service-java/plan.md)). Mọi tên file, lệnh và API Python dưới đây đọc theo [bảng ánh xạ](../261001-1228-learning-service-java/python-to-java-mapping.md); luật nghiệp vụ, mã lỗi và test case giữ nguyên.

## Overview

Ghi cột `hint` và luật hiện gợi ý vào tài liệu DB, `.sdd/specs/SERVICE_ARCHITECTURE_V3.md` (kiến trúc đích của MVP) và
`docs/system-architecture.md`. Contract đã sửa ở phase 1.

## Requirements

- `.sdd/database/DATABASE_V5.md`:
  - §0: thêm dòng **V5.4** (2026-09-30, thiết kế đích): "Content thêm `question_versions.hint` (gợi ý hiện sau lần sai đầu trong
    bài tập bài học). AI Learning không thêm bảng, không ghi `hints_used`. Plan:
    `plans/260930-1006-reading-question-hints`". Nếu lúc merge đã có migration thật thì bỏ chữ "thiết kế đích".
  - §5.5: thêm dòng `hint?` | text | — | "Gợi ý không chứa đáp án, tối đa 500 ký tự. Chỉ dùng cho câu điền từ và câu chọn từ 3
    phương án (kể cả True/False/Not Given); câu 2 phương án không có. Hiện cho học viên sau khi làm sai câu đó trong khối bài tập chưa đạt, giữ tới khi khối đạt; không dùng cho gói luyện
    và đề cuối."
  - §7.23 `lesson_exercise_submissions`: ghi "gợi ý đã mở tính từ `response` của các lần nộp; không lưu cột riêng". Index
    `(user_id, lesson_id, block_id)` đã ghi lúc validate (2026-09-30), chỉ cần kiểm còn khớp V11 thật.
- `docs/system-architecture.md` (mục luồng bài học của §6, do 1640 phase 8 viết): thêm một câu: "Gợi ý câu Reading
  (`question_versions.hint`): content lưu và trả qua `/internal/learning-content/lessons/{id}`; ai-learning quyết định khi nào
  học viên thấy (câu điền từ hoặc chọn từ 3 phương án, sau lần sai đầu, tới khi khối đạt)." Không sửa mục khác của tài liệu này.

## Related Code Files

- Modify: `.sdd/database/DATABASE_V5.md`, `.sdd/specs/SERVICE_ARCHITECTURE_V3.md` (khối ranh giới content, thêm
  `question_versions.hint`: content lưu, ai-learning quyết định hiện), `docs/system-architecture.md`

## Implementation Steps

1. Đọc lại hai file (đã có V5.3 và khối Listening), sửa đúng các mục trên.
2. Soát: tên cột, số mục, đường dẫn plan khớp code đã merge; không ghi giá trị secret.

## Success Criteria

- [ ] DATABASE_V5 có `hint` ở §5.5, dòng V5.4 ở §0.
- [ ] `SERVICE_ARCHITECTURE_V3.md` và `docs/system-architecture.md` ghi rõ content lưu, ai-learning quyết định hiện.

## Risk Assessment

- **Số phiên bản V5.4 trùng với thay đổi khác merge trước:** lấy số kế tiếp lúc sửa tài liệu.
