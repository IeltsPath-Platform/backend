---
phase: 11
title: "Reading với bài đọc từ Content"
status: pending
priority: P3
dependencies: [6]
effort: "~2d"
---

# Phase 11: Reading với bài đọc từ Content

## Overview
Bật capability `reading` của DeepTutor (đọc có hướng dẫn: hỏi đáp trên bài, quiz, từ vựng, dịch). DeepTutor mở "reading
material" từ kho tài liệu của chính nó. Với IELTS, material phải là **bài đọc của Content**. Pha này cấp material từ Content
qua một adapter, không dùng thư viện file hay knowledge base (T4).

## Đọc trước khi code
- `capabilities/reading/{capability,tools,mode}.py`: material được định danh và đọc thế nào (locator, trang, đoạn); tool
  nào cần gì.
- `services/reading_hints.py`, `api/routers/reading.py`, `reading_extensions.py`; bảng `reading_quiz_pending` trong
  `sqlite_store.py`.
- Content: `content_packages`, `content_sections`, `content_assets` (DATABASE_V5 §5); API hiện có để lấy nội dung một section Reading.

## Requirements
- Functional:
  - **Spike đầu pha:** xác định giao diện material mà `reading` cần (text phân đoạn? PDF?) và Content có đủ không. Nếu
    Content chưa có API trả nội dung bài đọc dạng text thì dừng lại và hỏi (có thể phải thêm API ở Content).
  - Adapter material: `material_id` = id section hoặc asset Reading của Content; nội dung lấy qua Content API bằng bearer
    của học viên, chỉ đọc.
  - Thêm bảng `reading_quiz_pending` vào adapter session (migration) nếu tool reading quiz dùng.
  - Lưu từ hay đoạn khi đọc: tới notes (pha 10) với `source_type = READING`, hoặc flashcards của learning-support nếu
    DeepTutor có tool vocabulary. Nếu nối vào flashcards thì cần quyết định thêm; mặc định chỉ notes.
- Non-functional: không lưu bản sao bài đọc vào `deeptutor-data/`.

## Implementation Steps
1. Spike, report ngắn, rồi hỏi nếu thiếu API Content.
2. Test: mở material Content → hỏi một câu → câu trả lời trích đúng đoạn; quiz reading → pending → chấm.
3. Adapter, nối capability.
4. Gate đầy đủ.

## Success Criteria
- [ ] Học viên đọc bài IELTS của Content cùng Tutor, bằng capability reading của DeepTutor.
