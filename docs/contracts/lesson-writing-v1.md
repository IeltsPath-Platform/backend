# Lesson writing API v1

**Status: approved 2026-10-01.** Owner: Learning Service (`/api/learning`). Extends
[`lesson-learning-v1.md`](lesson-learning-v1.md) with Writing essay blocks: Task 2 (plan `260930-0737`) and Task 1 Academic (plan `260930-0812`, added 2026-10-01).

**Implemented 2026-10-01** in `learning-service` (`SubmitLessonEssayUseCase`, `GetWritingSubmissionUseCase`, `EssayGrader`), Task 1 and Task 2 alike.
In `latestSubmission`, `overallBand` and `passed` are `null` unless the submission is `GRADED`, so a withheld grade is
never visible. `failureCode` values: `LLM_UNAVAILABLE`, `INVALID_GRADE`, `INVALID_PROMPT`, `DAILY_LIMIT_REACHED`,
`GRADING_ABANDONED`. A resend of a `FAILED` `requestId` grades the essay text stored with it, not a new text.

Band scores are an **estimate** produced by an LLM against the public IELTS band descriptors. They are not an official
IELTS result.

## Essay blocks inside a lesson

Content marks an `EXERCISE` block that holds exactly one `ESSAY` question with `blockKind = "ESSAY"`
([`learning-content-internal-v1.md`](learning-content-internal-v1.md)). Learning Service applies these rules on top of
`lesson-learning-v1`:

- `GET /lessons/{id}` returns an essay block as
  `{blockId, blockType: "EXERCISE", blockKind: "ESSAY", sortOrder, question: {questionVersionId, stem, task, minWords, passBand, images}, latestSubmission, sampleAnswer?}`.
  `images` is `[{mediaUrl, altText}]` in Content `sortOrder` (empty for Task 2). Clients render it only through `<img src>`, never as inline SVG, because `mediaUrl` may be a `data:image/svg+xml` URI. `chartFacts` is never returned.
  `latestSubmission` is `{id, status, overallBand, passed}` of the learner's newest submission for the block, or `null`.
  `sampleAnswer` (the Content `explanation`) is present only after the learner has passed the block. `answerSpec` is
  never returned. Auto-graded blocks gain `blockKind: "EXERCISE"` and are otherwise unchanged.
- A lesson is complete when every block with `blockKind = "EXERCISE"` has passed. Essay blocks do not count, so a
  learner without points can still finish every lesson and topic. A lesson with no auto-graded block uses
  `POST /lessons/{id}/complete`.
- `POST /lessons/{id}/exercises/{blockId}/submissions` on an essay block → 409 `ESSAY_BLOCK`.
- Lesson gates (`REVIEW_REQUIRED`, `TOPIC_LOCKED`, `LESSON_LOCKED`) apply to essay submissions exactly as to exercises.

## Course practice essays (2026-10-20)

The open attempt returned by `POST /api/learning/lessons/{lessonId}/practice-attempts` and
`GET /api/learning/practice-attempts/{attemptId}` contains essays in `sections[].essays`, alongside the section's
auto-graded questions. Each essay has `{questionVersionId, sortOrder, stem, task, minWords, passBand,
latestSubmission, guide, images}`. `guide` is learner-facing Markdown or null, separate from the grading explanation;
`images` is an ordered array of `{mediaUrl, altText}`, empty for a prompt with no attached image. URLs are validated
by Content and rendered as image sources. The response does not expose `chartFacts`, `answerSpec` or `explanation`.

Course Writing uses Introduction/Overview/Body 1/Body 2 for Task 1 and Introduction/Body 1/Body 2/Conclusion for Task 2.
The frontend joins non-empty parts with paragraph breaks and submits the same `{requestId, essayText}` payload to
`POST /api/learning/practice-attempts/{attemptId}/essays/{questionVersionId}/submissions`. Grading, charging and the
requirement to grade every essay before finishing the set are unchanged. The chart URLs are also frozen in the
submission's existing prompt snapshot. No timer is introduced; dictionary lookup remains available beside the editor.

Content V64 seeds the initial guides and four public Supabase chart references for the ten V63 course Writing tasks.
V66 adds paraphrasing and vocabulary guidance to those ten guides and clarifies Task 1 overview choices. V67 adds a
third W1 Task 1 table prompt and its fifth chart reference. Image upload is performed by the content owner; files and exact URLs are in
[course-writing-charts](../assets/course-writing-charts/README.md).

## `POST /lessons/{lessonId}/essays/{blockId}/submissions`

Request: `{"requestId":"<UUID>","essayText":"..."}`. The essay is graded **within the request** (typically 20–45 s).

Order: validate input → look up `requestId` → load the lesson from Content → check the point balance → gate check and
record `GRADING` → daily grading limit → LLM grading → debit points → record `GRADED` and mastery evidence.

Response 200:

