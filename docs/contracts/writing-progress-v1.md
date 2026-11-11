# Writing progress v1

Read-only view of a learner's graded Writing essays across time. It summarizes the stored grades; it does not call the
LLM, change mastery, evidence, gates or quota, and it does not return essay text or chart facts.

## Endpoint

`GET /api/learning/writing/progress?task=TASK_1|TASK_2&limit=10`

- Role `CUSTOMER`. The learner is taken from the token.
- `task` is optional. Omitted, the response has one block per task (`TASK_1` then `TASK_2`).
- `limit` is optional, 3 to 20, default 10: the number of essays read per task.
- `400 INVALID_TASK` for any other `task`; `400 INVALID_LIMIT` outside 3–20.
- Gateway routes it under `/api/learning/**`, rate-limit tier `default`.

## Which essays count

- Status `GRADED` and `revision_no = 1`: the first graded submission of an essay. Revisions are left out, because their
  feedback leads the learner to a corrected text rather than showing the level written unaided.
- Sources `LESSON_BLOCK`, `PRACTICE_ITEM` and `FREE_WRITING` all count; the response labels them `LESSON`, `PRACTICE`
  and `FREE`.
- The newest `limit` essays of the task, newest first in the query and oldest first in `points`.

## Response

```json
{
  "tasks": [
    {
      "task": "TASK_2",
      "insufficient": false,
      "points": [
        {
          "submissionId": "…",
          "source": "LESSON",
          "gradedAt": "2026-11-05T10:00:00Z",
          "overallBand": 5.5,
          "criteria": {"TR": 5.5, "CC": 6.0, "LR": 5.0, "GRA": 5.5},
          "promptVersion": "writing-rubric@v1+tplv1"
        }
      ],
      "recurringErrors": [
        {
          "errorCode": "TENSE",
          "errorName": "Tense",
          "fixHint": "…",
          "practiceKnowledgePointId": "…",
          "essaysWithError": 3,
          "essaysTotal": 5,
          "lastSeenAt": "2026-11-09T09:00:00Z"
        }
      ],
      "fixedErrors": [
        {"errorCode": "ARTICLE", "errorName": "Article", "lastSeenAt": "2026-10-20T08:00:00Z", "cleanEssaysSince": 3}
      ]
    }
  ]
}
```

- `points`: one per essay, criteria keyed `TA`/`TR`, `CC`, `LR`, `GRA`. Bands are the stored bands.
- `recurringErrors`: at most 10. Counted per essay: an error code listed twice in one grade counts once.
  Sorted by `essaysWithError` descending, then by severity descending. `practiceKnowledgePointId` and
  `practiceKnowledgePointCode` are present only when the error type maps to a knowledge point with a practice set.
  The code is copied into the grade when it is graded; grades stored before that have neither field.
- `fixedErrors`: a code whose last occurrence is followed by at least `learning.writing.progress-fixed-after` clean
  essays of the same task (default 2). `cleanEssaysSince` counts those essays. This is a signal, not a mastery verdict.
- `insufficient`: true when the task has fewer than 3 essays. The lists are then still returned.
- Catch-all codes (`*_OTHER`) sort last among recurring errors and never appear in `fixedErrors`.
- A field absent from an older grade (`errorName`, `fixHint`) is looked up once per request in Content's active error
  types. If Content is unavailable the name is left out; the request still succeeds.

## Not included

Essay text, `chartFacts`, knowledge-point judgments, study points, the model name and the raw grade JSON.

## Notes

- Each request makes one database query per task and at most one Content call.
- Migration `V52` adds an index on `(user_id, graded_at DESC) WHERE status = 'GRADED'`.
