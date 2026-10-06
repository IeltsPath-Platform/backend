# AssessmentCompleted.v2

Integration contract between Assessment Service (producer) and the Java Learning
Service (consumer). Assessment Service owns formal grading; Learning Service stores
per-user mastery evidence, reviews and topic progress. **The nullable-goal and
package-version changes were approved 2026-10-01; the consumer and producer updates
are deployed in that order. Both are implemented: Learning Service consumes the event,
and Assessment emits `package_version_id` with `learning_goal_id: null`.**

## When it is emitted

The event is emitted when a result version is finalized:

```text
POST /api/assessments/attempts                                  capture Content package, item and KP snapshots; no goal lookup
POST /api/assessments/attempts/{id}/submit                       objective items: auto-grade -> COMPLETED + outbox in one transaction
POST /api/assessments/grading/attempts/{attemptId}/results      EXAMINER/ADMIN opens a DRAFT version
PUT  /api/assessments/grading/results/{resultId}/details        item results, max scores, judgments, band (still DRAFT)
POST /api/assessments/grading/results/{resultId}/finalize       FinalizeAssessmentResultUseCase: DRAFT/PROCESSING -> COMPLETED
                                                                + outbox_events row, same DB transaction
OutboxRelay                                                     committed row -> RabbitMQ (publisher confirm + mandatory routing)
```

- A result is never announced partially graded. Finalization requires every attempt
  item to have an item result with `score` and `max_score`.
- A COMPLETED version is immutable. A regrade opens the next `result_version` for the
  same attempt, and that version is finalized and announced on its own.
- Every completed result version emits an event, including an attempt without an active
  goal. The new producer always sends `learning_goal_id: null`; it does not query User Service.
- `uq_outbox_assessment_completed_result` allows at most one `AssessmentCompleted.v2`
  row per result version.

## Envelope

| Field | Type | Notes |
| --- | --- | --- |
| `event_id` | UUID | Outbox row id. A republish keeps the same id; it is also the AMQP `message_id`. |
| `event_type` | string | Always `AssessmentCompleted.v2`. Consumers reject any other value. |
| `occurred_at` | ISO-8601 instant | Finalization time. |
| `source` | string | `assessment-service` |
| `data` | object | See below. |

## `data`

| Field | Type | Notes |
| --- | --- | --- |
| `user_id` | UUID | Learner. |
| `learning_goal_id` | UUID or null; key may be absent on older events | The new producer always sends `null`. The consumer accepts `null` or a missing key and scopes processing by `user_id`; a legacy non-null value is ignored for learner selection. |
| `package_version_id` | UUID; key may be absent on older events | The new producer always sends the Content package version used to create the attempt. The consumer tolerates a missing key and skips package-specific topic handling; it does **not** DLQ solely for absence. |
| `attempt_id` | UUID | Regrade lineage: every version of one attempt's result shares it. |
| `result_id` | UUID | The versioned result row (a new id for each version). |
| `result_version` | integer >= 1 | Monotonic per attempt. |
| `assessment_type` | string | `PLACEMENT`, `OFFICIAL_PRACTICE`, `MOCK`, `TOPIC_GATE`, `QUIZ`, `COURSE_GATE` |
| `status` | string | Always `COMPLETED`. |
| `completed_at` | ISO-8601 instant | |
| `overall_band` | number or null | The grader's band for this version (0.0–9.0, half-band steps); `null` when none was recorded. Optional on older events. Placement no longer writes mastery or tests out KPs; the field remains for compatibility. |
| `item_results[]` | array | One entry per attempt item. |

Each `item_results[]` entry:

| Field | Type | Notes |
| --- | --- | --- |
| `item_result_id` | UUID | |
| `question_version_id` | UUID | Content question version from the attempt snapshot. |
| `is_correct` | boolean or null | Explicit correctness. `null` means the item was not graded right/wrong (for example an essay). |
| `score`, `max_score` | number | `0 <= score <= max_score`, `max_score > 0`. |
| `knowledge_point_mappings[]` | array | The Content snapshot taken at attempt start. |

Each `knowledge_point_mappings[]` entry:

| Field | Type | Notes |
| --- | --- | --- |
| `knowledge_point_id` | UUID | Content `knowledge_points.id`. `KP.code` is never identity. |
| `weight` | number >= 0 | Attribution metadata only. It does not scale mastery, evidence quality or scheduling, and every mapped KP receives its own evidence. |
| `qualitative_judgment` | `PASS`, `FAIL`, `NOT_ASSESSED` or null | Explicit per-KP grader outcome. |
| `error_type` | string or null | Assessment error-analysis metadata. Legacy categories (`structural`, `deviation`, `application`, `metacognitive`) are retained for compatibility. |

The event carries no correct answers, expected answers or examiner reasoning.

Example for seed code `X1`, with Q15 answered wrongly. The seed assigns codes but not UUIDs, so UUIDs and timestamps below illustrate the envelope and relationships:

