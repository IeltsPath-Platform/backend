# Examiner "a learner chose you" notice (v1)

The moment a learner chooses an examiner for a job, Assessment tells that examiner, so they know there is something to grade
without opening the queue. It is the first of the three notices of a job: this one at once, the reminder at 36 hours
([`grading-due-soon-v1.md`](grading-due-soon-v1.md)) and the late notice to the learner at 48 hours
([`grading-overdue-v1.md`](grading-overdue-v1.md)). Assessment tells Notification, which stores one notification for the
examiner and pushes it. Grading is blind, so the notice carries **no learner**, no essay text and no band.

## Event

Published by Assessment on exchange `assessment.events`, routing key `assessment.grading-requested.v1`, through the same
outbox and relay as the other assessment events (at-least-once, persistent, confirmed and routed before it counts as sent).
The AMQP `message-id` is the `eventId`.

```json
{
  "eventId": "30000000-0000-4000-8000-000000000001",
  "eventType": "assessment.grading-requested.v1",
  "occurredAt": "2026-10-23T09:00:00Z",
  "examinerId": "30000000-0000-4000-8000-000000000002",
  "gradingJobId": "30000000-0000-4000-8000-000000000003",
  "skill": "WRITING",
  "deadline": "2026-10-25T09:00:00Z"
}
```

- `gradingJobId` is the job in the examiner's own queue (`GET /api/assessments/grading-jobs`).
- `deadline` is when the job becomes late: the time the learner chose the examiner plus 48 hours.
- `skill` is `WRITING` or `SPEAKING`.

## When it is sent

The outbox row is written in the same transaction that hands the job over, so a job handed over is always announced and a job
that was refused or rolled back never is. Right after that transaction commits, the relay runs at once (it does not wait for
its 2 second timer), so the notice leaves for the broker within a moment; the timer stays as the safety net if that run fails
or the process stops in between.

- A **new request** for a standalone review (`POST /internal/assessments/human-reviews`: a Practice or Lesson essay sent to an
  examiner). A repeated request with the same `requestId` finds the job already announced and sends nothing.
- The learner **chooses or replaces** the examiner of a job (`POST /api/assessments/grading-jobs/{id}/examiner`): the **new**
  examiner is told. One examiner grades a whole attempt, so an attempt with several jobs gives **one** notice.
- A request refused because the examiner is full, paused or unknown sends nothing.
- The examiner who was replaced is not told.

## What Notification does

Queue `notification.grading-requested.v1` (retry queue `….retry` with a 30 second delay, dead-letter queue `….dlq`, at most
5 retries) is bound to the routing key above and declared by both services from `GradingRequestedTopology` in `common-events`.
For each event Notification:

1. refuses an event that breaks the contract (a missing id, an event type other than the routing key, a `message-id` that is
   not the `eventId`) or cannot be read: it goes to the dead-letter queue without a retry;
2. stores one notification per `eventId`: `recipient` = `examinerId`, `actor` = `examinerId` (there is no learner to name),
   `eventType` = `assessment.grading-requested.v1`, `gradingJobId` = the job;
3. pushes `{ id, actorId, eventType, gradingJobId, createdAt }` to the examiner on `/user/queue/notifications`; a push that
   fails loses nothing, the inbox is the source of truth;
4. treats any other failure as temporary and retries it.

The notice asks the examiner to act, so the community notification preferences do not switch it off.

## Where the examiner sees it

- The notification bell in the top bar of the admin area gets a pop-up under it the moment the push arrives ("Thông báo mới:
  Có bài mới chờ bạn chấm (hạn 48 giờ)", gone after 8 seconds or when closed), the unread count goes up, and the list keeps
  the notice. Opening it leads to the Writing queue.
- The queue and the dashboard load again **as soon as the push arrives**, when the tab becomes visible again and every minute
  while it is visible (the socket can drop), so there is no reload button. The reload keeps what is on screen until the new
  data arrives.
