---
phase: 3
title: "SessionStore adapter (sessions, messages, turns, question notebook)"
status: pending
priority: P1
dependencies: [2]
effort: "~3d"
---

# Phase 3: SessionStore adapter

## Overview
Viết `PostgresSessionStore` thỏa `SessionStoreProtocol` của DeepTutor cho các bảng Tutor runtime mà V5 đã thiết kế:
`sessions`, `messages`, `turns`, `turn_events`, `notebook_entries`, `practice_review_state`, `practice_review_events`.
Mỗi session thuộc về một `user_id`.

## Đọc trước khi code
- `third_party/deeptutor/deeptutor/services/session/protocol.py` (hợp đồng), `sqlite_store.py` (bản tham chiếu: schema
  từ dòng ~269, `begin_turn`, `transition_turn`, `append_events`, active turn unique, `ActiveTurnConflict`).
- `services/practice/storage.py`, `agents/question/history.py` (question notebook và practice review dùng bảng nào).
- `.sdd/database/DATABASE_V5.md` §7.7–7.13.
- Report pha 1: method coverage của session store.

## Requirements
- Functional:
  - Migration `V6__tutor_session_runtime.sql` theo V5 §7.7–7.13:
    - các bảng: `sessions` (thêm `user_id`), `messages`, `turns`, `turn_events`, `notebook_entries`,
      `practice_review_state`, `practice_review_events`;
    - partial unique index "một active turn mỗi session";
    - khóa ngoại từ bảng pha 2 tới `sessions` và `turns`.
    - `reading_quiz_pending` của DeepTutor thêm ở pha 11 nếu reading cần.
    - Không tạo `notebook_categories` (V5 để sau).
  - Adapter:
    - Cài mọi method trong coverage, cộng các method mà router của DeepTutor dùng để liệt kê, xem, xóa session trong pha 4.
    - Method ngoài phạm vi (import legacy, workspace preferences migration…) ném `NotImplementedError` có tên.
    - Lọc theo current user ở mọi truy vấn.
    - `ActiveTurnConflict` giữ đúng semantics: unique index vi phạm → ném đúng lớp lỗi của DeepTutor.
    - `fencing_token` và `state_version` của turn giữ semantics chống worker cũ ghi đè.
  - Bootstrap: `deeptutor.services.session.get_session_store`, `get_sqlite_session_store` và
    `get_sqlite_session_store_for` trả adapter.
- Non-functional:
  - Không lưu nội dung message vào log.
  - Xóa session là soft delete (`archived_at`); hard delete chỉ qua API admin (không làm trong plan này).

## Implementation Steps
### Tests Before
1. Test hợp đồng lấy kịch bản từ test của DeepTutor cho `SQLiteSessionStore`, chạy trên adapter:
   - vòng đời turn: queued → running → waiting_input → completed;
   - `append_events` theo `seq`, `get_events(after_seq)`;
   - hai turn active trên cùng session → `ActiveTurnConflict`;
   - message branching qua `parent_message_id`;
   - summary.
2. Phạm vi người dùng: user khác không đọc, liệt kê hay xóa được session.
3. Question notebook: idempotency `UQ(session_id, turn_id, question_id)`; review cập nhật `practice_review_state` và ghi event.
### Refactor
4. Migration, adapter, bootstrap.
### Tests After
5. Gate đầy đủ; spike pha 1 chạy lại với adapter đầy đủ.

## Success Criteria
- [ ] Turn study của pha 1 chạy với adapter đầy đủ; mọi bảng runtime có dữ liệu đúng.
- [ ] Không file `chat_history.db` nào được tạo.
