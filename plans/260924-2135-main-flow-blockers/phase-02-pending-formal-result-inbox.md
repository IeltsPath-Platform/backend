---
phase: 2
title: "Pending formal result inbox"
status: pending
priority: P1
dependencies: [1]
effort: "~5h"
---

# Phase 2: Pending formal result inbox

## Overview
Khi `AssessmentCompleted.v2` tới mà learner chưa có path cho goal, consumer lưu event vào bảng chờ trong `ai_learning_db` rồi ACK, thay vì retry rồi đưa vào DLQ. Lần tạo path đầu tiên (API `POST /paths`, `/progress`, `/status`) sẽ áp dụng các kết quả đang chờ **trong cùng transaction tạo path**, qua đúng pipeline DeepTutor hiện có.

## Context
- Hiện tại `FormalAssessmentIngestionService.ingest` gọi `PathService.ensure_path(user, goal)`, hàm này ném `PathNotBootstrapped`. Consumer coi đây là lỗi tạm thời, nên retry 5 lần rồi đưa vào DLQ.
- Consumer không tự tạo path được: cần token của learner để đọc curriculum. Đã quyết định không mint token và không dùng chung secret.
- `PostgresLearningStore` gom các transaction lồng nhau vào chung một transaction theo cùng kết nối (ContextVar). Nhờ vậy việc áp dụng kết quả chờ ghép được vào transaction tạo path.
- Tham chiếu:
  - `services/ai-learning-service/app/application/formal_assessment_ingestion.py`
  - `app/application/path_service.py`
  - `app/persistence/postgres_learning_store.py`
  - `app/messaging/assessment_consumer.py`

## Requirements
- Functional:
  - Chưa có path: event được lưu vào bảng chờ, outcome `pending`, consumer ACK. Không tạo path, không đụng learning state.
  - Tạo path: mọi kết quả chờ của `(user, goal)` được áp dụng theo thứ tự `(attempt_id, result_version)`, xóa khỏi bảng chờ, và commit cùng lúc tạo path (một revision).
  - Redelivery của event đang chờ không tạo thêm dòng (PK `event_id`) và chỉ được áp dụng một lần.
  - Có nhiều version cùng attempt đang chờ: chỉ version mới nhất còn hiệu lực (ledger và supersede hiện có đảm bảo).
  - Event của goal khác không bao giờ bị áp vào path này.
  - Lỗi contract vẫn vào DLQ như cũ.
- Non-functional:
  - Không mất event khi consumer ghi bảng chờ và API tạo path chạy song song.
  - Không có phụ thuộc vòng giữa các class.
  - Không thêm thư viện.

## Architecture
```text
consumer ─► FormalAssessmentIngestionService.ingest(cmd)
              ├─ path có ─────────────► FormalResultApplier.apply_to_path(path_id, cmd)
              └─ PathNotBootstrapped ─► store.park_formal_result(cmd)   [advisory lock (user,goal)]
                                          ├─ path vẫn chưa có ─► INSERT pending, trả "pending" → ACK
                                          └─ path vừa xuất hiện ─► ensure_path lại → apply_to_path

API ─► PathService.ensure_path(user, goal, modules) ─► _create_path
         store.transaction(create=True, user, goal)   [advisory lock (user,goal) trước SELECT]
           ├─ get_or_create + replace_modules_for_path        (DeepTutor, như hiện nay)
           └─ FormalResultApplier.apply_pending(path_id, user, goal)
                 SELECT ... FOR UPDATE các dòng chờ → apply_to_path từng dòng (gom vào transaction đang mở)
                 → DELETE các dòng đã xử lý (applied/duplicate/stale)
         COMMIT một lần: path + modules + kết quả chờ + ledger + projection + events
```
- Tách class để không có phụ thuộc vòng:
  - `FormalResultApplier`: `apply_to_path` và `apply_pending`. Chỉ phụ thuộc store, `ExternalAssessmentLearningService` và scheduler; không cần `PathService`.
  - `FormalAssessmentIngestionService`: `ingest`. Phụ thuộc store, `PathService` và applier.
  - `PathService`: nhận applier tùy chọn, dùng cho `_create_path`.
- Advisory lock: `pg_advisory_xact_lock(hashtextextended('{user_id}:{goal_id}', 0))`, lấy ở cả `park_formal_result` và `transaction(create=True)`. Lock giữ tới hết transaction, nên hai phía chạy tuần tự:
  - Nếu lưu bảng chờ chạy trước: lần tạo path sẽ áp dụng kết quả chờ.
  - Nếu tạo path chạy trước: lưu bảng chờ thấy path đã có, nên áp dụng ngay theo luồng thường.
- Payload lưu nguyên event JSON đã validate (`FormalAssessmentCommand.raw_event`, `compare=False`, `repr=False`). Khi áp dụng thì parse lại bằng `FormalEvidenceAdapter` để giữ contract.

## Related Code Files
- Create: `services/ai-learning-service/migrations/V3__pending_formal_assessment_results.sql`
  - Bảng `pending_formal_assessment_results`: `event_id` PK, `user_id`, `learning_goal_id`, `attempt_id`, `result_version` CHECK > 0, `payload` JSONB, `received_at`.
  - Index `(user_id, learning_goal_id, attempt_id, result_version)`.
