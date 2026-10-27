# Learner "your essay was graded" notice and list (v1)

When the examiner finishes grading a learner's essay, Assessment tells the learner, who opens a list of the essays they sent
to a teacher and from there the comments and the errors marked in the essay. It is the last of the notices of a job: the
examiner is told at once ([`grading-requested-v1.md`](grading-requested-v1.md)), again at 36 hours
([`grading-due-soon-v1.md`](grading-due-soon-v1.md)), the learner at 48 hours if it is late
([`grading-overdue-v1.md`](grading-overdue-v1.md)), and this one when it is done.

## Event

Published by Assessment on exchange `assessment.events`, routing key `assessment.grading-completed.v1`, through the same
outbox and relay as the other assessment events (at-least-once, persistent, confirmed and routed before it counts as sent).
The AMQP `message-id` is the `eventId`.

```json
{
  "eventId": "30000000-0000-4000-8000-000000000001",
  "eventType": "assessment.grading-completed.v1",
  "occurredAt": "2026-10-26T13:00:00Z",
  "learnerId": "30000000-0000-4000-8000-000000000002",
  "gradingJobId": "30000000-0000-4000-8000-000000000003",
  "reviewSubmissionId": "30000000-0000-4000-8000-000000000004",
  "skill": "WRITING"
}
```

- `gradingJobId` is the learner's job (`GET /api/assessments/grading-jobs/mine`); `reviewSubmissionId` is the review Learning
  holds for the essay.
- The notice carries **no band, comment or essay text**, and does not name the examiner: the learner reads those in the app.

## When it is sent

The outbox row is written in the same transaction that stores the examiner's review
(`POST /api/assessments/grading-jobs/{id}/human-review`), so a review that was stored is always announced and one that was not
never is. Right after the commit the relay runs at once (not at its 2 second timer), so the bell rings as the examiner
finishes; the timer stays as the safety net.

- **Once**: completing again with the same review (a repeated request) stores nothing new and tells nobody again. Completing
  with a different review is refused (409), as before.
- Only a standalone review (an essay sent to a teacher from Practice, Lesson or free writing) is announced. Exam attempts
  (Mock, course tests) reach the learner through their result, not through this notice.

## What Notification does

Queue `notification.grading-completed.v1` (retry queue `….retry` with a 30 second delay, dead-letter queue `….dlq`, at most
5 retries) is bound to the routing key above and declared by both services from `GradingCompletedTopology` in
`common-events`. For each event Notification:

1. refuses an event that breaks the contract (a missing id, an event type other than the routing key, a `message-id` that is
   not the `eventId`) or cannot be read: it goes to the dead-letter queue without a retry;
2. stores one notification per `eventId`: `recipient` = `learnerId`, `actor` = `learnerId` (no examiner is named),
   `eventType` = `assessment.grading-completed.v1`, `gradingJobId` = the job;
3. pushes `{ id, actorId, eventType, gradingJobId, createdAt }` to the learner on `/user/queue/notifications`; a push that
   fails loses nothing, the inbox is the source of truth;
4. treats any other failure as temporary and retries it.

The notice tells the learner something they are waiting for, so the community notification preferences do not switch it off.

## Where the learner sees it

1. The bell shows a pop-up and the unread count the moment the push arrives; its text is "Giáo viên đã chấm xong bài của bạn.
   Xem nhận xét và các lỗi cần sửa". Opening it leads to **`/writing-reviews`**.
2. `/writing-reviews` lists the essays sent to a teacher, graded ones first with the band the teacher gave, then the ones
   still waiting. It is built from two calls:
   - `GET /api/learning/examiner-reviews?page=0&size=20` (Learning, `CUSTOMER`): the learner's essays newest first,
     `{ items: [{ submissionId, reviewSubmissionId, source, task, promptPreview, wordCount, requestedAt }], page, size,
     hasNext }`. `source` is `LESSON_BLOCK`, `PRACTICE_ITEM` or `FREE_WRITING`; `size` is at most 50.
   - `GET /api/assessments/grading-jobs/mine` (Assessment): the state of each job, matched by `reviewSubmissionId`. A job now
     also has `band` and `completedAt` once it is `COMPLETED`.
3. An essay opens at `/practice/writing/submissions/{submissionId}/result`, which reads
   `GET /api/learning/writing-submissions/{submissionId}/human-review` (unchanged): the band, the four criteria with the
   examiner's comment, the overall feedback, the essay with the marked errors and what to write instead.
4. What an error code means: `GET /api/content/error-types/approved` (Content, any signed-in role) lists the **reviewed and
   active** error types as `{ code, name, category, fixHint }`, no ids and no review status: the same set a learner is ever
   shown. The result page shows the name and the fix of each marked error, and a short list of the kinds of error in the
   essay. When the list cannot be read the page shows the codes and everything else as before.

## Queue infrastructure

| What | Name |
|---|---|
| Queue | `notification.grading-completed.v1` |
| Retry queue | `notification.grading-completed.v1.retry` |
| Dead-letter queue (DLQ) | `notification.grading-completed.v1.dlq` |
| Dead-letter exchange (DLX) | `notification.grading-completed.dlx` |
| Exchange and routing key | `assessment.events` / `assessment.grading-completed.v1` |

- A message that may pass on a second try waits **30 seconds** on the retry queue and returns to the queue, at most **5** times;
  after that, and for a message that breaks the contract or cannot be read, it goes to the DLQ.
- **Both Assessment and Notification declare this queue** from the same class, `GradingCompletedTopology` in `common-events`, and both bind
  it to `AssessmentMessaging.EXCHANGE` (not to a configurable name), so the broker sees identical declarations and a notice
  is not lost when Notification has never started. This is the one exception to the rule that a consumer owns its queues
  (`AGENTS.md` §3.5).
- Whoever changes a queue name, the retry delay, the retry count or the payload must change `GradingCompletedTopology` (or the event record),
  the consumer in Notification and **this contract together**.