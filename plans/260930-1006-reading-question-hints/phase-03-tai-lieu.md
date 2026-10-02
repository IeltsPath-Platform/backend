---
phase: 3
title: "Tài liệu"
status: completed
priority: P3
dependencies: [2]
effort: "0,5 ngày"
---

# Phase 3: Tài liệu

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java (plan `261001-1228`). Mọi tên file, lệnh và API Python dưới đây đọc theo [bảng ánh xạ](../260930-2057-mvp-reading-writing-listening-roadmap/python-to-java-mapping.md); luật nghiệp vụ, mã lỗi và test case giữ nguyên.

## Kết quả (2026-10-02)

- 7 tài liệu scoped đã đồng bộ: `.sdd/database/{DATABASE_V5,mvp-database}.md`, `.sdd/specs/{SERVICE_ARCHITECTURE_V3,FEATURE_TREE_V2}.md`, `docs/system-architecture.md`, `README.md`, `services/learning-service/README.md`.
- DATABASE_V5: V5.4 là triển khai thực tế 2026-10-02, `hint` ở §5.5, lịch sử mọi response tại Java §7.4; index đã có trong Learning `V1__learning_schema.sql`. Không thêm bảng/cột Learning. V11/§7.23 bên dưới là thiết kế Python lịch sử, không phải schema chạy hiện tại.
- Ranh giới rõ: Content lưu và trả hint qua endpoint nội bộ bài học; Learning Java quyết định theo user/bài/khối. Pending note đã reconciled với code; review DTO hint luôn null; replay/evidence/mastery giữ nguyên. Contract học viên hoàn thành cùng code (phase 1).
- Reviewer không finding. Graphify update exit 0: 8.147 node, 28.230 edge. Validator `docs/system-architecture.md`: 7 link OK, 49 warning (15 code reference + 34 config key, gồm FILL/CHOICE bị hiểu là env và reference ngoài source Learning đã chọn). Không tuyên bố validator clean.
- 19 JSON example trong contract parse hợp lệ; `git diff --check` pass. 20 file sửa không có UTF-8 BOM trước lượt sync plan; không chạy full reactor/live E2E, không push.

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

- [x] DATABASE_V5 có `hint` ở §5.5, dòng V5.4 thực tế ở §0; lịch sử và index Learning Java tại §7.4/V1.
- [x] `SERVICE_ARCHITECTURE_V3.md` và `docs/system-architecture.md` ghi rõ Content lưu, Learning Service Java quyết định hiện.

## Risk Assessment

- **Trùng phiên bản V5.4:** đã giải quyết; dòng V5.4 hiện ghi đúng triển khai Reading hints 2026-10-02.
- **Validator warning:** còn 49 cảnh báo; owner maintainer tài liệu/tooling, kiểm lại với đúng source scope hoặc sửa nhận diện enum khi thực hiện cải tiến validator. Link đã kiểm OK; cảnh báo không bị ghi thành kết quả clean.
- **Thay đổi phạm vi đã chấp nhận:** mapping Python sang Java; từ 3 tài liệu gốc mở thành 7 tài liệu hiện hành để khép pending note và hướng dẫn người chạy. Không đổi nghiệp vụ hoặc schema Learning.
