---
phase: 4
title: "Lưu trữ tutor: session, message, turn"
status: pending
priority: P1
dependencies: [3]
effort: "~1.5d"
---

# Phase 4: Lưu trữ tutor

## Overview
Bảng và store PostgreSQL cho buổi học của tutor. Thiết kế **gọn hơn** V5 §7.7–7.9, vì V5 sao chép runtime nhiều worker
của DeepTutor (`owner_id`, `fencing_token`, `state_version`, `turn_events`, `mastery_path_sessions`, `mastery_path_leases`).
Tutor tự viết chạy một instance, mỗi turn là một request HTTP, nên không cần các cột đó (YAGNI). Pha 7 cập nhật V5 cho khớp.

## Đọc trước
- `.sdd/database/DATABASE_V5.md` §7.7 `sessions`, §7.8 `messages`, §7.9 `turns` (để giữ tên cột khi có thể).
- `migrations/V0_1__create_v5_mastery_tables.sql` (`mastery_interactions` có `session_id`, `turn_id` kiểu uuid, dùng lại).
- `app/persistence/postgres_learning_store.py` (phong cách: psycopg2, mở kết nối theo unit of work, `RealDictCursor`).
- `tests/postgres_schema_support.py`, `tests/test_migrations_postgres.py`.

## Requirements
- Migration `migrations/V5__tutor_sessions.sql`:
  - `sessions`: `id uuid PK`, `user_id uuid NOT NULL`, `path_id uuid NOT NULL REFERENCES mastery_paths ON DELETE CASCADE`,
    `title varchar(200) NOT NULL DEFAULT 'New session'`, `created_at`, `updated_at timestamptz NOT NULL`,
    `archived_at timestamptz NULL`. Index `(user_id, updated_at DESC) WHERE archived_at IS NULL`.
  - `turns`: `id uuid PK`, `session_id uuid NOT NULL REFERENCES sessions ON DELETE CASCADE`,
    `status varchar(20) NOT NULL CHECK (status IN ('running','completed','failed'))`, `failure_code varchar(100) NOT NULL
    DEFAULT ''`, `created_at`, `finished_at timestamptz NULL`. **Partial unique index** `(session_id) WHERE status = 'running'`.
  - `messages`: `id bigint GENERATED ALWAYS AS IDENTITY PK`, `session_id uuid NOT NULL REFERENCES sessions ON DELETE
    CASCADE`, `turn_id uuid NULL REFERENCES turns`, `role varchar(20) NOT NULL CHECK (role IN ('user','assistant'))`,
    `content text NOT NULL`, `metadata_json jsonb NOT NULL DEFAULT '{}'`, `created_at timestamptz NOT NULL`.
    Index `(session_id, id)`.
  - Tool call và tool result **không** lưu thành message; câu hỏi và kết quả chấm đã nằm ở `mastery_interactions`.
    `metadata_json` của message assistant ghi `question_id` nếu turn đó đặt câu hỏi.
- `app/tutor/session_store.py`, class `TutorSessionStore(database_url)`. Mọi method nhận `user_id` và **kiểm chủ sở hữu
  trong SQL** (JOIN `sessions … WHERE user_id = %s AND archived_at IS NULL`); không thuộc → trả `None`/`False`, không lộ
  sự tồn tại:
  - `create_session(user_id, path_id, title)`: path phải thuộc `user_id` (`mastery_paths.user_id`).
  - `list_sessions(user_id)`, `get_session(user_id, session_id)`, `archive_session(user_id, session_id)`.
  - `begin_turn(user_id, session_id) -> turn_id`: vi phạm unique index → `ActiveTurnConflict`.
  - `finish_turn(turn_id, status, failure_code='')`.
  - `add_message(user_id, session_id, turn_id, role, content, metadata)`, `recent_messages(user_id, session_id, limit)`.
  - `recover_interrupted_turns()`: chuyển mọi turn `running` sang `failed`, `failure_code='interrupted'`. Gọi khi API khởi
    động (pha 6), vì một instance duy nhất nên turn `running` lúc khởi động chắc chắn đã chết.
- Không log nội dung message.

## Implementation Steps
### Tests Before (PostgreSQL, schema tạm như các test hiện có)
1. Migration chạy trên DB có dữ liệu của V0_1–V4; `test_migrations_postgres.py` cập nhật danh sách bảng.
2. Chủ sở hữu: học viên B không đọc, liệt kê, archive, bắt đầu turn hay ghi message vào session của A; không tạo được
   session trên path của A.
3. Hai `begin_turn` đồng thời trên cùng session (hai thread) → đúng một thành công, một `ActiveTurnConflict`.
4. `recover_interrupted_turns` → turn `running` thành `failed/interrupted`, `begin_turn` mới chạy được.
5. Archive → không còn trong `list_sessions`, `get_session` trả `None`.
### Refactor
6. Migration và store.
### Tests After
7. Gate đầy đủ.

## Success Criteria
- [ ] Ba bảng có ràng buộc đúng; store kiểm chủ sở hữu ở mọi method; một turn chạy mỗi session.
