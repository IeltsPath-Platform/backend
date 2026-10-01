---
phase: 4
title: "Consumer AssessmentCompleted.v2"
status: pending
priority: P1
dependencies: [3]
effort: "1.5 ngày"
---

# Phase 4: Consumer AssessmentCompleted.v2

## Overview

Spring AMQP listener cho `assessment.completed.v2` (contract đã sửa ở phase 1). Ghi bằng chứng, xử lý chấm lại, TOPIC_GATE
và chèn bài ôn trong **một** transaction, rồi mới ACK. Luật TOPIC_GATE và chèn bài ôn theo `260929-1640/phase-07`.

## Requirements

- **Topology** (khai báo bằng `Declarables` trong `infrastructure/messaging`): queue chính dead-letter sang retry exchange; retry
  queue TTL `learning.retry-delay-ms` (30 s) rồi về queue chính; DLQ. Lỗi tạm → reject không requeue (sang retry); quá
  `learning.max-delivery-attempts` (5, đếm bằng `x-death`) hoặc vi phạm contract → publish DLQ kèm header `x-learning-failure`
  rồi ACK. Tham khảo bản Python ở git `90fd390:services/ai-learning-service/app/messaging/`.
- **Parse** (port `formal_evidence_adapter.py` ở `90fd390`): `event_type` phải là `AssessmentCompleted.v2`, `status = COMPLETED`;
  `learning_goal_id`, `package_version_id`, `overall_band` optional; UUID sai, `score > max_score`, `max_score ≤ 0`, judgment lạ →
  vi phạm contract.
- **Idempotency và chấm lại** trong transaction có `pg_advisory_xact_lock` theo user:
  - đọc `assessment_result_versions(user_id, attempt_id)`: cùng hoặc thấp hơn → bỏ qua, ACK;
  - cao hơn: xóa `kp_evidence` của `attempt_id` (source `assessment`), rồi ghi version mới.
- **PLACEMENT:** chỉ ghi version.
- **Bằng chứng** mỗi KP mapping: `correct = is_correct` nếu khác null; null thì `qualitative_judgment` PASS/FAIL; còn lại bỏ qua.
  `source_reference_id` theo công thức UUIDv5 trong contract.
- **TOPIC_GATE:** tìm `topic_test_assignments` theo `(user_id, package_version_id)`, `consumed_at IS NULL`,
  `assigned_at ≤ completed_at`; có thì đánh dấu đã dùng + `percent`; ≥ 70% → `passed_at` một chiều. Không có hoặc thiếu
  `package_version_id` → log id, bỏ bước topic.
- **TOPIC_GATE, MOCK, OFFICIAL_PRACTICE, QUIZ:** `ReviewRule` với `considered` = KP của các item, `wrong` = KP có item sai.
- Không gọi HTTP; không log nội dung câu trả lời.

## Tests (nhẹ)

- Unit: parse event (goal null, thiếu `package_version_id`, vi phạm contract).
- Testcontainers (Postgres + RabbitMQ): TOPIC_GATE 3/4 theo kịch bản Lan → PASSED + review L3 cho KP2; giao lại cùng event
  → không đổi; version 2 thay bằng chứng version 1.

## Success Criteria

- [ ] Event không goal được xử lý; bằng chứng và phần bài học commit cùng nhau hoặc không gì cả.
- [ ] Sau khi phase 4 của 1640 merge: E2E qua Gateway giao mã đề → làm đề → topic PASSED.
