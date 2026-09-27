---
phase: 8
title: "Lưu ghi chú từ buổi học vào notes của learning-support"
status: pending
priority: P3
dependencies: [4]
effort: "~1.5d"
---

# Phase 8: Lưu ghi chú từ buổi học vào notes của learning-support

## Overview
Học viên bảo tutor "lưu lại đoạn này", nội dung được ghi thành **một note của học viên** trong `learning-support-service`,
kèm nguồn (buổi học, KP). Ý tưởng từ Notebook của DeepTutor, nhưng dùng sổ ghi chú sẵn có của IELTSPath.

## Đọc trước
- `services/learning-support-service`: `NoteController` (`/api/learning-support/notes`), `CreateNoteRequest` (title ≤ 255,
  body ≤ 20.000), `NoteResponse`, `V1__create_learning_support_tables.sql` (bảng `notes`; `flashcards` có `source_type`,
  `source_reference_id` để tham chiếu cách đặt tên).
- `app/clients/user_service.py`, `app/clients/content_service.py`: mẫu client HTTP chuyển tiếp internal JWT của học viên.

## Requirements
- **learning-support-service (Java)**:
  - Migration `V{n}__note_source.sql` (`n` = số phiên bản kế tiếp còn trống; pha 10 cũng thêm migration ở service này): `notes.source_type varchar(50) NULL` với CHECK trong `TUTOR_SESSION`, `KNOWLEDGE_POINT`,
    `PRACTICE`, `READING`; `notes.source_reference_id varchar(255) NULL`; index `(user_id, source_type, source_reference_id)`.
  - `CreateNoteRequest` thêm hai trường tùy chọn; `NoteResponse` trả ra; `GET /notes` lọc được theo `sourceType`,
    `sourceReferenceId`. Note do tutor tạo vẫn sửa, xóa như note thường.
- **AI Learning**:
  - `app/clients/learning_support.py`: `POST /api/learning-support/notes` bằng internal JWT của học viên, đi thẳng như các
    client hiện có.
  - Tool `save_note(title, body)`: nguồn là session hiện tại (và KP đang học). Body quá 20.000 ký tự thì cắt, có đánh dấu.
  - learning-support lỗi → tool trả lỗi cho LLM; lượt học không hỏng.
- Không ghi nội dung note vào log.

## Tests
1. Java: tạo note có và không có nguồn; lọc theo nguồn; `sourceType` sai → 400; migration trên dữ liệu cũ.
2. Python: tutor lưu note → learning-support giả nhận POST đúng bearer, title, body, nguồn; learning-support lỗi → lượt vẫn xong.

## Success Criteria
- [ ] Nội dung tutor mà học viên chọn lưu xuất hiện trong notes của họ, lọc được theo buổi học hoặc KP.