```json
{
  "event_id":"20000000-0000-4000-8000-000000000991","event_type":"AssessmentCompleted.v2","occurred_at":"2026-10-01T09:10:00Z","source":"assessment-service",
  "data":{"user_id":"20000000-0000-4000-8000-000000000981","learning_goal_id":null,"package_version_id":"20000000-0000-4000-8000-000000000401","attempt_id":"20000000-0000-4000-8000-000000000971","result_id":"20000000-0000-4000-8000-000000000975","result_version":1,"assessment_type":"TOPIC_GATE","status":"COMPLETED","completed_at":"2026-10-01T09:10:00Z","overall_band":null,
    "item_results":[{"item_result_id":"20000000-0000-4000-8000-000000000998","question_version_id":"20000000-0000-4000-8000-000000000015","is_correct":false,"score":0,"max_score":1,"knowledge_point_mappings":[{"knowledge_point_id":"20000000-0000-4000-8000-000000000006","weight":1.0,"qualitative_judgment":null,"error_type":null}]}]}
}
```

The example shows Q15 only; the actual X1 result contains Q2, Q14, Q15 and Q16. For `TOPIC_GATE`, only the **first completed submission** for `(user_id, package_version_id)` after the matching assignment's `assigned_at` consumes that assignment and may open the topic. Other attempts of the same version for that assignment do not open the topic, but their results still contribute mastery evidence. When all package codes have been used, the learner is assigned the least recently used code again; its new `assigned_at` starts a new assignment window, and prior knowledge of the answers is an accepted MVP limit. No `assignmentId` is added to the Assessment start request. Item evidence and review evaluation use the Content KP snapshot.

## RabbitMQ topology

| Object | Name | Owner |
| --- | --- | --- |
| Topic exchange (durable) | `assessment.events` | Assessment Service |
| Routing key | `assessment.completed.v2` | Assessment Service |
| Main queue | `learning.assessment-completed.v2` (dead-letters to the retry exchange) | Learning Service |
| Retry exchange and queue | `learning.assessment-completed.retry` / `learning.assessment-completed.v2.retry` (TTL `LEARNING_RETRY_DELAY_MS`, default 30 s, then back to the main queue) | Learning Service |
| Dead-letter exchange and queue | `learning.assessment-completed.dlx` / `learning.assessment-completed.v2.dlq` | Learning Service |

Consumer delivery rules:

- ACK only after the user's PostgreSQL transaction commits, including evidence,
  topic/review changes and the processed result version.
- Evidence can be applied before the learner first calls the API. There is no
  pending-results table or dependency on creating learner aggregate state.
- Transient failure: NACK without requeue, so the message waits in the retry queue.
- After `LEARNING_MAX_DELIVERY_ATTEMPTS` failures (default 5), or on a contract
  violation, publish to the DLQ with an `x-learning-failure` header and ACK the original.
  If the DLQ publish fails, retry the original instead of dropping it.
- The consumer makes no HTTP calls. Review eligibility comes from
  `knowledge_point_catalog.has_practice_set` and completed `lesson_progress`.

## Consumer idempotency and regrades

- Processing identity is `(user_id, attempt_id, result_version)`.
  `assessment_result_versions` has primary key `(user_id, attempt_id)` and stores the
  latest applied version. Read and advance it in the same transaction as all other
  writes, serialized by a transaction-scoped advisory lock for the user.
  - Same version: duplicate, no effect.
  - Lower version: stale, ignored.
  - Higher version: accepted. Remove only the earlier `kp_evidence` rows with
    `source=assessment` for that user and attempt, then apply the new version.
    Lesson and review evidence stays intact. Mastery uses the remaining evidence
    in insertion order (`kp_evidence.ordinal`).
- Evidence identity remains
  `source_reference_id = UUIDv5(6f0c1b2e-3d7a-4e59-9b8a-2c4d5e6f7a81, "{result_id}:{result_version}:{item_result_id}:{knowledge_point_id}")`,
  using canonical lower-case UUIDs and a decimal version.
  `UNIQUE (user_id, source, source_reference_id)` on `kp_evidence` prevents duplicated outcomes.
- The consumer applies the `TOPIC_GATE` assignment rule above by learner and package
  version. Later completed attempts of that version still contribute evidence and
  review evaluation. Topic `PASSED` is one-way; a regrade does not revoke it or
  consume another assignment.
- `PLACEMENT` records only the processed result version. It writes no mastery evidence.

## Rollout and DLQ replay

1. Deploy Learning Service's consumer accepting `learning_goal_id` as null or missing,
   `package_version_id` as optional, and `COURSE_GATE` before Assessment emits the new
   type. The consumer declares the main, retry and dead-letter topology above. The
   course-test flow uses `COURSE_GATE` for `COURSE_TEST` packages; restart Learning
   before Assessment, then replay any messages already sent to the DLQ.
2. While only the service skeleton exists, no new Learning Service queue is declared.
   The development rollout accepts this gap until the consumer is implemented.
3. Pause the Assessment outbox relay or producer during recovery. Inspect queue counts
   and `x-learning-failure` without logging payloads or tokens; confirm the consumer
   and schema are healthy before resuming publication.
4. Preserve each DLQ message's original body, `event_id`, AMQP `message_id`,
   `event_type` and `result_version`. Republish to `assessment.events` with routing key
   `assessment.completed.v2` and publisher confirmation. ACK the DLQ copy only after
   confirmed routing. Do not invent a learning goal or rewrite the event.
5. Verify queue counts settle and `assessment_result_versions` advances.
   Result-version and evidence idempotency make redelivery safe; keep the DLQ copy
   when publication confirmation fails.
