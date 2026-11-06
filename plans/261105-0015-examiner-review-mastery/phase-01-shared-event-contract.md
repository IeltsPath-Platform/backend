---
phase: 1
title: "Shared event contract"
status: pending
priority: P2
dependencies: []
---

# Phase 1: Shared event contract

## Overview
Định nghĩa event `assessment.human-review-completed.v1` trong `shared/common-events` (record + topology) và contract doc, để
Assessment (phase 2) và Learning (phase 5) dùng chung một định nghĩa.

## Requirements
- Exchange `assessment.events`, routing key `assessment.human-review-completed.v1`, AMQP `message-id` = `eventId`.
- Payload (JSON):
  ```json
  {
    "eventId": "…", "eventType": "assessment.human-review-completed.v1", "occurredAt": "2026-11-05T10:00:00Z",
    "learnerId": "…", "reviewSubmissionId": "…", "sourceSubmissionId": "…",
    "criteria": [{"criterion": "coherence", "band": 5.0}],
    "errorCodes": [{"code": "LINKER_MISUSE", "count": 2}]
  }
  ```
  `errorCodes` gộp `errorCodes` của bài chấm và `annotations[].errorCode`; `count` = số lần xuất hiện trong annotations, tối thiểu 1;
  thứ tự theo `count` giảm dần rồi `code`. Mọi mã examiner ghi (không lọc theo review status; Learning tự lọc mã đã duyệt).
- **Không** có essay, comment, feedback, excerpt, suggestion, examinerId.
- **Không** có `source`/`task` <!-- Red Team #3 -->: `source` do Learning gửi đi không phân biệt free-writing với practice
  (`HumanReviewUseCase.java:63`), và Learning tự có `source`, `task` từ bài viết `sourceSubmissionId`.
- Topology: queue `learning.human-review-completed.v1`, retry queue (delay 30 s), DLQ, theo mẫu `GradingCompletedTopology`.

## Architecture
`shared/common-events/src/main/java/com/ieltspath/events/assessment/`: `HumanReviewCompletedV1` (record, `EVENT_TYPE`,
`ROUTING_KEY`) và `HumanReviewCompletedTopology` (khai báo exchange/queue/binding/retry/DLQ). Cả Assessment và Learning khai báo
topology, nên thứ tự deploy không làm mất event.

## Related Code Files
- Create: `shared/common-events/src/main/java/com/ieltspath/events/assessment/HumanReviewCompletedV1.java`
- Create: `shared/common-events/src/main/java/com/ieltspath/events/assessment/HumanReviewCompletedTopology.java`
- Create: `shared/common-events/src/test/java/com/ieltspath/events/assessment/HumanReviewCompletedV1Test.java`
- Create: `docs/contracts/human-review-completed-v1.md`
- Đọc mẫu: `GradingCompletedV1`, `GradingCompletedTopology`, `docs/contracts/grading-completed-v1.md`

## Implementation Steps
1. Baseline: `mvn -q -pl shared/common-events -am test`.
2. Test đỏ: serialize/deserialize round-trip đúng tên trường; `EVENT_TYPE` = routing key; topology khai báo đủ queue, retry, DLQ, binding (theo test topology sẵn có nếu có).
3. Viết record + topology.
4. Viết `human-review-completed-v1.md`: payload, khi nào gửi (cùng transaction, chỉ lần lưu đầu), at-least-once, người nhận (Learning), không có văn bản.
5. Chạy lại module.

## Success Criteria
- [ ] Record + topology có test, module xanh.
- [ ] Contract doc khớp record (tên trường, routing key, queue).

## Risk Assessment
- Đổi tên trường sau khi phase 2/5 dùng → vỡ cả hai. Chốt ở phase này; phase sau không đổi.
