# Assessment grading v1

Updated: 2026-10-16. Base path `/api/assessments`. Identity comes from the verified
internal JWT. All queue and grading endpoints require `EXAMINER` or `ADMIN`.
Customer submission/job creation and owner-only job lookup keep their existing contracts.

## Blind examiner queue

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/grading-jobs?mode=HUMAN&status=QUEUED&page=0&size=20` | Oldest first, stable tie break by job id. `page >= 0`, `size` 1–100. Only HUMAN mode. Status: QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED. |
| POST | `/grading-jobs/{id}/claim` | Returns the assigned queue entry. Existing live claim or unavailable terminal job: 409, `code: JOB_ALREADY_CLAIMED`. Missing/non-human id: 404. |
| GET | `/grading-jobs/{id}/essay` | Only the assigned examiner; otherwise 403. Missing/non-human job: 404. |

List response: `{content: [...], page, size, totalElements}`. Each entry contains
`id`, `submissionId`, `attemptId`, `attemptItemId`, `attemptType`, `skill`, `task`,
`status`, `reason`, `assignedExaminerId`, `claimedAt`, `createdAt`, `waitSeconds`.
Attempt fields may be null for standalone submissions. Timestamps use ISO-8601 UTC.
Reason is COURSE_GATE_POLICY, LLM_FAILED, LLM_QUOTA, LLM_NOT_CONFIGURED or HUMAN_CREDIT.
QA_SAMPLE is reserved in the schema and has no workflow yet.

A claim remains QUEUED until finalization, so the default queue shows its assignee.
After **more than 24 hours**, an unfinished claim can be replaced. Reading/saving/finalizing
uses the current assignee; the previous examiner loses access when another examiner
reclaims it. Claims and result writes lock the same job rows.

Because result finalization covers the whole attempt, claiming an essay also claims
the other unassigned/expired HUMAN jobs of that attempt. A live claim held by another
examiner blocks the claim. This keeps one examiner responsible for the attempt.
Standalone jobs are claimed individually. ADMIN follows the same claim rules.

Essay response: `{jobId, attemptId, attemptItemId, prompt, essay, task, rubric}`.
`prompt` is the snapshotted Content stem (standalone: submission stem), and `rubric`
is currently `null`; versioned rubric retrieval belongs to the next rubric feature.
Queue, claim and essay responses never include LLM bands, criteria, feedback or raw
job entities. They also never include the answer snapshot.

## Grading and finalization

The existing endpoints remain:

1. `POST /grading/attempts/{attemptId}/results` opens a DRAFT, optional `{overallBand}`.
2. `PUT /grading/results/{resultId}/details` saves the examiner's item scores,
   skill scores, error codes, KP judgments and overall band.
3. `POST /grading/results/{resultId}/finalize` completes a fully graded result and
   writes `AssessmentCompleted.v2` to the outbox in the same transaction.

Queued essays require the assigned examiner for every step. For each submitted
COURSE_GATE writing essay, put its explicit band in the existing JSON **string**
`itemResults[].feedbackSnapshot`; overall attempt band and item points are separate:

```json
{
  "overallBand": 6.5,
  "itemResults": [{
    "attemptItemId": "00000000-0000-4000-8000-000000000001",
    "score": 1,
    "maxScore": 1,
    "correct": true,
    "feedbackSnapshot": "{\"band\":6.5,\"criteria\":[{\"code\":\"TR\",\"band\":6.5}]}"
  }]
}
```

Band must be numeric 0–9 in half-band steps. Missing/invalid essay band blocks
finalization with 400; result, queue and comparisons remain uncommitted. Rubric
criteria are stored as supplied; rubric validation is deferred. Error codes must
exist in the active catalog or saving returns 422 `UNKNOWN_ERROR_CODE`.
Every item still requires score/maxScore; finalization is idempotent for an already
completed result version. A regrade opens another result version and reopens its
completed HUMAN jobs as unassigned QUEUED jobs. The examiner then claims the attempt
before reading essays or saving details; another examiner can perform the regrade.
Learners cannot modify course result details through the legacy owner route.

## Course essay policy and comparisons

On submission, each sent COURSE_GATE essay queues both AI and HUMAN jobs. The HUMAN
reason is COURSE_GATE_POLICY, retained if the parallel LLM job fails. LLM completion
cannot finalize the course result. Objective-only course tests retain deterministic
auto grading; topic, practice and placement grading policies remain unchanged.
For an omitted course essay, submission records explicitly preserve the blank
answer (`text_payload: ""`, `prompt_snapshot.omitted: true`) and queue it for a human.
No LLM job is created for an omitted answer. Thus even a course attempt with every
essay omitted is discoverable and can be scored zero by an examiner.

Each submitted essay has one comparison row keyed by submission id. Actual LLM
completion upserts band, criteria and prompt version (`essay-rubric-v1`); examiner
finalization upserts explicit essay band, criteria, catalog error codes and examiner
identity/time, using the actual assigned submission even if another submission
was sent later. Either order preserves the other half. A grader-supplied essay band
also records comparisons when a non-course attempt is manually regraded.

When the LLM fails or is unavailable, examiner finalization remains possible and
`llm_*` stays null. No default band is invented to fill a comparison. A placement
fallback band is not recorded as an actual LLM grade. Comparisons are internal;
no comparison-reading endpoint is exposed to examiners.

## Provisional learner feedback

`GET /attempts/{id}/provisional-feedback` is owner-only and COURSE_GATE-only (404
for another learner or another attempt type):

```json
{
  "pendingExaminer": true,
  "message": "Đang chờ giám khảo. Nhận xét tạm thời, chưa phải kết quả chính thức.",
  "essays": [{"attemptItemId": "...", "summary": "...", "comments": ["..."], "focus": ["..."]}]
}
```

Only text from completed LLM feedback's summary, criteria comments and focus is
projected. Band fields, scores and raw JSON are excluded. Before LLM completion or
after a model failure, essays may be empty. After examiner finalization,
`pendingExaminer` is false and the message explains the comments remain provisional.
The normal learner result endpoint continues to return only completed results.
