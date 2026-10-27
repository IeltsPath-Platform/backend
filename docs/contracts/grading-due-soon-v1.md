# Examiner reminder contract (v1)

The deadline of a human grading job has two marks, both counted from the moment the learner chose the examiner:

| After | Who is told | What it is for |
|---|---|---|
| **36 hours** | the **examiner** (this contract) | the job will be late in 12 hours: give it priority |
| **48 hours** | the **learner** ([`grading-overdue-v1.md`](grading-overdue-v1.md)) | the job is late: keep waiting, or choose another examiner |

At 48 hours the late examiner is not told again. The learner's two choices are in
[`human-review-v1.md`](human-review-v1.md), "Choosing and replacing the examiner". Assessment tells Notification, which
stores one notification for the examiner and pushes it. Grading is blind, so the reminder carries **no learner**, no essay
text and no band.

## Event

Published by Assessment on exchange `assessment.events`, routing key `assessment.grading-due-soon.v1`, through the same
outbox and relay as the other assessment events (at-least-once, persistent, confirmed and routed before it counts as sent).
The AMQP `message-id` is the `eventId`.

```json
{
  "eventId": "30000000-0000-4000-8000-000000000001",
  "eventType": "assessment.grading-due-soon.v1",
  "occurredAt": "2026-10-23T21:10:00Z",
  "examinerId": "30000000-0000-4000-8000-000000000002",
  "gradingJobId": "30000000-0000-4000-8000-000000000003",
  "skill": "WRITING",
  "deadline": "2026-10-24T09:00:00Z"
}
```

- `gradingJobId` is the job in the examiner's own queue (`GET /api/assessments/grading-jobs`).
- `deadline` is when the job becomes late: the time the learner chose the examiner plus 48 hours.
- `skill` is `WRITING` or `SPEAKING`.

## When it is sent

The same timer as the late notice (every 5 minutes, `assessment.examiner.overdue-notice.poll-interval`) looks for human jobs
that are still `QUEUED` or `PROCESSING`, whose examiner was chosen **at least 36 hours and less than 48 hours** ago and who
were never reminded. QA samples are never included.

- A job is reminded **once**. The mark lives on the job (`grading_jobs.due_soon_notified_at`) and is written in the same
  transaction as the outbox row, so a crash neither loses nor repeats the reminder.
- A job already late is not reminded (the learner is told instead), so jobs that are over 48 hours old when this ships get
  no reminder.
- One examiner grades a whole attempt, so an attempt with several jobs gives **one** reminder.
- When the learner gives the job to another examiner, the mark is cleared: the new examiner is reminded 36 hours after being
  chosen.
- The timer follows `assessment.outbox.relay.enabled`: with the relay off nothing is recorded.

## What Notification does

Queue `notification.grading-due-soon.v1` (retry queue `….retry` with a 30 second delay, dead-letter queue `….dlq`, at most
5 retries) is bound to the routing key above and declared by both services from `GradingDueSoonTopology` in `common-events`,
for the same reason as the late notice. For each event Notification:

1. refuses an event that breaks the contract (a missing id, an event type other than the routing key, a `message-id` that is
   not the `eventId`) or cannot be read: it goes to the dead-letter queue without a retry;
2. stores one notification per `eventId`: `recipient` = `examinerId`, `actor` = `examinerId` (there is no learner to name),
   `eventType` = `assessment.grading-due-soon.v1`, `gradingJobId` = the job;
3. pushes `{ id, actorId, eventType, gradingJobId, createdAt }` to the examiner on `/user/queue/notifications`; a push that
   fails loses nothing, the inbox is the source of truth;
4. treats any other failure as temporary and retries it.

The reminder asks the examiner to act, so the community notification preferences do not switch it off.

## Where the examiner sees it

- The notification bell in the top bar of the admin area (examiners only) lists it and opens the Writing queue. The bell
  uses `GET /api/notifications`, `PUT /api/notifications/{id}/read` and the `/ws/notifications` push, like the learner's.
- The queue marks a job from 36 hours as **Sắp quá hạn**, and the dashboard counts them: `GET
  /api/assessments/examiners/me/summary` has `dueSoon` (open jobs chosen at least 36 and less than 48 hours ago), next to
  `overdue` (48 hours or more).

There is no e-mail or mobile push channel yet.

## Queue infrastructure

| What | Name |
|---|---|
| Queue | `notification.grading-due-soon.v1` |
| Retry queue | `notification.grading-due-soon.v1.retry` |
| Dead-letter queue (DLQ) | `notification.grading-due-soon.v1.dlq` |
| Dead-letter exchange (DLX) | `notification.grading-due-soon.dlx` |
| Exchange and routing key | `assessment.events` / `assessment.grading-due-soon.v1` |

- A message that may pass on a second try waits **30 seconds** on the retry queue and returns to the queue, at most **5** times;
  after that, and for a message that breaks the contract or cannot be read, it goes to the DLQ.
- **Both Assessment and Notification declare this queue** from the same class, `GradingDueSoonTopology` in `common-events`, and both bind
  it to `AssessmentMessaging.EXCHANGE` (not to a configurable name), so the broker sees identical declarations and a notice
  is not lost when Notification has never started. This is the one exception to the rule that a consumer owns its queues
  (`AGENTS.md` §3.5).
- Whoever changes a queue name, the retry delay, the retry count or the payload must change `GradingDueSoonTopology` (or the event record),
  the consumer in Notification and **this contract together**.