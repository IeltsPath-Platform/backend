---
phase: 5
title: "Tài liệu"
status: completed
priority: P3
dependencies: [3, 4]
effort: "0.5 ngày"
---

# Phase 5: Tài liệu

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java (plan `261001-1228`). Mọi tên file, lệnh và API Python dưới đây đọc theo [bảng ánh xạ](../260930-2057-mvp-reading-writing-listening-roadmap/python-to-java-mapping.md); luật nghiệp vụ, mã lỗi và test case giữ nguyên.

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

- [x] Tài liệu khớp code; không thêm giá trị secret hay URL bucket của cá nhân vào tài liệu.

## Risk Assessment

- Trùng chỗ sửa với phase tài liệu của các plan Writing và plan chia service: sửa sau khi các plan đó merge, chỉ thêm mục Listening.

## Kết quả

- Hoàn tất 2026-10-02 trên nhánh `feat/lesson-writing-listening-docs`, sau khi hoàn tất tài liệu Writing Task 1; không push.
- Đồng bộ Listening trong `DATABASE_V5.md` và các mục Listening của `SERVICE_ARCHITECTURE_V3.md`: schema hiện thực, audio key/https, transcript, snapshot allowlist và `sectionSolutions` khi kết quả ≥70%. Chọn gói theo dạng câu/độ khó vẫn hoãn ngoài MVP.
- Kiến trúc, README Content và `CLAUDE.md` ghi rõ Content là nơi duy nhất ghép URL; thiếu base chỉ lỗi với reference dạng key; mp3 do team upload, chưa có upload API/signed URL. Đối chiếu đủ 8 key và thời lượng V12, không đổi bảng seed đã đúng.
- Đối chiếu `lesson-learning-v1`, `learning-content-internal-v1`, `assessment-completed-v2` với resolver, DTO và use case Java; contract đã khớp nên không sửa. Event không chứa transcript, lời giải từng câu giữ nguyên.
- Kiểm nguồn, link Markdown, UTF-8, `git diff --check` và `graphify update .`; không chạy lại Maven/E2E cho thay đổi chỉ ở tài liệu theo yêu cầu kiểm tra vừa đủ. Không sửa code, config hoặc migration.
- Validator tài liệu exit 0, 7 internal link hợp lệ; còn 47 warning về tên Java/config (15/32) do giới hạn scanner với layout Maven/config-repo, không coi là một lần kiểm sạch warning. Các phần thêm đã được đối chiếu trực tiếp với nguồn.
