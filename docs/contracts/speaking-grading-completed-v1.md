# SpeakingGradingCompleted.v1

Learning publishes one result through its transactional outbox for each accepted placement request. Assessment deduplicates by `event_id` and attempt. `status` is `GRADED` or `FAILED`; failed results have no band and are treated as partial under placement rule R3. Feedback contains learner-facing Vietnamese text only.

```json
{
  "event_id": "uuid",
  "event_type": "SpeakingGradingCompleted.v1",
  "occurred_at": "ISO-8601",
  "source": "learning-service",
  "data": {
    "attempt_id": "uuid", "user_id": "uuid", "status": "GRADED",
    "band": 6.5,
    "criteria": [{"code": "FC", "band": 7.0, "comment": "Nhận xét tiếng Việt."}],
    "summary": "Nhận xét tổng quan tiếng Việt.",
    "rubric_code": "SPEAKING", "rubric_version": 1, "failure_code": null
  }
}
```

The `band` is the mean of the four criteria, rounded to the nearest half band. Learning also keeps the whole graded result (transcripts, pronunciation, errors) in `placement_speaking_jobs.result` and serves it to the learner with `GET /api/learning/placement-grades/{attemptId}` ([placement-writing-grading-v1](placement-writing-grading-v1.md#learner-read)); it never travels in this event.

Correlation and trace context are carried in AMQP headers and stored with the outbox row. Neither event may contain transcript, recording bytes, or signed URL. See also [SpeakingGradingRequested.v1](speaking-grading-requested-v1.md).
