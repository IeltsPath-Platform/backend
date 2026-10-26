---
phase: 5
title: "Docs and flashcard API contract"
status: completed
priority: P2
dependencies: [1, 4]
---

# Phase 5: Docs and flashcard API contract
<!-- Updated: Validation Session 2 - contract for linking the flashcard module; no review endpoint or gate -->

## Overview
Ghi lại cách lấy từ vựng của lesson để module flashcard (library Leitner) nối vào sau. Không có endpoint review riêng,
không có gate.

## Requirements
Mở rộng `docs/contracts/lesson-learning-v1.md`, không tạo file mới:
- **`GET /lessons/{id}`:** block `VOCABULARY` (`blockType: "VOCABULARY"`) mang `vocabularySenseIds` theo thứ tự học. Kèm
  ví dụ JSON. Ghi rõ block này không có `passed` và không ảnh hưởng completion.
- **Nội dung thẻ:** `GET /api/content/vocabulary/senses?ids=` (library). Ghi path, query, shape, giới hạn 100 id, và việc
  chỉ trả sense ACTIVE theo thứ tự request.
- **Nối module flashcard (hướng dẫn, chưa làm):** mỗi `vocabularySenseId` tương ứng một thẻ `sourceType:
  VOCABULARY_SENSE` trong `flashcard-srs-v1.md`. Việc tạo deck theo lesson và định nghĩa "học xong" thuộc vòng sau.
- **Mastery:** từ vựng lesson không ghi `kp_evidence`. Lý do: tài liệu kiến trúc tách SRS khỏi Mastery Engine; mastery
  VOCABULARY chỉ đến từ bài tập chấm điểm.

`docs/system-architecture.md`:
- Mục "Học topic và bài": thêm một dòng "block VOCABULARY = danh sách sense của library, không chặn completion".
- Bảng quyết định: thêm "từ vựng lesson/flashcard không ghi `kp_evidence`; mastery VOCABULARY chỉ từ bài tập chấm điểm".

## Related Code Files
- Modify: `docs/contracts/lesson-learning-v1.md`
- Modify: `docs/system-architecture.md`

## Implementation Steps
1. Đọc `lesson-learning-v1.md` để theo đúng format (mục endpoint, ví dụ JSON).
2. Thêm các mục trên, đối chiếu với response thật lấy từ Swagger hoặc kiểm thủ công ở phase 4.
3. Cập nhật `system-architecture.md`, ghi ngày cập nhật.
4. Gửi đoạn contract cho dev flashcard để họ biết điểm nối.

## Success Criteria
- [x] Ví dụ trong contract khớp response thật (đối chiếu DTO và V72).
- [x] Docs kiến trúc phản ánh quyết định về mastery.

## Risk Assessment
- **Dev flashcard cần thêm field:** báo trước, cập nhật contract cùng commit với code.


## Progress — 2026-10-21

- Cập nhật `lesson-learning-v1.md` với block VOCABULARY, danh sách sense theo thứ tự, batch Library endpoint và cách nối `VOCABULARY_SENSE`; nêu rõ SRS không tạo `kp_evidence`.
- Cập nhật `system-architecture.md` với luồng đọc vocabulary và quyết định tách mastery, ghi ngày 2026-10-21.
- Đối chiếu JSON/field với response DTO, API controller và V72 migration; cả hai tiêu chí phase 5 hoàn tất.