```json
{
  "submissionId":"…","status":"GRADED","task":"TASK_2","wordCount":312,
  "overallBand":6.5,"passed":true,
  "criteria":[
    {"code":"TR","band":6.5,"strengths":["…"],"improvements":["…"]},
    {"code":"CC","band":6.0,"strengths":["…"],"improvements":["…"]},
    {"code":"LR","band":6.5,"strengths":["…"],"improvements":["…"]},
    {"code":"GRA","band":6.5,"strengths":["…"],"improvements":["…"]}
  ],
  "corrections":[{"excerpt":"peoples can save","suggestion":"people can save","category":"GRAMMAR","errorCode":"PLURAL_FORM"}],
  "summary":"…","pointsCharged":3,"sampleAnswer":"…"
}
```

- `criteria`: exactly `TR`, `CC`, `LR`, `GRA` for `TASK_2`, and `TA`, `CC`, `LR`, `GRA` for `TASK_1` (Task Achievement, judged against the question's `chartFacts`); each band is 0–9 in steps of 0.5; `strengths` and `improvements` hold at
  most 3 items of ≤ 300 characters.
- `overallBand` is computed by Learning Service, not taken from the LLM: the mean of the four bands rounded to the
  nearest half band, with .25 rounding up to .5 and .75 up to the next whole band.
- `corrections`: at most 10; `excerpt` is an exact substring of the essay; `category` remains the broad category
  (`GRAMMAR`, `VOCABULARY`, `COHERENCE`, `TASK` or `OTHER`) and the additive `errorCode` identifies a matching active,
  human-approved Content error type. A correction with an unknown, inactive or unapproved `errorCode` is omitted.
- `summary` ≤ 600 characters. `passed = overallBand ≥ passBand`. `sampleAnswer` only when `passed` or the block was
  passed before.

## `GET /writing-submissions/{submissionId}`

Only the owner can read a submission; another user's id → 404.

| `status` | Body |
| --- | --- |
| `GRADED` | Same as the POST response |
| `PAYMENT_PENDING` | `{submissionId, status, code}`; the grade is withheld until payment succeeds |
| `GRADING`, `FAILED` | `{submissionId, status, failureCode}` |

## Revisions and Mentor review

A graded lesson essay can be rewritten and sent again (`POST /writing-submissions/{id}/revisions`, `GET
/writing-submissions/{id}/history`): see [essay-revision-v1](essay-revision-v1.md). A learner with a human-grading credit
can ask a Mentor to review a graded essay (`POST`/`GET /writing-submissions/{id}/human-review`): see
[human-review-v1](human-review-v1.md). Neither changes the evidence rule above.

## Idempotency and payment

- `requestId` identifies one grading. Resending the same `requestId`: `GRADED` → the stored response; `PAYMENT_PENDING`
  → retry the debit only (the LLM is not called again); `FAILED` → grade again on the same submission; `GRADING` younger
  than 120 s → 409 `GRADING_IN_PROGRESS`, older → taken over and graded again.
- 3 points per successful grading (Access Service, `POST /internal/access/points/debit`, idempotency key
  `lesson-writing:{userId}:{requestId}`). Points are charged only after the LLM grade succeeds; there is no refund.
- At most one `GRADING` submission per learner and block.
- Daily limit: at most 10 gradings per learner per day (`Asia/Ho_Chi_Minh`), counted when the LLM is called. Exceeding
  it → 429 `DAILY_LIMIT_REACHED`, no LLM call, no charge.
- Mastery evidence (`source = lesson_writing`) is recorded on every graded submission until the block is first passed,
  and never after. It has one row per knowledge point of the question with `correct = passed`, and one row per rubric
  criterion whose knowledge point is mapped, with `correct = criterion band >= passBand`. Criterion knowledge points
  are not part of the response. The question's event carries the most frequent correction `errorCode`. No review is
  inserted.

## Errors

Body `{detail, code}`, as in `lesson-learning-v1`.

| HTTP | `code` | When |
| --- | --- | --- |
| 403 | `LESSON_LOCKED`, `TOPIC_LOCKED`, `REVIEW_REQUIRED` | Lesson gate |
| 404 | `NOT_FOUND` | Lesson, block or submission not found for this learner |
| 409 | `NOT_ESSAY_BLOCK` | Essay submitted to a block whose `blockKind` is not `ESSAY` |
| 409 | `ESSAY_BLOCK` | Exercise submission sent to an essay block |
| 409 | `REQUEST_CONFLICT` | `requestId` already used for another learner, lesson or block |
| 409 | `GRADING_IN_PROGRESS` | The block already has a recent `GRADING` submission |
| 422 | `ESSAY_EMPTY`, `ESSAY_TOO_SHORT`, `ESSAY_TOO_LONG` | Empty; under 50 words; over 1,000 words or 10,000 characters. Checked before the balance or the LLM |
| 402 | `INSUFFICIENT_POINTS` | Balance below the cost before grading (no LLM call), or debit refused after grading (`PAYMENT_PENDING`, with `submissionId`) |
| 429 | `DAILY_LIMIT_REACHED` | Daily grading limit reached |
| 503 | `GRADING_UNAVAILABLE` | LLM not configured, failed, timed out or returned an invalid result; no charge |
| 503 | `PAYMENT_UNAVAILABLE` | Access Service failed or rejected the token during debit; status `PAYMENT_PENDING`, resend the same `requestId` |
