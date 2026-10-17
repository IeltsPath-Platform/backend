# Independent human review contract (v1)

This is the Learning-to-Assessment contract for Mentor feedback on graded lesson and Practice essays. The standalone
submission is stored separately from assessment attempts and results. It does not create an attempt, an assessment
result, or mastery evidence.

## Learner-facing API (Learning)

Role `CUSTOMER`, own essays only. Both calls forward the learner's bearer token.

`POST /api/learning/writing-submissions/{id}/human-review` — `202`, `{ "reviewSubmissionId": "…", "status": "REQUESTED" }`.
The essay (lesson block or Practice) must be `GRADED`. Learning spends one human-grading credit for the essay (Access is
idempotent by essay id), then asks Assessment for the review below. If Assessment cannot create it the credit is given
back. One request per essay: asking again returns the same review and spends nothing. The request id sent to Assessment
is derived from the essay id, so Assessment creates the review once.

`GET /api/learning/writing-submissions/{id}/human-review` — the review as returned by Assessment (below): `status`
`PENDING` or `DONE`, grade fields only when `DONE`.

Errors: `404 NOT_FOUND` (not the caller's), `404 NO_HUMAN_REVIEW` (nothing requested), `409 NOT_REVIEWABLE` (not graded),
`402 HUMAN_GRADING_CREDIT_REQUIRED`, `409 HUMAN_GRADING_CONFLICT`, `503 HUMAN_REVIEW_UNAVAILABLE`. The review is feedback
only: no evidence, no reviews, no change to Practice or gates. Learning stores only `human_review_requests`
(essay → review id); the essay text is sent to Assessment and never logged.

## Create a review

Learning calls Assessment with an internal service JWT after validating that the essay belongs to the current learner and
is already `GRADED`. Access credit consumption remains a separate call authenticated with the learner bearer token.

### `POST /internal/assessments/human-reviews`

```json
{
  "requestId": "8a8ead2e-91cc-42a0-a981-4f7f475f0b2b",
  "learnerId": "c26f2b10-d777-4888-ad68-6a51def59e01",
  "sourceSubmissionId": "9d272143-3d16-4ed8-ad24-f641eab4f906",
  "source": "PRACTICE",
  "task": "TASK_2",
  "task": "Write an essay discussing both views.",
  "promptSnapshot": {"stem": "Some people believe..."},
  "essay": "The essay text submitted by the learner.",
  "rubric": {"code": "IELTS_WRITING_TASK_2", "version": 1, "criteria": []}
}
```

`source` is `LESSON` or `PRACTICE`; `task` is `TASK_1` or `TASK_2`. `requestId` is the idempotency key. Repeating it
with the same payload returns the same review ID. Reusing it with another payload, or requesting a second review for the
same `sourceSubmissionId`, returns `409`. Assessment creates an independent submission and queues a `HUMAN` job with
reason `HUMAN_CREDIT`. The submission appears in the existing examiner queue with its source and rubric; it has no
attempt or result ID.

```json
{
  "requestId": "8a8ead2e-91cc-42a0-a981-4f7f475f0b2b",
  "reviewSubmissionId": "19d211e9-c99b-4dd7-bf2d-612fc728d5a4",
  "status": "PENDING"
}
```

## Read a review

### `GET /internal/assessments/human-reviews/{reviewSubmissionId}?learnerId={learnerId}`

```json
{
  "reviewSubmissionId": "19d211e9-c99b-4dd7-bf2d-612fc728d5a4",
  "status": "DONE",
  "band": 6.5,
  "criteria": [{"criterion": "coherence", "band": 6.0, "comment": "Progression is clear."}],
  "errorCodes": ["COHESION_DEVICE_OVERUSE"],
  "feedback": "Develop the main position further."
}
```

Pending response uses `status: "PENDING"` with grade fields omitted. A `DONE` response contains the examiner's overall
band, criteria, error codes, and feedback. The response never includes the examiner's identity. The review is feedback
only: it does not change Learning mastery, remediation, Practice status, or gate results.

## Examiner completion

The existing `GET /api/assessments/grading-jobs` queue includes the `HUMAN_CREDIT` job. `POST
/api/assessments/grading-jobs/{jobId}/claim` assigns the job, and `GET
/api/assessments/grading-jobs/{jobId}/essay` returns its prompt, essay, task, source, and rubric without learner identity.
Only the assigned examiner can submit the grade:

### `POST /api/assessments/grading-jobs/{jobId}/human-review`

Roles: `EXAMINER`, `ADMIN`. Overall and criterion bands are IELTS half-bands from 0 through 9.

```json
{
  "band": 6.5,
  "criteria": [{"criterion": "coherence", "band": 6.0, "comment": "Progression is clear."}],
  "errorCodes": ["COHESION_DEVICE_OVERUSE"],
  "feedback": "Develop the main position further."
}
```

Response: `204 No Content`. Repeating the identical grade is idempotent; a different grade after completion returns
`409`.

These `/internal/assessments/...` endpoints require the gateway-signed internal service JWT. If a request has already
consumed a credit but Assessment cannot create the review, Learning calls the Access refund endpoint documented in
[`admin-area-v1.md`](admin-area-v1.md).
