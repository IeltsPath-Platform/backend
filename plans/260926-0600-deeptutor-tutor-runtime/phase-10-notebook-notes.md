---
phase: 10
title: "Notebook vào notes của learning-support-service"
status: pending
priority: P2
dependencies: [6]
effort: "~1.5d"
---

# Phase 10: Notebook vào notes của learning-support-service

## Overview
Notebook của DeepTutor (`NotebookManager`) cho phép lưu một đoạn trả lời hay kết quả của Tutor thành record. IELTSPath đã
có sổ ghi chú của học viên: bảng `notes` của `learning-support-service`, API `/api/learning-support/notes`. Theo T3,
"lưu vào notebook" của DeepTutor sẽ ghi thành **một note của học viên**, có thêm nguồn để biết note đến từ KP hay buổi học
nào.

## Đọc trước khi code
- `third_party/deeptutor/deeptutor/services/notebook/service.py` (`NotebookManager.add_record`, `NotebookRecord`,
  `RecordType`), `api/routers/notebook.py`; tìm tool hay capability nào gọi `add_record`.
- `services/learning-support-service`:
  - `NoteController`, `CreateNoteRequest`, domain và JPA của note;
  - migration `V1__create_learning_support_tables.sql` (bảng `notes`; bảng `flashcards` có `source_type`,
    `source_reference_id` để tham chiếu cách đặt tên).
- `app/clients/user_service.py`: mẫu client HTTP chuyển tiếp bearer của học viên.

## Requirements
- Functional:
  - **learning-support-service**:
    - Migration `V2__note_source.sql`: `notes.source_type varchar(50) NULL`, `notes.source_reference_id varchar(255) NULL`
      (id của session, KP hay turn là chuỗi UUID), index `(user_id, source_type, source_reference_id)`.
    - `CreateNoteRequest` thêm hai trường tùy chọn. `NoteResponse` trả chúng ra. `GET /notes` lọc được theo
      `sourceType`, `sourceReferenceId`.
    - `source_type` là một trong `TUTOR_SESSION`, `KNOWLEDGE_POINT`, `READING`, `PRACTICE` (CHECK constraint).
      Để null khi học viên tự tạo note.
    - Note do Tutor tạo vẫn là của học viên: sửa, xóa như note thường.
  - **AI Learning**:
    - `app/clients/learning_support.py`: `POST /api/learning-support/notes`, dùng internal JWT của học viên (như
      User/Content client). Service-to-service đi thẳng như hiện nay, không qua Gateway.
    - Adapter `NotebookManager` qua bootstrap:
      - `add_record` → tạo note; title lấy từ record, body lấy từ output đã làm sạch (`clean_thinking_tags`), nguồn lấy
        từ session, turn hoặc KP của record;
      - `list_notebooks` / `get_records` đọc lại từ notes theo nguồn.
      - Các thao tác không có tương đương (nhiều notebook có tên) → một "notebook" ảo duy nhất là notes của học viên.
    - Lỗi gọi learning-support (service tắt) → tool trả lỗi cho LLM; không làm hỏng turn.
- Non-functional:
  - Body tối đa 20.000 ký tự (giới hạn của `CreateNoteRequest`), cắt có đánh dấu.
  - Không ghi nội dung note vào log.

## Implementation Steps
### Tests Before
1. Java:
   - tạo note có và không có nguồn;
   - lọc theo nguồn;
   - `source_type` sai → 400;
   - migration V2 trên dữ liệu cũ.
2. Python:
   - Tutor lưu một record → learning-support nhận POST đúng bearer của học viên, title, body, nguồn (HTTP giả);
   - learning-support lỗi → turn vẫn xong, tool báo lỗi.
3. Không file notebook nào được ghi vào `deeptutor-data/`.
### Refactor
4. Migration và API Java; client và adapter Python.
### Tests After
5. Gate Java của learning-support và gate Python.

## Success Criteria
- [ ] Nội dung Tutor mà học viên chọn lưu xuất hiện trong notes của họ, lọc được theo KP hoặc buổi học.
