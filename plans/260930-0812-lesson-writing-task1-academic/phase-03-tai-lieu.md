---
phase: 3
title: "Tài liệu"
status: completed
priority: P3
dependencies: [2]
effort: "0.5 ngày"
---

# Phase 3: Tài liệu

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java (plan `261001-1228`). Mọi tên file, lệnh và API Python dưới đây đọc theo [bảng ánh xạ](../260930-2057-mvp-reading-writing-listening-roadmap/python-to-java-mapping.md); luật nghiệp vụ, mã lỗi và test case giữ nguyên.

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

- [x] Tài liệu khớp code; không thêm số phase hay mã plan vào nội dung sản phẩm.

## Risk Assessment

- `docs/system-architecture.md`: chỉ thêm mục riêng, không sửa mục của plan khác cùng lúc (plan kiến trúc chia service đã xóa 2026-09-30).

## Kết quả

- Hoàn tất 2026-10-02 trên nhánh `feat/lesson-writing-listening-docs`, tạo từ `feat/main-follow` commit `512f465`; không push.
- Đồng bộ `DATABASE_V5.md` §5.5, §5.9, §5.16 và §7.10 (thay số mục Python §7.27): Task 1/Task 2, chartFacts, ảnh, mã lỗi media, prompt snapshot và bộ tiêu chí theo task.
- Bổ sung Writing trong README của Learning Service Java và kiến trúc §11: grader đọc chartFacts, không đọc ảnh; Writing trong đề cuối còn chờ chính sách phí.
- Đối chiếu `lesson-writing-v1`, `learning-content-internal-v1` và `answer-spec-v1` với `LessonBlockKind`, `MediaReferencePolicy`, DTO, `EssayPrompt`, `WritingTask` và `EssayGrader`; contract đã khớp nên không sửa.
- Kiểm tra nguồn, link Markdown, UTF-8 và `git diff --check`; không chạy lại Maven/E2E vì chỉ sửa tài liệu và người dùng yêu cầu kiểm tra vừa đủ. Không sửa code, migration hoặc cấu hình.
