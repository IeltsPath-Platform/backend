---
phase: 7
title: "AI Learning: consumer kết quả đề"
status: pending
priority: P1
dependencies: [4, 5, 6]
effort: "1.5 ngày"
---

# Phase 7: AI Learning: consumer kết quả đề

## Overview

Sau khi áp bằng chứng của `AssessmentCompleted.v2`, consumer làm tiếp phần bài học, **trong cùng path transaction và trước bản ghi idempotency**:
- **TOPIC_GATE:** dùng lần giao mã đề còn mở để tìm topic; đạt thì ghi `passed_at` (topic kế tự thành "đang học" vì trạng thái suy ra khi đọc, phase 6); rồi đánh giá lại path.
- **MOCK, OFFICIAL_PRACTICE, QUIZ:** chỉ đánh giá lại path.

Consumer không gọi HTTP. Kết quả đỗ lại cũng đi qua đúng đường này khi được áp dụng.

## Requirements

- **Ranh giới idempotency:**
  - `LessonResultApplier` được inject vào `FormalResultApplier` và chạy **trong** `store.transaction(...)` của `apply_to_path`, trên connection đang mở (`_active_connection`, `postgres_learning_store.py:270`), **trước** `record_applied_result` (`formal_result_applier.py:77-115`, `postgres_learning_store.py:287`).
  - Lỗi ở phần bài học thì rollback cả bằng chứng lẫn version, event được giao lại và chạy lại đủ.
  - Đường áp kết quả đỗ lại (`path_service` → `apply_pending` → `FormalResultApplier.apply_to_path`, `path_service.py:53,258`) dùng **cùng** applier đã inject. `PathService` của API và của consumer đều được dựng kèm applier này (`app/api/dependencies.py:20-27`, `assessment_consumer.py:116-122`).
  - **Một applier duy nhất:** `FormalAssessmentIngestionService` tự tạo `FormalResultApplier` riêng khi không được truyền (`formal_assessment_ingestion.py:39`). Consumer phải truyền `applier=` (cùng instance với `PathService`) khi dựng ingestion; test kiểm hai chỗ dùng chung một instance.
- **TOPIC_GATE:**
  - Tìm `topic_test_assignments` theo (`user_id`, `package_version_id`), với `consumed_at IS NULL` và `assigned_at ≤ completed_at` của event.
  - Không có, hoặc thiếu `package_version_id`: ghi log (id, loại event), vẫn giữ bằng chứng, bỏ qua bước topic, **không** `ContractError`.
  - Có: đánh dấu `consumed_attempt_id`, `consumed_at`, `percent` (Σ`score` / Σ`max_score` của `item_results`).
  - ≥ 70%: `UPDATE topic_progress SET passed_at = … WHERE passed_at IS NULL` (**một chiều**: đã PASSED thì không bao giờ gỡ, kể cả khi chấm lại ra điểm thấp hơn). Không có bước mở topic kế: trạng thái suy ra khi đọc (phase 6).
  - < 70%: không ghi gì vào `topic_progress`.
<!-- Updated: Validation Session 1 - chỉ ghi passed_at; một applier cho ingestion và PathService; sửa số dòng record_applied_result -->

  - Rồi `reevaluate_reviews(considered = KP của các item, wrong = KP có item sai)`.
- **MOCK, OFFICIAL_PRACTICE, QUIZ:** `reevaluate_reviews` như trên, không có bước topic.
- **PLACEMENT:** đã xử lý ở phase 5 (không bằng chứng, không test-out).
- **Chấm lại** (result v2 của cùng attempt): bằng chứng theo cơ chế supersede có sẵn; PASSED không bị gỡ; lần giao đã dùng giữ nguyên; bài ôn đã chèn giữ nguyên.
- Không log nội dung câu trả lời (AGENTS §5).

## Architecture

- `app/lessons/result_applier.py`: `LessonResultApplier.apply(conn, user_id, command)`, dùng `review_rule` và `store` của phase 6.
- `app/application/formal_result_applier.py`: nhận `lesson_applier` (tham số chỉ truyền theo tên) và gọi nó trong transaction.
- `app/application/formal_assessment_ingestion.py`: không có logic bài học; mọi việc nằm trong applier.
- `app/messaging/assessment_consumer.py`, `app/api/dependencies.py`: dựng applier; không có client HTTP.

