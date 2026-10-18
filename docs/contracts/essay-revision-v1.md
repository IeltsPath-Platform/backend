# Essay revision v1

A learner can rewrite a graded lesson essay and send it again, then compare the versions. FE: "Sửa và nộp lại" on the
essay result, side-by-side comparison, band per version chart.

## `POST /api/learning/writing-submissions/{id}/revisions`

Role `CUSTOMER`, own essays only. Body as the lesson essay submission: `{ "requestId": "…", "essayText": "…" }`. The
response is the usual essay result (`lesson-writing-v1`) of the new version.

- The revision goes through the existing essay flow: validation, balance check, daily limit, LLM grading, point charge and
  `requestId` idempotency. The grader gets the same prompt and rubric; it is **not** told about the earlier version or its
  feedback. Evidence and the question event log follow the existing rules, unchanged.
- `{id}` must be a **graded lesson essay** that is the newest of its chain. Revisions chain: a revision of a revision is
  allowed, a branch from an older version is not. A failed revision does not use up the chain; send another with a new
  `requestId`.
- At most **three revisions** per original essay.
- Resending the same `requestId` continues or replays that revision and charges nothing more.

Errors: `404 NOT_FOUND` (not the caller's), `409 NOT_REVISABLE` (not graded, or a practice essay), `409 REVISION_NOT_LATEST`,
`409 REVISION_LIMIT`, plus the errors of an essay submission (`402`, `422`, `429`, `503`, `409 GRADING_IN_PROGRESS`).

## `GET /api/learning/writing-submissions/{id}/history`

`{id}` is any version of the chain.

```json
{
  "rootSubmissionId": "…",
  "versions": [
    { "submissionId": "…", "revisionNo": 1, "wordCount": 262, "overallBand": 5.5, "deltaOverall": null,
      "criteria": [{ "code": "TR", "band": 5.5, "delta": null }], "errorCodes": ["GRAMMAR"] },
    { "submissionId": "…", "revisionNo": 2, "wordCount": 270, "overallBand": 6.5, "deltaOverall": 1.0,
      "criteria": [{ "code": "TR", "band": 6.5, "delta": 1.0 }], "errorCodes": [] }
  ],
  "resolvedErrorCodes": ["GRAMMAR"], "remainingErrorCodes": [],
  "canRevise": true, "nextRevisionNo": 3, "reviseFromSubmissionId": "…"
}
```

- Only graded versions are listed, oldest first; a withheld grade never appears. `delta` is against the version before.
  The grading is automatic, so deltas include grader noise.
- `resolvedErrorCodes` / `remainingErrorCodes` compare the two newest versions (empty with one version). Codes are the
  reviewed error codes of the essay corrections.
- `canRevise` is false when the newest version cannot be revised (limit reached, or a revision is already in progress);
  otherwise revise from `reviseFromSubmissionId`.

## In the learner report

An essay entry in the `evidence` block of `GET /api/learning/report` (`source = lesson_writing`) also carries `revisionNo`
(1 for the first essay) and `revisionDelta` (its overall band minus the band of the version it revised, absent for a first
essay). Other evidence entries do not have these fields. Both are derived from the revision chain, so the question event
log has no `revision_no` column. The grading is automatic: a delta includes grader noise.
