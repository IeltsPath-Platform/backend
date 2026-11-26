# SpeakingGradingRequested.v1

Assessment publishes this event through its transactional outbox when a placement submission includes Speaking recordings and `assessment.placement.speaking-grading.enabled` is true. Learning owns the queue and job. The event contains no transcript or signed URL; `object_key` is a storage key in the configured shared `speaking-recordings` bucket.

```json
{
  "event_id": "uuid",
  "event_type": "SpeakingGradingRequested.v1",
  "occurred_at": "ISO-8601",
  "source": "assessment-service",
  "data": {
    "attempt_id": "uuid",
    "user_id": "uuid",
    "items": [{
      "attempt_item_id": "uuid", "part_no": 1, "question_no": 1,
      "question": "Question text", "object_key": "speaking/user/uuid.wav"
    }]
  }
}
```

The consumer deduplicates by `event_id` and `attempt_id`. It retries transient delivery failures and sends invalid or exhausted messages to its dead-letter queue. Correlation and trace context travel in AMQP headers; they are persisted on outbox rows, not duplicated in this payload.
