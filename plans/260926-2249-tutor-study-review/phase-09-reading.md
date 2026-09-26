---
phase: 9
title: "Đọc bài Reading của Content cùng tutor"
status: blocked
priority: P3
dependencies: [4]
effort: "~2d"
---

# Phase 9: Đọc bài Reading của Content cùng tutor

## Trạng thái: blocked
Content chưa có API trả **text** bài đọc: hiện chỉ có `/api/content/packages`, `/api/content/assets/{id}`,
`/api/content/videos`. Cần nhóm quyết định thêm API ở Content (ví dụ nội dung một section Reading dạng text phân đoạn)
trước khi làm pha này.

## Overview
Học viên mở một bài Reading của Content; tutor hỏi đáp trên bài, giải thích từ vựng, và cho quiz ngắn trên bài. Ý tưởng
từ capability reading của DeepTutor, tự viết.

## Đọc trước
- Content: `content_packages`, `content_sections`, `content_assets` (`.sdd/database/DATABASE_V5.md` §5).
- Tham khảo, chỉ đọc: `third_party/deeptutor/deeptutor/capabilities/reading/`.

## Requirements (khi hết bị chặn)
- Session có thể gắn một bài đọc (`material_id` = id section/asset Reading của Content). Nội dung lấy qua Content API bằng
  internal JWT của học viên, chỉ đọc, không lưu bản sao.
- Tool `reading_passage(paragraph?)` trả đoạn văn; `reading_quiz` đặt câu hỏi trên bài (dùng lại cơ chế câu hỏi của pha 2
  hoặc pha 6, không ghi evidence trừ khi nhóm quyết định khác).
- Lưu từ hoặc đoạn hay: dùng `save_note` với nguồn `READING` (pha 8).

## Tests (khi hết bị chặn)
1. Mở bài Content → hỏi một câu → câu trả lời trích đúng đoạn.
2. Quiz trên bài → chấm → sổ câu hỏi (pha 6).

## Success Criteria
- [ ] Học viên đọc bài IELTS của Content cùng tutor.
