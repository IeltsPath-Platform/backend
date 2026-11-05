# Independent human review contract (v1)

This is the Learning-to-Assessment contract for Mentor feedback on graded lesson and Practice essays. The standalone
submission is stored separately from assessment attempts and results. It does not create an attempt, an assessment
result, or mastery evidence.

## Learner-facing API (Learning)

Role `CUSTOMER`, own essays only. Both calls forward the learner's bearer token.

`POST /api/learning/writing-submissions/{id}/human-review` — body `{ "examinerId": "…" }` (required: the learner picks
the examiner from `GET /api/users/examiners`, see [`user-internal-v1.md`](user-internal-v1.md)); `202`,
`{ "reviewSubmissionId": "…", "status": "REQUESTED" }`.
The essay (lesson block or Practice) must be `GRADED`. Learning spends one human-grading credit for the essay (Access is
idempotent by essay id), then asks Assessment for the review below. The credit is given back only when this call spent
it (`charged: true` in the Access answer) and Assessment refused the review outright (a 4xx answer: nothing was created,
for example `EXAMINER_UNAVAILABLE`: the learner then asks again with another examiner and the credit is spent again).
A timeout, a server error or a lost connection may have left the review behind, so Learning asks once more with the same
request id and, if that fails too, keeps the credit with the essay and answers `503`; asking again reuses it and spends
nothing more. One request per essay: asking again returns the same review and spends nothing. The request id sent to Assessment
is derived from the essay id, so Assessment creates the review once.

`GET /api/learning/writing-submissions/{id}/human-review` — the review as returned by Assessment (below): `status`
`PENDING`, `DONE` or `CANCELLED` (the learner gave up a late request), grade fields only when `DONE`.

`POST /api/learning/free-writing/examiner-submissions` — sends a free-writing essay (a prompt of a practice test) to an
examiner **instead of the AI grader**. Body `{ "requestId", "packageVersionId", "questionVersionId", "essayText",
"examinerId" }`; `200` with the submission, status `AWAITING_EXAMINER` (no grade, `essayText` included). Nothing is graded
by the AI and no point is charged: the learner needs **Premium with a grading credit left** (Access spends one credit,
`402 HUMAN_GRADING_CREDIT_REQUIRED` otherwise) and the examiner must be able to take it (`409 EXAMINER_UNAVAILABLE`:
full for today, not accepting, or not an examiner; the credit is given back). The essay is checked like an AI-graded one
(`422 ESSAY_EMPTY`, `ESSAY_TOO_SHORT`, `ESSAY_TOO_LONG`, `NOT_A_PRACTICE_TEST`, `404` for a prompt that is not an essay of
the test). `requestId` makes the essay idempotent: sending again with the same id continues the same essay, and after a
refusal the learner sends it again, possibly to another examiner, without a second essay being stored. The learner reads
the result from `GET /api/learning/writing-submissions/{id}` (status) and the examiner's review from
`GET /api/learning/writing-submissions/{id}/human-review` below. **One essay per prompt waits at a time.** A learner who sent a prompt to an examiner, left, and
comes back to write the same prompt again (a new `requestId`) is refused with `409 EXAMINER_REVIEW_PENDING` while that
earlier essay has not been graded: nothing is stored, no credit is spent and the examiner's day is not used. The error
body carries `submissionId`, the essay still waiting, to open its result page. Once the examiner has graded (`DONE`),
the same prompt can be written and sent again, as a new essay that spends a new credit. An essay that never reached an
examiner (refused for credit or availability) does not block. Sending the AI grade for the prompt is a separate path and
is not blocked. `GET /api/learning/free-writing/examiner-submissions/pending?questionVersionId=…` answers `{ "pending":
true, "submissionId": "…" }` or `{ "pending": false }` so the screen can say so before the learner writes; when Assessment
cannot be reached it answers `false` and the send checks again.

The AI path (`POST /api/learning/free-writing/submissions`,
points) is unchanged; the learner picks one or the other, and which one is open is decided by Premium credits (examiner)
or the points balance (AI).

