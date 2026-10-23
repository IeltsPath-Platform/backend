# Admin area and human grading contract (v1)

This contract covers the Assessment and Access endpoints implemented for examiner quality review and paid human grading.
Learning-owned learner profiles, flags, work items, and notes are outside this contract.

## Examiner quality review

All endpoints require an internal JWT.

### `GET /api/assessments/qa-reviews/queue`

Role: `CONTENT_AUTHOR`. Returns queued `HUMAN` jobs with reason `QA_SAMPLE`. Essay projections are blind: they
do not contain the original examiner or band. Jobs older than 48 hours include `overdue: true`.

```json
{
  "content": [{
    "id": "a7f5c520-4ee8-45a5-a63b-b64fdfed2b76",
    "submissionId": "c26f2b10-d777-4888-ad68-6a51def59e01",
    "attemptId": "f0ad2c50-d55d-4dc5-af49-fac127718b93",
    "attemptItemId": "7a50ce25-8ef1-4877-a035-9c3f922d1ca2",
    "attemptType": "TOPIC_GATE",
    "source": "TOPIC_GATE",
    "skill": "WRITING",
    "task": "TASK_2",
    "status": "QUEUED",
    "reason": "QA_SAMPLE",
    "assignedExaminerId": null,
    "claimedAt": null,
    "createdAt": "2026-10-17T02:00:00Z",
    "waitSeconds": 372,
    "overdue": false
  }],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

`POST /api/assessments/qa-reviews/{jobId}/claim` assigns a review to the caller, and
`GET /api/assessments/qa-reviews/{jobId}/essay` returns the essay and task without the original grade. The reviewer must
not be the examiner who graded the original submission.

### `GET /api/assessments/qa-reviews/mine?page=&size=`

Role: `CONTENT_AUTHOR`. The quality-review essays the caller has claimed and not yet reviewed, oldest claim first, so a
reviewer who left the page can find them again. Same page shape and entries as the queue (`overdue` included), with
`assignedExaminerId` and `claimedAt` set. `page` defaults to 0 and `size` to 20 (1 to 100, otherwise `400`). An essay
leaves this list as soon as its review is submitted (`POST /api/assessments/qa-reviews`), and it never lists essays that
are only queued or claimed by someone else.

### `POST /api/assessments/qa-reviews`

Role: `CONTENT_AUTHOR`. Submit a blind re-review. Reviewer band must be an IELTS half-band from 0 through 9.

```json
{
  "gradingJobId": "a7f5c520-4ee8-45a5-a63b-b64fdfed2b76",
  "reviewerBand": 6.5,
  "reviewerCriteria": {"taskResponse": 6.0, "coherence": 6.5, "lexical": 7.0, "grammar": 6.5},
  "note": "Task response is underdeveloped."
}
```

Response: `204 No Content`.

### `GET /api/assessments/admin/examiner-quality`

Role: `CONTENT_AUTHOR`. Returns one aggregate per original examiner with sample count, mean absolute band difference, and the
fraction of reviews whose difference is greater than one band.

```json
[{"examinerId": "…", "examinerName": "Linh Tran", "reviewedEssays": 4,
  "averageAbsoluteBandDifference": 0.50, "overOneBandRate": 0.2500}]
```

`examinerName` is the display name from the User Service and is `null` when it cannot be read. Assessment asks
`POST /internal/users/display-names` (see [user-internal-v1](user-internal-v1.md)) once for the whole table, in batches of
50 ids, with the caller's bearer token and `X-Correlation-Id`. A User Service error or timeout (2 s connect, 3 s read)
still returns the table, with every name `null`. The base URL is `assessment.user.base-url`
(`USER_SERVICE_URL`, default `http://localhost:8085`).

## Paid human grading for topic essays

### `POST /api/assessments/attempts/{attemptId}/items/{itemId}/human-grading`

Role: `CUSTOMER`. Only an essay submitted by the caller as part of a submitted `TOPIC_GATE` attempt can be requested.
Assessment consumes one Access human-grading credit, then creates an idempotent `HUMAN` job with reason
`HUMAN_CREDIT`. No database transaction is open while Access is called; the job is written in its own short
transaction. A request whose job already exists neither spends nor refunds anything. If job creation fails, Assessment
refunds the credit only when this call spent it (the consume response says `charged: true`) and the job is known not to
exist; if the refund also fails, the credit stays with the submission and asking again queues the job without spending a
second credit.

Response: `202 Accepted`.

```json
{"submissionId":"c26f2b10-d777-4888-ad68-6a51def59e01","status":"QUEUED"}
```

### Access internal credit API

Assessment forwards the learner bearer token so Access identifies the learner. Both operations are idempotent by
`submissionId`.

`POST /internal/access/human-grading/consume`

```json
{"submissionId":"c26f2b10-d777-4888-ad68-6a51def59e01"}
```

`POST /internal/access/human-grading/refund` takes the same request body. Both return:

```json
{"submissionId":"c26f2b10-d777-4888-ad68-6a51def59e01","remainingCredits":2,"charged":true}
```

`charged` is `true` when the call spent a credit and `false` when the credit of the submission had already been spent
(a repeat) or when the call was a refund. Only a caller that received `charged: true` may refund.

The Access endpoints return Access's standard `ErrorResponse` (`timestamp`, `status`,
`error`, `message`, `path`, `details`); they do not include a named `code` field.
Insufficient credits return `402`; reusing a submission ID as a different learner
returns `409`. Assessment maps those upstream statuses on its learner-facing request
to `HUMAN_GRADING_CREDIT_REQUIRED` and `HUMAN_GRADING_CREDIT_CONFLICT`, respectively.
Repeating consume or refund for the same learner and submission is idempotent. A
refund for a submission with no recorded consumption returns `400`. Successful
consumption writes `HumanGradingCreditConsumed`; a successful compensation writes
`HumanGradingCreditRefunded`.

## Not included

Learning-owned endpoints for lesson/Practice essays are described as a proposed contract in
[`human-review-v1.md`](human-review-v1.md); no Learning endpoint is implemented here.
