---
phase: 5
title: "Tài liệu"
status: pending
priority: P3
dependencies: [3, 4]
effort: "0.5 ngày"
---

# Phase 5: Tài liệu

## Overview

Ghi Listening, media URL và luật giấu transcript vào tài liệu nguồn. Chỉ ghi điều code đã làm.

Thiết kế đích đã được ghi ngày 2026-09-30 (phần chọn gói theo dạng câu và độ khó đã ghi là hoãn ngày 2026-10-01):
- `DATABASE_V5.md`: V5.3, §4.2 (ghi chú `learning_type`), §5.18, §6.2, §7.25;
- `SERVICE_ARCHITECTURE_V3.md`: §1, §6, §7, §8.2.

Phase này đối chiếu các mục đó với code thật, sửa chỗ lệch, bỏ nhãn "thiết kế đích" của V5.3 khi mọi plan V5.3 đã xong, và
làm các tài liệu còn lại trong danh sách dưới.

## Requirements

- `.sdd/database/DATABASE_V5.md`:
  - §5.8 `content_assets`: AUDIO lưu key hoặc URL `https` ở `media_reference`, transcript ở `text_content`; ghép URL bằng
    `CONTENT_MEDIA_BASE_URL`;
  - §5.14 khối `ASSET` AUDIO trong bài học;
  - §6.2 `attempt_sections.section_snapshot`: khóa `audio`, `solution` (chỉ server đọc);
  - lịch sử phiên bản thêm dòng Listening.
- `docs/system-architecture.md`: media trên cloud, content là nơi duy nhất ghép URL; điểm chưa nhất quán: bucket public, chưa
  signed URL, chưa upload API.
- `services/content-service/README.md`: danh sách key mp3 của seed và thời lượng; biến `CONTENT_MEDIA_BASE_URL`.
- `CLAUDE.md` gốc §5 (cạm bẫy): thiếu `CONTENT_MEDIA_BASE_URL` thì bài Listening lỗi `INVALID_MEDIA_REFERENCE`.
- `docs/contracts/*`: kiểm lại khớp code.

## Related Code Files

- Modify: `.sdd/database/DATABASE_V5.md`, `docs/system-architecture.md`, `services/content-service/README.md`, `CLAUDE.md`

## Implementation Steps

1. Đọc từng tài liệu, đối chiếu với seed, resolver và DTO thật.
2. Sửa theo danh sách; giữ cấu trúc và giọng văn.
3. `graphify update .` sau khi code đã merge.

## Success Criteria

- [ ] Tài liệu khớp code; không có giá trị secret hay URL bucket thật trong tài liệu nếu bucket là của cá nhân.

## Risk Assessment

- Trùng chỗ sửa với phase tài liệu của các plan Writing và plan chia service: sửa sau khi các plan đó merge, chỉ thêm mục Listening.
