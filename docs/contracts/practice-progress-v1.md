# Practice progress v1 (Speaking and Writing)

The practice-test library has a "done" tab. For Reading and Listening it reads
`GET /api/assessments/practice/progress` (Assessment keeps those attempts). Speaking answers and free Writing essays are
kept by Learning, so Learning answers the same question for them.

## `GET /api/learning/practice-progress`

CUSTOMER only, like the other learner routes of Learning. Query parameters, both required:

| Parameter | Values |
| --- | --- |
| `skill` | `SPEAKING` or `WRITING` (anything else is `400`) |
| `scope` | `PART` or `FULL`, the same meaning as in the catalog filter |

Response:

```json
{"completed":[{"packageId":"6b2bf327-9455-4be7-942c-f24c5669f422","partNo":2,"submissionId":"0f1d6c1e-5b0a-4a52-9d6a-7a7a3f0c2b11"}]}
```

`packageId` and `partNo` are the key the catalog filters by (`only` and `exclude` of `POST /api/content/practice-tests/search`);
`partNo` is `null` for a full test. `submissionId` is the graded answer to open: the Speaking result page
(`/practice/speaking/submissions/{id}/result`) or the Writing result page (`/practice/writing/submissions/{id}/result`).

### What counts

- Only a **graded** answer counts: a Speaking submission with status `GRADED` (graded and charged) or a free essay with
  status `GRADED`. An answer still waiting, failed, or that needs examiner review does not, so the learner can do the test
  again from the "not done" tab.
- Speaking: the submission must answer a catalog test. A lesson's own Speaking practice answer (it has a lesson) is left
  out. `scope=PART` returns the parts answered one at a time, `scope=FULL` the full tests.
- Writing: free essays only (`FREE_WRITING`). A Writing test is always one task, so `scope=FULL` returns an empty list.
- A test is listed once. When the same test was graded several times, or on more than one version of its package, the
  most recent graded answer is returned: the package id is the same across versions.
- The list holds at most the 500 most recent tests, which is the most keys the catalog accepts in one filter.

Learning reads its own tables first and then asks Content, with no transaction open during that call, through
`POST /internal/learning-content/practice-test-keys` (see [Internal learning content](./learning-content-internal-v1.md)).
A Content outage is answered as `503 CONTENT_UNAVAILABLE`, like the other Learning reads that need Content. The route
falls under the default rate-limit tier of the gateway.

**Status: proposed 2026-11-12.**
