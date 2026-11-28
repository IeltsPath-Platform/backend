# Placement Writing grading v1

Placement essays are graded by Learning's essay grader, the one used for course and free Writing practice, so the
placement report can show the same detail. Grading is free and records no learning evidence: the placement result carries
the band. Assessment keeps its own essay grader as the fallback.

## Flow

1. On submit, Assessment queues an AI job per sent essay (`grading_jobs`, key `gate-essay:<attemptItemId>:ai`) as before.
   When `assessment.placement.writing-grading.enabled` is true (default), it marks those jobs `PROCESSING` and writes
   `PlacementWritingGradingRequested.v1` to `outbox_events` in the same transaction. An essay whose prompt Learning cannot
   use (no stem or task, Task 1 without chart facts) stays `QUEUED` for the local grader.
2. Learning stores the request in `placement_writing_jobs` (queue `learning.placement-writing-grading.requested.v1`,
   retry and DLQ), grades every essay outside the claim transaction, keeps the whole grade, and writes
   `PlacementWritingGradingCompleted.v1` to `placement_speaking_outbox` (shared with the Speaking results).
3. Assessment (queue `assessment.placement-writing-grading.completed.v1`, retry and DLQ) completes the graded jobs with
   the band and comments, in the JSON shape its own grader stores, then completes the placement when nothing is pending.
   A `FAILED` essay goes back to `QUEUED` and the local grader grades it.
4. A job Learning never answers is picked up again by the local grader after `stuck-after` (10 minutes), like any stuck
   AI job. A late answer for a job already completed changes nothing.

Learning retries a run only when no essay was graded (default 2 attempts, 30 seconds apart), so a graded essay is never
graded twice.

## PlacementWritingGradingRequested.v1

Routing key `PlacementWritingGradingRequested.v1` on `assessment.events`.

```json
{
  "event_id": "uuid", "event_type": "PlacementWritingGradingRequested.v1", "occurred_at": "ISO-8601",
  "source": "assessment-service",
  "data": {
    "attempt_id": "uuid", "user_id": "uuid",
    "items": [{
      "attempt_item_id": "uuid", "question_version_id": "uuid", "stem": "Question text", "task": "TASK_2",
      "min_words": 250, "pass_band": 6.0, "chart_facts": null, "knowledge_point_ids": ["uuid"],
      "essay_text": "The learner's essay"
    }]
  }
}
```

`chart_facts` is required for `TASK_1`. The event carries the essay because Learning has no copy of it; it is removed with
the outbox row when the learner's account is deleted.

## PlacementWritingGradingCompleted.v1

Routing key `PlacementWritingGradingCompleted.v1` on `assessment.events`.

```json
{
  "event_id": "uuid", "event_type": "PlacementWritingGradingCompleted.v1", "occurred_at": "ISO-8601",
  "source": "learning-service",
  "data": {
    "attempt_id": "uuid", "user_id": "uuid",
    "items": [{
      "attempt_item_id": "uuid", "status": "GRADED", "band": 6.0, "summary": "Nhận xét tiếng Việt.",
      "criteria": [{"code": "TR", "band": 6.0, "comment": "Điểm mạnh và điểm cần cải thiện."}],
      "focus": ["Ưu tiên cần sửa"], "prompt_version": "WRITING_TASK_2@1+tpl3", "failure_code": null
    }]
  }
}
```

`status` is `GRADED` or `FAILED`; a failed item has no band and names its `failure_code`. Neither event carries the
prompt sent to the model or the chart facts back.

## Learner read

`GET /api/learning/placement-grades/{attemptId}` (role `CUSTOMER`, own attempts only; another learner's attempt answers as
one with nothing graded):

```json
{
  "speaking": {"status": "GRADED", "result": { "...": "the display object of a Speaking practice result" }},
  "writing": [{"attemptItemId": "uuid", "submission": { "...": "a Writing submission response, submissionId = attemptItemId" }}]
}
```

`speaking` is null when no recording was sent; its `result` is null unless graded. `writing` lists graded essays only. The
web report shows these with the same components as practice, and falls back to the comments in the placement result
when the detail is missing.