- Create: `services/ai-learning-service/app/application/formal_result_applier.py` (tách `apply_to_path` và `_apply` ra khỏi ingestion service; thêm `apply_pending`)
- Modify: `app/application/formal_assessment_ingestion.py` (`ingest` = xác định path hoặc lưu bảng chờ, rồi gọi applier; thêm outcome `pending`)
- Modify: `app/adapters/formal_evidence_adapter.py` (`FormalAssessmentCommand.raw_event`)
- Modify: `app/application/path_service.py` (nhận applier; `_create_path` gọi `apply_pending` trong transaction)
- Modify: `app/persistence/postgres_learning_store.py` (advisory lock khi `create=True`; `park_formal_result`; `pending_formal_results` FOR UPDATE; `delete_pending_formal_results`)
- Modify: `app/messaging/assessment_consumer.py` (log outcome `pending`; hàm dựng consumer)
- Modify: `main.py` (`get_path_service` nối applier để API tạo path cũng áp dụng kết quả chờ)
- Modify: `tests/formal_assessment_support.py` (`InMemoryLearningStore` có các method mới; lock của store thay cho advisory lock)
- Modify: `tests/test_formal_assessment_ingestion.py`, `tests/test_assessment_consumer.py`, `tests/test_formal_assessment_postgres.py`, `tests/test_assessment_rabbitmq.py`
- Modify: `docs/contracts/assessment-completed-v2.md` và `services/ai-learning-service/README.md` (quy tắc xử lý khi chưa có path)

## Implementation Steps
### Tests Before (khóa hành vi đang giữ)
1. Chạy suite Python (PG) và ghi mốc. Liệt kê các test sẽ **đổi kỳ vọng có chủ đích**:
   - `test_event_for_a_goal_without_a_path_is_not_applied_to_another_path` đang mong `PathNotBootstrapped`, sẽ đổi thành outcome `pending`.
   - `test_transient_failure_is_redelivered_through_the_retry_queue` (RabbitMQ) đang dùng "chưa có path" làm lỗi tạm thời; phải đổi sang lỗi tạm thời khác (ví dụ patch applier ném `OperationalError` lần đầu).
2. Viết test mới cho hành vi mới; chúng phải **fail** trước khi refactor:
   - In-memory:
     - Event trước khi có path: outcome `pending`, có 1 dòng chờ, không có path, consumer ACK.
     - Redelivery khi đang chờ: vẫn 1 dòng.
     - `ensure_path(modules)`: path có attempt từ kết quả chờ, bảng chờ rỗng, chỉ tăng đúng 1 revision.
     - Hai version cùng attempt đang chờ: chỉ version 2 còn hiệu lực (`compute_mastery` của v2).
     - Dòng chờ của goal khác không bị áp.
     - Dòng chờ có payload không parse được: vẫn nằm lại, không chặn việc tạo path, có log.
   - PostgreSQL:
     - Race: `park` và `create` song song (barrier, lặp N lần) → mỗi vòng áp dụng đúng 1 lần, không dòng chờ nào bị kẹt.
     - E2E: consumer lưu bảng chờ → `PathService.active_status` với stub User và stub Content (trả curriculum đúng hình Content DTO) → objective phản ánh kết quả chờ ngay lần gọi đầu.
### Refactor
3. Thêm migration V3.
4. Store:
   - Lấy advisory lock trong `transaction(create=True, …)` khi có `learning_goal_id`, trước `SELECT … FOR UPDATE`.
   - `park_formal_result` (transaction riêng: lock → kiểm tra path → `INSERT … ON CONFLICT (event_id) DO NOTHING`).
   - `pending_formal_results` và `delete_pending_formal_results` chạy trên kết nối của transaction đang mở.
5. Tách `FormalResultApplier` (chuyển nguyên logic hiện có, không đổi hành vi) và thêm `apply_pending`.
6. `FormalAssessmentIngestionService.ingest`: bắt `PathNotBootstrapped` → `park` → trả `pending`, hoặc chạy lại luồng thường nếu path vừa xuất hiện.
7. `PathService._create_path`: sau `replace_modules_for_path`, gọi `applier.apply_pending(...)` trong cùng `store.transaction`.
8. Nối lại `main.py` và `build_consumer`; cập nhật contract doc và README.
### Tests After
9. Toàn bộ test mới và cũ pass; hai test đổi kỳ vọng đã được cập nhật và ghi rõ lý do trong docstring.
### Regression Gate
10. Suite Python (PG): 0 fail. Có `AI_LEARNING_TEST_AMQP_URL` thì chạy cả test RabbitMQ (pha 4 bắt buộc).
11. `compileall`, `git diff --check`, `graphify update .`

## Success Criteria
- [ ] Event tới trước khi có path không còn rơi vào DLQ; nó được ACK và nằm trong bảng chờ.
- [ ] Lần `/status` đầu tiên tạo path và trả objective đã phản ánh kết quả chờ.
- [ ] Race test PG: 0 lần áp dụng đôi, 0 dòng chờ bị kẹt.
- [ ] Revision chỉ tăng 1 cho transaction tạo path có kèm kết quả chờ.
- [ ] Contract doc và README mô tả đúng hành vi mới.

## Risk Assessment
- **Race giữa lưu bảng chờ và tạo path**: xử lý bằng advisory lock theo transaction ở cả hai phía; có test PG lặp để bắt lỗi.
- **Tạo path chậm hơn** khi có nhiều kết quả chờ: chấp nhận ở Phase 1 (mỗi learner chỉ có ít bài). Đo trong test E2E.
- **Bảng chờ phình** với goal không bao giờ được mở. Đã chốt: giữ kết quả đến khi path được tạo, không dọn trong đợt này; ghi rủi ro trong README.
- **Payload chờ không parse được** sau khi contract đổi: dòng giữ lại, có log, không chặn tạo path.
- Lỗi DB khi áp dụng kết quả chờ làm rollback cả việc tạo path; API trả 503, learner gọi lại. Không có trạng thái dở dang.

## Security Considerations
- Chỉ áp dụng kết quả chờ khớp đúng cặp `(user_id, learning_goal_id)` của path đang tạo; `user_id` của path lấy từ JWT đã xác thực.
- Không mint token và không dùng chung secret.
