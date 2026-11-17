# `library.vocabulary-review-due.v1`

Library emits this event on its `library.events` exchange to remind a learner that saved vocabulary is due for review. Notification turns it into an inbox notice (and a push, when enabled). The learner is not told how many words are due: the count can change before they read the notice.

## When it is emitted

`VocabularyReviewDueScheduler` runs once a day at 08:00 in `Asia/Ho_Chi_Minh` (`library.review-due.cron` and `library.review-due.zone`). For each learner it counts notebook words (`source_type = VOCABULARY_SENSE`, `status = ACTIVE`) that were learned before (`review_count > 0` or Leitner box 2 or higher) and are due (`next_review_at` is null or not after now). Brand-new words are not counted. A learner with at least `library.review-due.min-count` (default 5) such words gets one event.

The event goes through Library's transactional outbox (`outbox_events`, V43); `LibraryOutboxRelay` publishes it with a broker confirm and mandatory routing. The day is the calendar day in the configured zone, not the learner's own zone.

## Payload

| Field | Type | Meaning |
|---|---|---|
| `eventId` | UUID | Derived from the learner and `reviewDate` (`nameUUIDFromBytes("vocabulary-review-due:{userId}:{reviewDate}")`). It is also the AMQP `message_id` and the outbox row id. |
| `eventType` | string | `library.vocabulary-review-due.v1`. Consumers reject any other value. |
| `occurredAt` | ISO-8601 instant | When the scan ran. |
| `userId` | UUID | The learner. |
| `reviewDate` | ISO-8601 date | The calendar day the reminder is for. |
| `dueCount` | integer | Learned words due at `occurredAt`, at least 1. Not shown to the learner. |

## Idempotency

The same learner on the same day always has the same `eventId`. Two instances running together, or a restart, insert the same outbox row once (`ON CONFLICT DO NOTHING`), and Notification stores the notice once (`insertIfAbsent` by `eventId`). A broker redelivery is dropped the same way.

## Consumer

Notification uses queue `notification.vocabulary-review-due.v1` with `.retry` and `.dlq`; both services declare the queue from `VocabularyReviewDueTopology`, so it exists whichever starts first. The notice type is `library.vocabulary-review-due.v1` and a learner can switch it off in notification preferences (on by default). The notice has no post or grading job and leads to the vocabulary notebook. Consumers make no synchronous call to Library.
