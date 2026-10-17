# Independent human review contract proposal (v1)

This is the proposed Learning-to-Assessment contract for Mentor feedback on graded lesson and Practice essays. It is a
contract only: the Assessment standalone-submission persistence design and Learning integration require review before
implementation. It does not reuse assessment attempt result or mastery APIs.

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
  "task": "Write an essay discussing both views.",
  "promptSnapshot": {"stem": "Some people believe..."},
  "essay": "The essay text submitted by the learner.",
  "rubric": {"code": "IELTS_WRITING_TASK_2", "version": 1, "criteria": []}
}
```

`requestId` is the idempotency key. Assessment creates an independent submission and queues a `HUMAN` job with reason
`HUMAN_CREDIT`; the request does not create an attempt or assessment result.

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

Pending response uses `status: "PENDING"` with grade fields omitted. The response never includes the examiner's identity.
The review is feedback only: it does not change Learning mastery, remediation, Practice status, or gate results.

Before implementation, confirm the endpoint/authentication and standalone result storage with both service owners. If a
request has already consumed a credit but Assessment cannot create the review, Learning calls the Access refund endpoint
documented in [`admin-area-v1.md`](admin-area-v1.md).