Errors: `404 NOT_FOUND` (not the caller's), `404 NO_HUMAN_REVIEW` (nothing requested), `409 NOT_REVIEWABLE` (neither graded nor sent to an examiner),
`402 HUMAN_GRADING_CREDIT_REQUIRED`, `409 HUMAN_GRADING_CONFLICT`, `409 EXAMINER_UNAVAILABLE` (the chosen examiner is
full, not accepting submissions or not an examiner), `422` (no `examinerId`), `503 HUMAN_REVIEW_UNAVAILABLE`. The review is feedback
only: no evidence, no reviews, no change to Practice or gates. Learning stores only `human_review_requests`
(essay → review id); the essay text is sent to Assessment and never logged.

## Create a review

Learning calls Assessment with the learner's own internal JWT after validating that the essay belongs to the current
learner and is already `GRADED`. Access credit consumption remains a separate call authenticated with the learner bearer token.

### `POST /internal/assessments/human-reviews`

```json
{
  "requestId": "8a8ead2e-91cc-42a0-a981-4f7f475f0b2b",
  "learnerId": "c26f2b10-d777-4888-ad68-6a51def59e01",
  "sourceSubmissionId": "9d272143-3d16-4ed8-ad24-f641eab4f906",
  "source": "PRACTICE",
  "task": "TASK_2",
  "promptSnapshot": {"stem": "Some people believe...", "images": [{"url": "https://cdn.example/chart.png", "altText": "Bar chart"}]},
  "essay": "The essay text submitted by the learner.",
  "rubric": {"code": "IELTS_WRITING_TASK_2", "version": 1, "criteria": []},
  "examinerId": "5b1f0c6e-3d0a-4a43-9c0e-0f1d9a7d2c11"
}
```

`promptSnapshot.images` is optional: the chart images of the prompt (Task 1) the learner saw, each `{url, altText?}`, so the
examiner reads the essay against the same chart. Learning sends it only when the prompt has images; an entry without a `url`
is ignored. Reviews created before this field existed have no images.

`examinerId` is required: the examiner the learner chose. Assessment asks User Service whether that user is an active
examiner who accepts submissions and checks that the examiner has not yet received `dailyLimit` submissions today.
Otherwise nothing is created and the answer is `409` with code `EXAMINER_UNAVAILABLE` and `details.reason`
`NOT_ACCEPTING`, `FULL` or `OVERDUE_BACKLOG` (the examiner has 5 or more open jobs already late, see below);
`503 USER_SERVICE_UNAVAILABLE` when User Service cannot be reached (the request is not accepted without the check). Repeating a `requestId` that already made a review returns it without the check.

The review always belongs to the authenticated learner (the JWT subject). `learnerId` is optional and only checked: a
value that differs from the JWT subject returns `403`.

`source` is `LESSON` or `PRACTICE`; `task` is `TASK_1` or `TASK_2`. `requestId` is the idempotency key. Repeating it
with the same payload returns the same review ID. Reusing it with another payload, or requesting a second review for the
same `sourceSubmissionId`, returns `409`. Assessment creates an independent submission and queues a `HUMAN` job with
reason `HUMAN_CREDIT` for that examiner. The submission appears only in that examiner's queue, with its source and
rubric; it has no attempt or result ID.

```json
{
  "requestId": "8a8ead2e-91cc-42a0-a981-4f7f475f0b2b",
  "reviewSubmissionId": "19d211e9-c99b-4dd7-bf2d-612fc728d5a4",
  "status": "PENDING"
}
```

## Read a review

### `GET /internal/assessments/human-reviews/{reviewSubmissionId}`

```json
{
  "reviewSubmissionId": "19d211e9-c99b-4dd7-bf2d-612fc728d5a4",
  "examinerId": "5b1f0c6e-3d0a-4a43-9c0e-0f1d9a7d2c11",
  "status": "DONE",
  "band": 6.5,
  "criteria": [{"criterion": "coherence", "band": 6.0, "comment": "Progression is clear."}],
  "errorCodes": ["LINKER_MISUSE"],
  "feedback": "Develop the main position further.",
  "annotations": [
    {"excerpt": "However, the", "start": 120, "end": 132, "errorCode": "LINKER_MISUSE", "suggestion": "Use \"In addition\" here."},
    {"excerpt": "have rose", "start": 812, "end": 821, "suggestion": "have risen"}
  ]
}
```

`annotations` is omitted when the examiner marked no error. Each entry quotes the essay: `start` and `end` are UTF-16
offsets (the indexes of a Java or JavaScript string), `end` exclusive, and `excerpt` is the text between them.
`suggestion` is the examiner's own wording.

An error code is shown to the learner only while its entry in the error-type catalogue is `APPROVED`. A code still in
review is left out of `errorCodes` and of each annotation (the second entry above has no `errorCode`); the examiner's
`excerpt` and `suggestion` stay. When Content cannot be reached, no code is shown and the review is still returned.

Only the authenticated learner can read their review; another learner's review answers `404`, as an unknown id does.

Pending response uses `status: "PENDING"` with grade fields omitted; a cancelled one uses `status: "CANCELLED"`, also with
no grade. A `DONE` response contains the examiner's overall
band, criteria, error codes, and feedback. `examinerId` is the examiner the learner chose; the learner reads that
examiner's name, photo, band and certificates from User Service. The examiner does not see the learner (below). The
review is feedback only: it does not change Learning mastery, remediation, Practice status, or gate results.

## Choosing and replacing the examiner

There is no shared queue: every human-graded submission waits for one named examiner, and no other examiner sees it.
A submission can wait without an examiner only for jobs created before this rule; the learner then chooses.

Role `CUSTOMER`, own jobs only.

- `GET /api/assessments/grading-jobs/mine` — the learner's human grading jobs:
  `{ id, reviewSubmissionId, skill, source, state, examinerId, requestedAt, deadline, canChoose, changesLeft, createdAt,
  band, completedAt, canCancel }`.
  `state` is `AWAITING_CHOICE` (no examiner yet), `WAITING`, `OVERDUE` (no result 48 hours after `requestedAt`),
  `COMPLETED`, `CANCELLED` (the learner gave the job up, see below) or `CLOSED`. `deadline` is `requestedAt` + 48 hours;
  `changesLeft` starts at 2; `canCancel` is true for an `OVERDUE` job with `changesLeft` 0.
- `POST /api/assessments/grading-jobs/{id}/examiner` — body `{ "examinerId": "…" }`; returns the updated job. Allowed
  when the state is `AWAITING_CHOICE`, or `OVERDUE` with `changesLeft > 0`. The whole attempt moves to the new examiner
  (one examiner grades all the open jobs of an attempt) and their 48 hours start again. Errors: `404` (not the
  caller's job), `409 EXAMINER_UNAVAILABLE` (full or not accepting), `409 EXAMINER_CHANGE_NOT_ALLOWED` with
  `details.reason` `NOT_OVERDUE`, `LIMIT_REACHED` or `SAME_EXAMINER`, `409` when grading already finished.
- `POST /internal/assessments/human-reviews/{reviewSubmissionId}/cancel` — the learner gives up a late review whose examiner
  can no longer be changed (called by Learning with the learner's token). The job becomes `CANCELLED`: nobody can claim,
  draft or complete it afterwards (a completion attempt answers `409`), and an unfinished draft is dropped. Answers the review
  with `status` `CANCELLED`; repeating it answers the same. Errors: `404` (not the learner's), `409
  GRADING_CANCEL_NOT_ALLOWED` with `details.reason` `NOT_OVERDUE` or `CHANGES_LEFT`, `409` when the examiner already graded
  it (the grade stays and nothing is cancelled). Giving the credit back is Learning's step, below.
- `POST /api/learning/examiner-reviews/{reviewSubmissionId}/cancel` — role `CUSTOMER`. Learning finds the learner's essay of
  that review, asks Assessment to cancel, then calls Access to refund the credit of that essay (both are idempotent, so after a
  failure the learner repeats the call and what already happened is not repeated). Answers the review with status
  `CANCELLED`. Errors: `404 NO_HUMAN_REVIEW`, `409 REVIEW_CANCEL_NOT_ALLOWED` (not late yet, or a change is left), `409
  HUMAN_GRADING_CONFLICT` (graded meanwhile), `503` when Access or Assessment cannot be reached. A cancelled review reads as
  `status` `CANCELLED` at `GET /api/learning/writing-submissions/{id}/human-review`, no longer blocks the same prompt
  (`EXAMINER_REVIEW_PENDING`), and asking the Mentor again for that same essay answers `409 REVIEW_CANCELLED`: the learner
  sends the essay again as a new one, which spends a new credit.

The flow of one job, from the moment the learner chooses the examiner:

0. **At once**: Assessment tells the **examiner** that a learner chose them (see
   [`grading-requested-v1.md`](grading-requested-v1.md)); the bell lights up and the queue and dashboard load the job without
   a reload.
1. **36 hours** without a result: Assessment reminds the **examiner** once that the job will be late in 12 hours (see
   [`grading-due-soon-v1.md`](grading-due-soon-v1.md)).
2. **48 hours**: the job becomes `OVERDUE`. Assessment announces it once to the **learner** through the notification service
   (see [`grading-overdue-v1.md`](grading-overdue-v1.md)). The late examiner is not told again.
3. The learner has two choices: **keep waiting** for the same examiner (nothing to call: the job stays with them and can still
   be graded) or **ask for another examiner** with `POST …/{id}/examiner`, at most twice and without another credit. The job
   then goes to the examiner the learner picked, not to a shared queue, and the 36 and 48 hours start again.
   Once both changes are used and the job is late again, the learner may instead **cancel** it and take the credit back
   (`POST /api/learning/examiner-reviews/{reviewSubmissionId}/cancel`, above).
4. **Done**: when the examiner finishes, Assessment tells the **learner** (see
   [`grading-completed-v1.md`](grading-completed-v1.md)); the bell opens the list of graded essays, and each essay shows the
   comments and the marked errors.

`reviewSubmissionId` in the `mine` answer is the review id Learning returns for a Practice or Lesson essay
(`GET /api/learning/writing-submissions/{id}/human-review`), so the learner's essay page finds its job; it is `null` for
jobs of an exam attempt.

## Examiner completion

`GET /api/assessments/grading-jobs` returns the caller's own queue: the open jobs whose chosen examiner is the caller
(administrators are not granted this endpoint). It does not include the learner's identity. `POST
/api/assessments/grading-jobs/{jobId}/claim` starts the job, and only the chosen examiner may claim it
(`409 JOB_ALREADY_CLAIMED` for anyone else, also for a job nobody was chosen for); `GET /api/assessments/grading-jobs/{jobId}/essay` returns its prompt, `images` (the prompt's chart, empty when it has none), essay, task, source, and rubric without
learner identity. Only the examiner who claimed it can submit the grade. Sampled quality-check jobs (reason
`QA_SAMPLE`) have no chosen examiner and keep their separate rule: any examiner may claim one after it has waited 24 hours.

An examiner takes at most `dailyLimit` new submissions a day (default 30, between 1 and 50, set by an administrator).
The limit is one number for all of the examiner's work, Writing and Speaking together: 20 Writing jobs leave room for 10
Speaking jobs, and neither kind has a quota of its own. A day runs from midnight to midnight in `Asia/Ho_Chi_Minh` (`assessment.examiner.day-timezone`). Every human job handed to
the examiner that day counts, finished or not, from the moment the learner chose them (a job given to someone else
after it was late counts for the new examiner from then on, and stops counting for the old one). Once the limit is
reached, choosing that examiner answers `409 EXAMINER_UNAVAILABLE` with `details.reason` `FULL`, and the learner chooses
another examiner. Jobs already held stay with the examiner. An examiner can also stop accepting new submissions.

An examiner with **5 or more open jobs already late** (chosen 48 hours ago or more, not graded) takes no new submission
either, whatever the day's count: choosing them answers `409 EXAMINER_UNAVAILABLE` with `details.reason` `OVERDUE_BACKLOG`.
It is temporary and needs nobody to lift it: once they grade enough for fewer than 5 to remain late, they can be chosen
again. Their late jobs stay with them and can still be graded; the learners who wait for them keep the choices above. The
limit is `ExaminerAssignmentPolicy.MAX_OVERDUE_OPEN`.

### Which examiners can still be chosen

`GET /api/assessments/examiners/availability?ids=<id>,<id>…` — role `CUSTOMER`, 1 to 50 ids (the ones listed by
`GET /api/users/examiners`). Answers `[{ "examinerId": "…", "full": true, "backlog": false }]`: `full` is true when the
examiner cannot take another submission now (reached today's limit, stopped accepting, or `backlog`); `backlog` is true when
the reason is the late jobs above, so the list can word it "temporarily not taking new submissions". Unknown ids are left
out. The limit itself is not shown. The learner's list greys out the examiners who cannot take one; the check at choosing
time stays the one that counts.

### The examiner's own numbers

`GET /api/assessments/examiners/me/summary` — role `EXAMINER`, for the examiner's dashboard and queue header. Answers

```json
{ "queued": 6, "processing": 2, "dueSoon": 3, "overdue": 1, "completedToday": 3, "completedThisWeek": 11, "receivedToday": 7, "dailyLimit": 30, "overdueLimit": 5 }
```

Only jobs learners chose this examiner for count; quality-assurance samples do not. `queued` and `processing` are the
open jobs by state; `dueSoon` are open jobs chosen at least 36 and less than 48 hours ago (the examiner was reminded);
`overdue` are open jobs chosen 48 hours ago or more. `receivedToday` is what the daily limit counts
(jobs chosen today, graded or not). `completedToday` and `completedThisWeek` use the job's completion time; a day starts
at midnight and a week on Monday, in `assessment.examiner.day-timezone` (default `Asia/Ho_Chi_Minh`, the same zone as the
limit). `dailyLimit` comes from User Service and is `null` when it cannot be reached; the other numbers are still right.
`overdueLimit` is the number of late jobs from which learners cannot choose the examiner (5): with `overdue` at or above it,
the dashboard tells the examiner why and that grading some late jobs opens it again.

### `POST /api/assessments/grading-jobs/{jobId}/human-review`

Role: `EXAMINER`. Overall and criterion bands are IELTS half-bands from 0 through 9.

```json
{
  "band": 6.5,
  "criteria": [{"criterion": "coherence", "band": 6.0, "comment": "Progression is clear."}],
  "errorCodes": ["LINKER_MISUSE", "TENSE"],
  "feedback": "Develop the main position further.",
  "annotations": [
    {"excerpt": "have rose", "start": 812, "end": 821, "errorCode": "TENSE", "suggestion": "have risen"}
  ]
}
```

`annotations` is optional, at most 200 entries. For each entry `excerpt` must be at most 500 characters and equal the
essay between `start` and `end` (`0 <= start < end <= essay length`, UTF-16 offsets), `suggestion` at most 1000.
Every code in `errorCodes` and `annotations` must be an active code of the error-type catalogue, reviewed or not
(`422 UNKNOWN_ERROR_CODE` otherwise); the review status only decides what a learner is shown. A range that does not
quote the essay answers `400`. Nothing is stored when any check fails.

Response: `204 No Content`. Repeating the identical grade (annotations included) is idempotent; a different grade after
completion returns `409`.

### Draft

An examiner who claimed the job can keep an unfinished review. Roles: `EXAMINER`, `ADMIN`.

- `PUT /api/assessments/grading-jobs/{jobId}/human-review/draft` — body: a JSON object, any shape up to 200 KB
  (normally the fields of the grade above, any of them missing). `204`. The first draft sets the review `IN_REVIEW`.
  Replaces the previous draft. `403` when the caller did not claim the job, `404` when it is not a standalone review,
  `409` once it is completed, `400` when the body is not an object or is too large.
- `GET /api/assessments/grading-jobs/{jobId}/human-review/draft` — `200` `{ "payload": {…}, "savedAt": "…" }`, or `204`
  when no draft was saved. Same `403` and `404`.

The draft is never returned to the learner, is removed when the review is completed, and is dropped when another
examiner takes over an expired claim. The strict checks above run only on completion.

### Reading again what the examiner graded

Role `EXAMINER`. An examiner can always read what they finished; nobody else can, and the learner is never named.

- `GET /api/assessments/examiners/me/completed?page=0&size=20` (size 1 to 100, else `400`) — the jobs this examiner
  finished, newest first: `{ content: [{ jobId, source, skill, task, band, completedAt }], page, size, totalElements }`. Only
  standalone reviews (Practice, Lesson) with a grade are listed; QA samples are not.
- `GET /api/assessments/examiners/me/completed/{jobId}` — one finished review with what it was about: `{ jobId, source,
  skill, task, prompt, images, essay, band, criteria, errorCodes, feedback, annotations, completedAt }`. Unlike the learner's read,
  `errorCodes` and each annotation's `errorCode` are all there (the examiner may record any active code). `404` when the job
  is another examiner's, is not finished, or has no grade.

The admin screen shows them under the tab "Đã chấm" of the queue, in a block on the dashboard, and in a read-only page.

These `/internal/assessments/...` endpoints require the gateway-signed internal service JWT. If a request has already
consumed a credit but Assessment cannot create the review, Learning calls the Access refund endpoint documented in
[`admin-area-v1.md`](admin-area-v1.md).
