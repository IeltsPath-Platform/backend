# Late examiner notice contract (v1)

Assessment tells Notification when the examiner a learner chose has not graded a submission within 48 hours, so the
learner can choose another examiner (see [`human-review-v1.md`](human-review-v1.md), "Choosing and replacing the
examiner"). Notification stores one notification for the learner and pushes it. The notice carries no essay text, no
band and no learner name.

## Event

Published by Assessment on exchange `assessment.events`, routing key `assessment.grading-overdue.v1`, through the same
outbox and relay as `AssessmentCompleted.v2` (at-least-once, persistent, confirmed and routed before it counts as sent).
The AMQP `message-id` is the `eventId`.

```json
{
  "eventId": "30000000-0000-4000-8000-000000000001",
  "eventType": "assessment.grading-overdue.v1",
  "occurredAt": "2026-10-23T09:10:00Z",
  "learnerId": "30000000-0000-4000-8000-000000000002",
  "examinerId": "30000000-0000-4000-8000-000000000003",
  "gradingJobId": "30000000-0000-4000-8000-000000000004",
  "skill": "WRITING",
  "deadline": "2026-10-23T09:00:00Z"
}
```

- `gradingJobId` is the learner's job: the learner finds it, with its state, in `GET /api/assessments/grading-jobs/mine`
  and replaces the examiner with `POST /api/assessments/grading-jobs/{id}/examiner`.
- `deadline` is when the job became late: the time the learner chose the examiner plus 48 hours.
- `skill` is `WRITING` or `SPEAKING`.

## When it is sent

A timer in Assessment (every 5 minutes, `assessment.examiner.overdue-notice.poll-interval`) looks for human jobs that are
still `QUEUED` or `PROCESSING`, whose examiner was chosen at least 48 hours ago and that were never announced. QA samples
are never announced.

- A job is announced **once**. The mark lives on the job (`grading_jobs.overdue_notified_at`) and is written in the same
  transaction as the outbox row, so a crash neither loses nor repeats the notice.
- One examiner grades a whole attempt, so an attempt with several late jobs gives **one** notice (for one of its jobs).
- When the learner gives the job to another examiner, the mark is cleared and the new examiner's 48 hours start again. If
  that examiner is late too, the learner is told again.
- Jobs already late when this ships are announced on the first run.
- The timer follows `assessment.outbox.relay.enabled`: with the relay off nothing is recorded.

## What Notification does

Queue `notification.grading-overdue.v1` (retry queue `….retry` with a 30 second delay, dead-letter queue `….dlq`, at most
5 retries) is bound to the routing key above. **Both services declare it**, from the same helper in `common-events`
(`GradingOverdueTopology`), so the declarations are identical: the broker only accepts an event that reaches a queue, and
the relay gives up on an event after 20 refusals, so a queue that did not exist yet (Assessment started before Notification
ever did) would have lost notices. For each event Notification:

1. refuses an event that breaks the contract (a missing id, an event type other than the routing key, a `message-id` that
   is not the `eventId`) or cannot be read: it goes to the dead-letter queue without a retry;
2. stores one notification per `eventId` (a repeated delivery adds nothing): `recipient` = `learnerId`, `actor` =
   `examinerId`, `eventType` = `assessment.grading-overdue.v1`, `gradingJobId` = the job;
3. pushes `{ id, actorId, eventType, gradingJobId, createdAt }` to the learner on `/user/queue/notifications`. A push that
   fails loses nothing: the inbox is the source of truth.
4. treats any other failure as temporary and retries it.

The notice asks the learner to act, so it is **not** switched off by the community notification preferences, and it is not
listed in `GET /api/notifications/preferences`.

## Reading it

`GET /api/notifications` items gain `gradingJobId` (null for community notifications; `postId` and `commentId` are null for
this one). Mark it read with `PUT /api/notifications/{id}/read` like any other. There is no e-mail or mobile push
channel yet; only the in-app inbox and the push above.
