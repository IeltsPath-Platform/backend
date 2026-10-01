---
phase: 3
title: "Tài liệu"
status: pending
priority: P3
dependencies: [2]
effort: "0.5 ngày"
---

# Phase 3: Tài liệu

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java (plan [`261001-1228`](../261001-1228-learning-service-java/plan.md)). Mọi tên file, lệnh và API Python dưới đây đọc theo [bảng ánh xạ](../261001-1228-learning-service-java/python-to-java-mapping.md); luật nghiệp vụ, mã lỗi và test case giữ nguyên.

## Overview

Ghi Task 1 vào tài liệu nguồn. Chỉ ghi điều code đã làm.

## Requirements

- `.sdd/database/DATABASE_V5.md`:
  - §5.5 bảng `answer_spec`: dòng `ESSAY` có `task = TASK_1 | TASK_2`; `chartFacts` bắt buộc với Task 1;
  - §5.9 `content_asset_links`: ảnh biểu đồ Task 1 gắn theo `question_version_id`; `text_content` của IMAGE là alt text;
    `media_reference` chỉ `https://` hoặc data URI ảnh (sai → `INVALID_MEDIA_REFERENCE`); API trả `mediaUrl`;
  - §5.16 luật khối essay Task 1 (có `chartFacts`, ≥ 1 ảnh);
  - §7.27 `lesson_writing_submissions`: `prompt_snapshot` chứa `task`, `chartFacts`, `images`; bộ mã tiêu chí theo task.
- `docs/contracts/*`: kiểm lại khớp code sau khi merge.
- `services/ai-learning-service/README.md`: Task 1, bộ mã tiêu chí.
- `docs/system-architecture.md` §11 (điểm chưa nhất quán): ảnh dùng URL hoặc data URI vì chưa có object storage; Writing
  trong đề cuối chưa làm, chờ chính sách phí.

## Related Code Files

- Modify: `.sdd/database/DATABASE_V5.md`, `docs/system-architecture.md`, `services/ai-learning-service/README.md`

## Implementation Steps

1. Đọc từng tài liệu, đối chiếu với V11 và code ai-learning thật.
2. Sửa theo danh sách; giữ giọng văn và cấu trúc sẵn có.
3. `graphify update .` sau khi code đã merge.

## Success Criteria

- [ ] Tài liệu khớp code; không nhắc số phase hay mã plan trong tài liệu sản phẩm.

## Risk Assessment

- `docs/system-architecture.md`: chỉ thêm mục riêng, không sửa mục của plan khác cùng lúc (plan kiến trúc chia service đã xóa 2026-09-30).