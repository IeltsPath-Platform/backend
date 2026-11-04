# Examiner's Writing review applied to mastery (v1)

When an examiner finishes a standalone Writing review (an essay the learner sent to a teacher from a lesson, a Practice set, or
free writing), Assessment tells Learning what the examiner graded. Learning applies it to the learner's mastery the way it applies
the AI's Writing grade, and creates the optional reviews the result calls for. Contract of the grade itself:
[`human-review-v1.md`](human-review-v1.md).

## Event

Published by Assessment on exchange `assessment.events`, routing key `assessment.human-review-completed.v1`, through the same outbox and
relay as the other assessment events (at-least-once). The AMQP `message-id` is the `eventId`.

```json
{
  "eventId": "30000000-0000-4000-8000-000000000001",
  "eventType": "assessment.human-review-completed.v1",
  "occurredAt": "2026-11-05T10:00:00Z",
  "learnerId": "30000000-0000-4000-8000-000000000002",
  "reviewSubmissionId": "30000000-0000-4000-8000-000000000003",
  "sourceSubmissionId": "30000000-0000-4000-8000-000000000004",
  "criteria": [{"criterion": "coherence", "band": 5.0}, {"criterion": "taskResponse", "band": 6.5}],
  "errorCodes": [{"code": "LINKER_MISUSE", "count": 2}, {"code": "TENSE", "count": 1}]
}
```

- `criteria[].criterion` is the examiner's name for the criterion: `taskResponse`, `coherence`, `lexical` or `grammar`. Learning maps it to the
  rubric code (`TA` or `TR` for `taskResponse` by task, `CC`, `LR`, `GRA`).
- `errorCodes` lists each code the examiner recorded (in the list or marked in the essay) with how often it was marked in the essay; at
  least 1, most frequent first. Every recorded code is sent; Learning counts only the approved ones.
- `sourceSubmissionId` is the learner's essay in Learning. Learning reads the essay's task, source and prompt from its own store.
- **No essay text, comment, feedback, excerpt, suggestion, examiner identity or source/task field** is in the event.

## When it is sent

The outbox row is written in the transaction that stores the examiner's review, only by the call that finished it. A repeated completion
of the same review sends nothing. Only Writing reviews are sent (`skill = WRITING`).

## What Learning does

Queue `learning.human-review-completed.v1` (retry after 30 s, at most 5 retries, then the DLQ `learning.human-review-completed.v1.dlq`).
A message that breaks the contract, or whose essay is not the learner's, is dead-lettered without retry. Any other failure is retried.

In one transaction under the learner's lock:

1. **Applied once per essay.** If the essay already has applied study points, nothing changes.
2. **Criteria.** Each criterion band reaching the prompt's pass band is correct for its knowledge point; a lower band is wrong and
   is a failed criterion. Two criteria on one point must both pass.
3. **The examiner's result replaces the AI's.** The AI's criterion evidence for this essay (lesson or practice) is removed, and only that
   source is touched. Evidence of other sources stays.
4. **Evidence (`source = human_review`, weight 0.75 by default, `learning.mastery.mp1.source-weights.human-review`).** One answer per
   criterion point, correct or not as above.
5. **Marked errors (`source = error_trap`, weight 0.3).** Each approved code with a trap point is one wrong answer on that trap point,
   once per review. An unapproved code is neither counted nor shown.
6. **Optional reviews, never blocking, at most `learning.human-review.review-limit` (default 2) in total.** The marked errors come first
   (`ERROR_PATTERN`, on the parent point, when a practice set with at least two matching traps exists); then the weakest failed criteria
   (`WRITING_CRITERION`, lesson essays only). A point with a pending review is skipped.
7. **Study points** are stored for the learner (see `GET …/human-review` in [`human-review-v1.md`](human-review-v1.md)).

Mastery is rebuilt from the evidence of the affected points; no milestone is emitted. A review that was already `DONE` before this event
existed is not applied afterwards (no backfill).

## Replay

A message in the DLQ is put back on its queue with `POST /api/learning/admin/dead-letters/learning.human-review-completed.v1.dlq/replay`
(the same administration endpoint as the other Learning queues; `GET /api/learning/admin/dead-letters` lists the counts). The application
is idempotent per essay, so a replay after a partial failure applies the grade once.

## Errors and logs

Logs carry the `reviewSubmissionId` and the error type only, never the payload.
