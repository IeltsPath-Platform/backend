---
phase: 1
title: "AI Learning baseline schema and Flyway"
status: pending
priority: P1
dependencies: []
effort: "~2h"
---

# Phase 1: AI Learning baseline schema and Flyway

## Overview
Tạo DDL gốc V5 cho `ai_learning_db` thành migration Flyway đầu chuỗi (`V0_1__`), giữ nguyên V1 và V2. Test Python dùng đúng chuỗi migration này thay cho DDL chép tay, để chỉ có một nguồn sự thật.

## Context
- Hiện trong repo không có DDL cho `mastery_paths`, `mastery_interactions`, `mastery_events`. V1 và V2 giả định các bảng này đã tồn tại.
- `tests/postgres_schema_support.py` đang chép tay DDL (`BASE_V5_TABLES`), dễ lệch với schema thật.
- Đã xác minh: Flyway 11 (theo BOM Boot 3.5.14) áp `V0_1` → `V1` → `V2` đúng thứ tự trên DB rỗng.
- Tham chiếu: `.sdd/database/DATABASE_V5.md` mục 7.1, 7.3, 7.4; DDL SQLite gốc trong `third_party/deeptutor/deeptutor/learning/storage.py:375-485`.

## Requirements
- Functional:
  - DB rỗng chạy `V0_1 → V1 → V2` thành công.
  - Chỉ tạo các bảng store đang dùng: `mastery_paths`, `mastery_interactions`, `mastery_events`. Không tạo `mastery_path_sessions`/`mastery_path_leases` (YAGNI).
  - `mastery_interactions.status` nhận đúng giá trị DeepTutor ghi (chữ thường: `registered`, `awaiting_input`, `answered`, `graded`, `abandoned`).
  - Có partial unique index "tối đa 1 interaction active mỗi path", như upstream.
- Non-functional:
  - Không sửa V1/V2 đã có.
  - Không thêm thư viện Python.
  - Thứ tự áp migration trong test tính theo số version, không theo chữ (tránh lỗi `V10` đứng trước `V2`).

## Architecture
```text
migrations/V0_1__create_v5_mastery_tables.sql   mastery_paths, mastery_interactions, mastery_events
migrations/V1__one_mastery_path_per_learning_goal.sql   (giữ nguyên) unique (user_id, learning_goal_id)
migrations/V2__formal_assessment_evidence.sql   (giữ nguyên) evidence projection + result-version ledger
        │
        ├── Flyway container (pha 4) ──► ai_learning_db
        └── tests/postgres_schema_support.py ──► schema tạm cho mỗi test class
```
Bảng theo V5 (các FK tới `sessions`/`turns` bỏ vì bảng đó chưa tồn tại):
- `mastery_paths`: `path_id` PK, `user_id` NOT NULL, `learning_goal_id`, `state_json` JSONB NOT NULL, `revision` BIGINT NOT NULL CHECK ≥ 0, `owner_session_id`, `created_at`/`updated_at` TIMESTAMPTZ NOT NULL; index `(user_id, updated_at DESC)` và `(learning_goal_id)`.
- `mastery_interactions`: `interaction_id` PK, `path_id` FK CASCADE, `status` CHECK chữ thường, `question_json` JSONB, `session_id`/`turn_id` UUID nullable, `user_answer` TEXT DEFAULT '', `result_json` JSONB DEFAULT '{}', timestamps; `UNIQUE (path_id) WHERE status IN ('registered','awaiting_input','answered')`.
- `mastery_events`: `id` BIGINT IDENTITY PK, `path_id` FK CASCADE, `revision`, `event_type` VARCHAR(100), `payload_json` JSONB DEFAULT '{}', `session_id`/`turn_id` UUID, `created_at`; index `(path_id, revision, id)`.

## Related Code Files
- Create: `services/ai-learning-service/migrations/V0_1__create_v5_mastery_tables.sql`
- Create: `services/ai-learning-service/tests/test_migrations_postgres.py`
- Modify: `services/ai-learning-service/tests/postgres_schema_support.py` (bỏ `BASE_V5_TABLES`; áp mọi `V*.sql` theo version số)
- Modify: `services/ai-learning-service/README.md` (mục migration: Flyway là runner; ghi chú baseline cho DB đã áp tay)

## Implementation Steps
### Tests Before (khóa hành vi hiện tại)
1. Chạy toàn bộ suite Python với `AI_LEARNING_TEST_DATABASE_URL`. Mốc hiện tại: 57 pass, 3 skip (RabbitMQ). Ghi lại kết quả làm baseline.
2. Viết `tests/test_migrations_postgres.py`, để **fail trước** khi có V0_1:
   - Schema rỗng + chỉ migrations của repo (không dùng DDL tay) thì phải có đủ 3 bảng, index unique goal (V1) và unique evidence (V2).
   - Hàm sắp xếp migration trả đúng `['0.1','1','2']`, và `V10` phải đứng sau `V2`.
### Refactor
3. Viết `V0_1__create_v5_mastery_tables.sql` theo phần Architecture.
4. `postgres_schema_support.py`: xóa `BASE_V5_TABLES`; sắp xếp file theo version số (tách `V(\d+(?:_\d+)*)__`, so sánh từng phần số); áp tuần tự.
### Tests After
5. `test_migrations_postgres.py` phải pass và bổ sung:
   - `status` chữ thường được nhận, `REGISTERED` chữ hoa bị CHECK từ chối.
   - Insert interaction active thứ 2 trên cùng path bị unique từ chối.
6. Chạy lại toàn bộ suite (các test PG giờ dựng schema từ migrations thật).
### Regression Gate
7. Suite Python với PostgreSQL: 0 fail. Tổng pass = mốc cũ + test mới, không tính 3 test RabbitMQ đang skip.
8. `python -m compileall -q app main.py tests`; `git diff --check`; `graphify update .`

## Success Criteria
- [ ] DB/schema rỗng áp `V0_1 → V1 → V2` thành công bằng code test (pha 4 sẽ xác nhận lại bằng Flyway thật).
- [ ] Không còn DDL chép tay trong test.
- [ ] Mọi test PG cũ vẫn pass trên schema dựng từ migrations.
- [ ] README ghi rõ runner, thứ tự và cách baseline cho DB đã áp tay.

## Risk Assessment
- Nếu có môi trường đã áp V1/V2 bằng tay (không có bảng lịch sử Flyway), `flyway migrate` sẽ báo schema không rỗng. Cách xử lý: ghi trong README chạy một lần với `-baselineOnMigrate=true -baselineVersion=2`.
- DeepTutor ghi status chữ thường trong khi V5 ghi chữ hoa. Chọn theo DeepTutor vì đây là dữ liệu thật store ghi xuống; ghi chú lệch doc trong README.
- `interaction_id` kiểu UUID theo V5, trong khi định dạng `question_id` của DeepTutor chưa kiểm chứng. Phase 1 không insert interaction (chỉ đọc), rủi ro dời sang lúc làm Tutor Chat; ghi chú trong README.

## Security Considerations
- Migration không chứa secret. Flyway nhận thông tin kết nối qua env ở pha 4.
