# Assessment grading v1

Updated: 2026-10-17. Base path `/api/assessments`. Identity comes from the verified
internal JWT. All queue and grading endpoints require `EXAMINER`.
Customer submission/job creation and owner-only job lookup keep their existing contracts.

## Blind examiner queue

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/grading-jobs?mode=HUMAN&status=QUEUED&page=0&size=20` | Oldest first, stable tie break by job id. `page >= 0`, `size` 1–100. Only HUMAN mode. Status: QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED. |
| POST | `/grading-jobs/{id}/claim` | Returns the assigned queue entry. Existing live claim or unavailable terminal job: 409, `code: JOB_ALREADY_CLAIMED`. Missing/non-human id: 404. |
| GET | `/grading-jobs/{id}/essay` | Only the assigned examiner; otherwise 403. Missing/non-human job: 404. |

List response: `{content: [...], page, size, totalElements}`. Each entry contains
`id`, `submissionId`, `attemptId`, `attemptItemId`, `attemptType`, `source`, `skill`,
`task`, `status`, `reason`, `assignedExaminerId`, `claimedAt`, `createdAt`,
`waitSeconds`, and `overdue`. Attempt fields may be null for standalone submissions.
For attempt jobs, `source` is the attempt type; standalone review jobs use `LESSON`
or `PRACTICE`. Timestamps use ISO-8601 UTC. Reason is COURSE_GATE_POLICY,
LLM_FAILED, LLM_QUOTA, LLM_NOT_CONFIGURED, HUMAN_CREDIT, or QA_SAMPLE. QA_SAMPLE
jobs use the QA review routes in [admin-area-v1](admin-area-v1.md#examiner-quality-review).

A claim remains QUEUED until finalization, so the default queue shows its assignee.
A job a learner chose an examiner for can be claimed only by that examiner
([`human-review-v1`](human-review-v1.md): there is no shared queue); the learner may replace
the examiner once the job is late, which drops the previous claim and unfinished review.
After **more than 24 hours**, an unfinished claim on a `QA_SAMPLE` job can be replaced
by another examiner. Reading/saving/finalizing uses the current assignee. Claims and
result writes lock the same job rows.

Because result finalization covers the whole attempt, claiming an essay also claims
the other unassigned/expired HUMAN jobs of that attempt. A live claim held by another
examiner blocks the claim. This keeps one examiner responsible for the attempt.
Standalone jobs are claimed individually.

Essay response: `{jobId, attemptId, attemptItemId, prompt, essay, task, rubric, source}`.
`prompt` is the snapshotted Content stem (standalone: submission stem). `source` is
the attempt type for attempt jobs, or `LESSON`/`PRACTICE` for standalone reviews.
`rubric` is the stored rubric snapshot for a standalone review; it is currently
`null` for regular attempt jobs. The response contains no answer snapshot. The stored
AI job records the rubric and shared template version as `prompt_version` in the form
`<rubricCode>@<rubricVersion>+tpl<templateVersion>`.
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
completed HUMAN jobs as unassigned QUEUED jobs that still belong to the examiner the
learner chose. That examiner claims the attempt again before reading essays or saving
details; no other examiner can take the regrade, as there is no shared queue.
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
completion upserts band, criteria and the applied prompt version; examiner
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

## AI rubric evaluation and activation (phase 11)

The AI evaluation routes below require `CONTENT_AUTHOR`. Gold essays and comparisons remain internal; the examiner queue never
exposes AI grades or comparison data.

| Method | Path | Behavior |
| --- | --- | --- |
| POST | `/api/assessments/admin/ai-gold-essays` | Add a manually rated gold essay; returns `201 {id}`. |
| POST | `/api/assessments/admin/ai-gold-essays/import-comparisons` | Import comparison rows with an examiner band; returns `{imported}` and skips submissions already imported. |
| POST | `/api/assessments/admin/ai-evaluations` | Start an asynchronous evaluation, optionally scoped to `{task:"TASK_1"}` or `{task:"TASK_2"}`; returns `202 {id}`. If task is omitted, Assessment chooses the task with the largest gold set. |
| GET | `/api/assessments/admin/ai-evaluations/{id}` | Read run metrics and per-essay comparisons, plus `thresholds` (below). |
| POST | `/api/assessments/admin/ai-prompt-activations` | Activate a passed evaluation with `{runId}`; returns `204`. Otherwise returns `409 EVALUATION_NOT_PASSED`. |
| GET | `/api/assessments/admin/ai-gold-essays?task=&page=&size=` | List the gold set (below). |
| GET | `/api/assessments/admin/ai-evaluations?task=&page=&size=` | List evaluation runs, newest first (below). |
| GET | `/api/assessments/admin/ai-prompt-activations` | The prompt version each task currently uses (below). |

### Read routes

All three lists are `CONTENT_AUTHOR` only, filtered and ordered in the database. `task` is optional (`TASK_1` or `TASK_2`,
otherwise `400`); `page` defaults to 0 and `size` to 20 (1 to 100, otherwise `400`). Pages are `{content, page, size,
totalElements}`.

- Gold essays, newest first: `{id, task, examinerBand, source, essayPreview, createdAt}`. `source` is `MANUAL` or
  `COMPARISON` (imported from a comparison row). `essayPreview` is the first 200 characters; the prompt, chart facts and
  criteria are not listed.
- Runs, newest first: `{id, task, model, promptVersion, status, n, withinHalfBand, mae, qwk, passed, startedAt,
  finishedAt}`. `status` is `RUNNING`, `DONE` or `FAILED`; metrics and `passed` are `null` until the run is done. The detail
  stays at `GET …/ai-evaluations/{id}`.
- Activations: always two entries, `TASK_1` then `TASK_2`: `{task, promptVersion, runId, activatedAt}`. A task never
  activated has `null` in the last three fields: it is still graded by Content's `ACTIVE` rubric, which no evaluation has
  approved (see "bootstrap" below).
- `GET …/ai-evaluations/{id}` also returns `thresholds: {withinHalfBand, qwk}`, the values in force
  (`assessment.ai-eval.min-within-half-band`, `assessment.ai-eval.min-qwk`).

Nothing here logs essay or prompt text.

Manual gold request fields are `task`, `prompt`, optional `chartFacts`, `essay`, `examinerBand`, optional
`examinerCriteria`, and optional `examinerErrorCodes`. Task 1 requires chart facts when sent to the grader. The examiner
band must be 0–9 in half-band steps. Imported rows are limited to `essay_grade_comparisons` with a final examiner band.

Evaluation runs record `model`, the prompt version used, `status`, `n`, `withinHalfBand`, `mae`, `qwk`,
`perCriterion`, `errorCodeFalsePositive`, `passed`, timestamps and item-level AI-versus-examiner differences. Metrics
use only the requested task's gold essays. QWK uses bands rounded to 0.5; pass defaults are `withinHalfBand >= 0.80`
and `qwk >= 0.60`, configurable as `assessment.ai-eval.min-within-half-band` and `assessment.ai-eval.min-qwk`.
Evaluation uses its own daily cap (`assessment.ai-eval.daily-limit`, default `100`) and does not consume a learner's
grading quota. Evaluation candidates always use the task's current Content `ACTIVE` rubric, independently of the
version used for runtime grading. Activation requires `passed=true`; otherwise it returns `409 EVALUATION_NOT_PASSED`.

Before a task has its first `ai_prompt_activations` row, Assessment's `GradeGateEssayJobUseCase` and Learning's
`EssayGrader` both use Content's current `ACTIVE` rubric. Missing activation alone does not produce
`PROMPT_NOT_ACTIVATED`. V39 seeds both tasks as `ACTIVE` for bootstrap, with `source_note` identifying them as AI drafts
that have not been reviewed by an IELTS expert.

After the task's first activation, both graders use only its activated `promptVersion`. Changing Content's active
rubric does not switch either grader until a new passed run is activated. Historical lookup includes `RETIRED` rubrics
and excludes `DRAFT`. Failure to load activation state or the required rubric never triggers bootstrap fallback:
Assessment hands the job to an EXAMINER and Learning returns a temporary grading failure.

### Internal activation lookup

`GET /internal/assessments/ai-prompt-activations?task=TASK_1` requires an authenticated internal JWT and accepts
`TASK_1` or `TASK_2`. This route is separate from the admin activation command and is not exposed through the gateway.
It returns `{"task":"TASK_1","promptVersion":null}` when the task has no activation row, or
`{"task":"TASK_1","promptVersion":"IELTS_WRITING_TASK_1@1+tplv1"}` for an activated version. Only a successful response
with a null version permits bootstrap; an outage is an error.

Learning calls this route using `learning.assessment.base-url`, default `http://localhost:8083`, then loads a non-null
version through Content's `GET /internal/learning-content/rubrics/{code}/{version}`.
See [internal Content rubric contract](./learning-content-internal-v1.md#essay-rubrics-phase-11).

Phase 11 remains `in_progress` until an expert-rated gold set and a real evaluation run with persisted metrics exist.
Bootstrap seed data and automated test fixtures do not satisfy that acceptance criterion.
