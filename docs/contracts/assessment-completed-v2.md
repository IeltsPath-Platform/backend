# AssessmentCompleted.v2

Integration contract between Assessment Service (producer) and AI Learning
Service (consumer). Assessment Service is the formal grading authority; DeepTutor,
inside AI Learning, is the only adaptive engine (mastery, repetition, review queue,
next objective). **The nullable-goal and package-version contract changes were approved
2026-10-01; the current producer and consumer still use the earlier contract until implementation.**

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
| `learning_goal_id` | UUID or null; key may be absent on older events | The new producer always sends `null`. The consumer accepts `null` or a missing key and locates the path by `user_id`; a legacy non-null value is retained as metadata but does not select a path. |
| `package_version_id` | UUID; key may be absent on older events | The new producer always sends the Content package version used to create the attempt. The consumer tolerates a missing key and skips package-specific topic handling; it does **not** DLQ solely for absence. |
| `attempt_id` | UUID | Regrade lineage: every version of one attempt's result shares it. |
| `result_id` | UUID | The versioned result row (a new id for each version). |
| `result_version` | integer >= 1 | Monotonic per attempt. |
| `assessment_type` | string | `PLACEMENT`, `OFFICIAL_PRACTICE`, `MOCK`, `TOPIC_GATE`, `QUIZ` |
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
| `knowledge_point_id` | UUID | Content `knowledge_points.id` = DeepTutor `KnowledgePoint.id`. `KP.code` is never identity. |
| `weight` | number >= 0 | Attribution metadata only. It does not scale mastery, evidence quality or scheduling, and every mapped KP receives its own evidence. |
| `qualitative_judgment` | `PASS`, `FAIL`, `NOT_ASSESSED` or null | Explicit per-KP grader outcome. |
| `error_type` | string or null | Assessment error analysis. Only DeepTutor categories (`structural`, `deviation`, `application`, `metacognitive`) are used. |

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
| Main queue | `ai-learning.assessment-completed.v2` (dead-letters to the retry exchange) | AI Learning |
| Retry exchange and queue | `ai-learning.assessment-completed.retry` / `ai-learning.assessment-completed.v2.retry` (TTL `AI_LEARNING_RETRY_DELAY_MS`, default 30 s, then back to the main queue) | AI Learning |
| Dead-letter exchange and queue | `ai-learning.assessment-completed.dlx` / `ai-learning.assessment-completed.v2.dlq` | AI Learning |

Consumer delivery rules:

- ACK only after the path's PostgreSQL transaction commits.
- No path yet for `user_id`: the event is parked in
  `pending_formal_assessment_results` (keyed by `event_id`, so a redelivery keeps one
  row) and ACKed. It is not retried and never reaches the DLQ. See below.
- Transient failure: NACK without requeue, so the message waits in the retry queue.
- After `AI_LEARNING_MAX_DELIVERY_ATTEMPTS` failures (default 5), or on any contract
  violation, the message is published to the DLQ with an `x-ai-learning-failure`
  header and the original is ACKed. If the DLQ publish fails, the message is retried
  instead of being dropped.

## Consumer idempotency and regrades

- Evidence identity: `source_reference_id = UUIDv5(6f0c1b2e-3d7a-4e59-9b8a-2c4d5e6f7a81,
  "{result_id}:{result_version}:{item_result_id}:{knowledge_point_id}")`, using canonical
  lower-case UUIDs and a decimal version.
- `formal_assessment_result_versions(path_id, attempt_id)` records the applied version.
  It is read and advanced under the `mastery_paths` row lock, and its upsert only
  accepts a higher version.
  - Same version: duplicate, no effect.
  - Lower version: stale, ignored.
  - Higher version: accepted. The attempt's earlier formal outcomes are removed from
    the aggregate and the affected knowledge points are rebuilt with DeepTutor's
    `calculate_mastery` and `scheduler.replay`, then the new version is applied.
- `UNIQUE (path_id, source, source_reference_id)` on `mastery_learning_evidence` is the
  database backstop against a duplicated outcome.
- The consumer applies the `TOPIC_GATE` assignment rule above by learner and package
  version. Later completed attempts of the same version still pass through evidence
  and review processing, while the topic transition remains one-way.

`PLACEMENT` records only the processed result version. It writes no mastery evidence and performs no test-out.

## Results that arrive before the learning path

The consumer cannot create a path: that needs the learner's token to read the
curriculum. A result for a user whose path does not exist yet is parked. Path creation
or loading an existing path applies pending results for the user; learner `GET /topics`
also refreshes the path.

- Parking and path creation both take a transaction-scoped advisory lock keyed by
  `user_id`. Either the row is parked before
  the path commits, and creation applies it, or parking sees the committed path and
  applies the result normally.
- The transaction that creates or loads the path applies every parked result of that
  `user_id` in `(attempt_id, result_version)` order through the same
  pipeline and ledger as a live event, deletes them, and commits path, curriculum and
  results as one revision. Only the latest version of an attempt remains in effect.
- A legacy non-null `learning_goal_id` does not prevent applying a result to the user's
  one path; results belonging to another user are never applied.
- A parked payload that no longer parses stays parked, is logged, and does not block
  the path. Rows are kept until the path is created; there is no expiry yet.
- If applying a parked result fails, the whole path creation rolls back (no partial
  state) and the request fails, with 503 when the database is unavailable; the next
  request retries it.

## Rollout and DLQ replay

1. Deploy AI Learning's consumer/adapter accepting `learning_goal_id` as null or missing and `package_version_id` as optional **before** deploying Assessment's new producer. Also deploy the one-path-per-user pending lookup before allowing goal-less results. An old consumer treats a goal-less event as a contract violation and can send it to `ai-learning.assessment-completed.v2.dlq`.
2. Pause the Assessment outbox relay or producer while recovering from a mixed-version rollout. Inspect the DLQ count and the `x-ai-learning-failure` header without logging payload answers or tokens. Confirm the new consumer and path schema are healthy, then resume normal publication.
3. For each DLQ message caused by the old consumer's nullable-goal rejection, preserve the **original body, `event_id`, AMQP `message_id`, `event_type`, and `result_version`** and republish to `assessment.events` with routing key `assessment.completed.v2` and publisher confirmation. ACK/remove the DLQ copy only after confirmed routing. Do not rewrite `learning_goal_id` or invent a goal. Other contract failures require investigation before replay.
4. Verify the main/retry/DLQ counts settle and `formal_assessment_result_versions` advances or the event is parked in `pending_formal_assessment_results` for its `user_id`. A later path load must drain parked rows. Redelivery is safe under `event_id` parking and result-version idempotency; keep the DLQ copy when publish confirmation fails.
