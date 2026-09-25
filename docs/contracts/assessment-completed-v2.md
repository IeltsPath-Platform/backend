# AssessmentCompleted.v2

Integration contract between Assessment Service (producer) and AI Learning
Service (consumer). Assessment Service is the formal grading authority; DeepTutor,
inside AI Learning, is the only adaptive engine (mastery, repetition, review queue,
next objective).

## When it is emitted

The event is emitted when a result version is finalized:

```text
POST /api/assessments/attempts                                  capture active learning goal + Content KP snapshot
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
- An attempt started while the learner had no active goal is finalized without an
  event, because it cannot be attributed to a learning path.
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
| `learning_goal_id` | UUID | **Required.** The goal captured when the attempt started. It is never replaced by whichever goal is active later. |
| `attempt_id` | UUID | Regrade lineage: every version of one attempt's result shares it. |
| `result_id` | UUID | The versioned result row (a new id for each version). |
| `result_version` | integer >= 1 | Monotonic per attempt. |
| `assessment_type` | string | `PLACEMENT`, `OFFICIAL_PRACTICE`, `MOCK`, `TOPIC_GATE`, `QUIZ` |
| `status` | string | Always `COMPLETED`. |
| `completed_at` | ISO-8601 instant | |
| `overall_band` | number or null | The grader's band for this version (0.0–9.0, half-band steps); `null` when none was recorded. **Optional and additive:** events published before it existed have no such key, and consumers read a missing key as `null`. AI Learning uses it only for `PLACEMENT` test-out. |
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
- No path yet for `(user_id, learning_goal_id)`: the event is parked in
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

## Placement test-out

For `assessment_type = PLACEMENT`, AI Learning also tests out knowledge points, after recording the evidence and in
the same transaction:

- every point of the path whose effective band (snapshot taken from Content when the path was built) has an upper
  end not above `overall_band`;
- every point that at least one item of this placement maps to, with no item answered wrong or judged `FAIL`.

A tested-out point is a DeepTutor learner mastery override noted `placement:{attempt_id}:v{result_version}`. A higher
result version of the same attempt replaces the earlier test-out. Points already mastered by evidence, and points
with a learner's own override, are left as they are. Other assessment types never test out.

## Results that arrive before the learning path

The consumer cannot create a path: that needs the learner's token to read the
curriculum. A result for a goal whose path does not exist yet is therefore parked,
and the learner's first path request (`POST /paths`, `GET /progress` or `GET /status`)
applies it.

- Parking and path creation both take `pg_advisory_xact_lock` on
  `hashtextextended('{user_id}:{learning_goal_id}', 0)`. Either the row is parked before
  the path commits, and creation applies it, or parking sees the committed path and
  applies the result normally.
- The transaction that creates the path applies every parked result of that
  `(user_id, learning_goal_id)` in `(attempt_id, result_version)` order through the same
  pipeline and ledger as a live event, deletes them, and commits path, curriculum and
  results as one revision. Only the latest version of an attempt remains in effect.
- Parked results of another goal are never applied to the path.
- A parked payload that no longer parses stays parked, is logged, and does not block
  the path. Rows are kept until the path is created; there is no expiry yet.
- If applying a parked result fails, the whole path creation rolls back (no partial
  state) and the request fails, with 503 when the database is unavailable; the next
  request retries it.