## Related Code Files

- Create: `app/lessons/result_applier.py`
- Modify: `app/application/formal_result_applier.py`, `app/application/path_service.py` (dựng kèm applier cho luồng pending), `app/messaging/assessment_consumer.py:116-122` (truyền `applier=` vào `FormalAssessmentIngestionService`), `app/api/dependencies.py:20-27`
- Tests: `tests/test_formal_assessment_ingestion.py`, `tests/test_formal_assessment_postgres.py`, `tests/test_assessment_consumer.py`, `tests/test_assessment_rabbitmq.py`, `tests/formal_assessment_support.py` (builder event thêm `package_version_id`, `completed_at`); tạo mới `tests/test_lesson_result_applier_postgres.py`

## Implementation Steps

**Tests Before:**
1. Khóa hành vi đang có: event hợp lệ → bằng chứng đúng, version ghi một lần; lỗi contract → DLQ; lỗi tạm → retry queue; quá số lần → DLQ.

**Tests After** (viết trước code):
2. TOPIC_GATE 3/4 với lần giao còn mở → `passed_at` được ghi, `GET /topics` trả topic đó PASSED và topic kế IN_PROGRESS, lần giao đã dùng, chèn bài ôn L3 cho KP2 (seed kịch bản Lan).
3. 2/4 → `passed_at` vẫn NULL (topic vẫn IN_PROGRESS), lần giao đã dùng, bài ôn chèn nếu đủ luật.
4. Lần giao đã dùng, hoặc event trước `assigned_at` (làm lại mã cũ trực tiếp qua assessment) → bằng chứng ghi, topic không đổi.
5. **Lỗi giữa chừng:** cài lỗi trong `LessonResultApplier` → event nack. Giao lại → PASSED đúng, bằng chứng không nhân đôi.
6. Giao lại cùng event sau khi đã thành công → không đổi gì.
7. MOCK sai câu KP1 (bài dạy L2 đã xong) → chèn L2; KP có bài dạy chưa học → không chèn; KP không có gói luyện (`has_practice_set = false`, như KP5) → không chèn. QUIZ xử lý như MOCK.
8. Chưa có path → đỗ lại; tạo path qua API → áp dụng, PASSED đúng (đường pending dùng applier).
9. Chấm lại v2 thấp hơn → PASSED giữ nguyên, bằng chứng v1 bị supersede.
10. Consumer dựng không có client HTTP; test RabbitMQ (khi có `AI_LEARNING_TEST_AMQP_URL`) đi trọn luồng.

**Implement:** result_applier → inject vào `FormalResultApplier` → dựng ở dependencies và consumer.

**Regression Gate:**
```powershell
$env:PYTHONDONTWRITEBYTECODE = "1"
python -m pytest tests -p no:cacheprovider
```
Chạy kèm `AI_LEARNING_TEST_DATABASE_URL` và `AI_LEARNING_TEST_AMQP_URL`.

**E2E thủ công** (sau phase 4 và 7):
1. `docker compose up -d --build rabbitmq ai-learning-db ai-learning-migrate ai-learning-api ai-learning-consumer`.
2. Chạy content và assessment trên host.
3. Qua Gateway: `POST /topics/{id}/test-assignments` → tạo attempt → trả lời → submit. Kiểm topic PASSED và bài ôn trong `ai_learning_db`.
4. Thử replay DLQ theo runbook trong contract.

## Success Criteria

- [ ] Test bước 1–10 pass, gồm test lỗi giữa chừng rồi giao lại.
- [ ] Phần bài học và bằng chứng commit cùng nhau, hoặc không commit gì.
- [ ] Không log nội dung câu trả lời.

## Risk Assessment

- **Làm lại mã cũ trực tiếp qua assessment:** được chấm và ghi bằng chứng, nhưng không mở topic (bước 4). Chấp nhận.
- **Event trễ** (outbox relay thử lại): `assigned_at ≤ completed_at` vẫn đúng, vì `completed_at` là thời điểm chấm chứ không phải thời điểm event tới.
