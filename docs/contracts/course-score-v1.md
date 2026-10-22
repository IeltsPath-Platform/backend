# Course score and outcome v1

A course is passed by a weighted total, not by the course test alone. FE: course score page, topic status, Admin score
configuration screen.

## Rules

- **Topic test (mid-course).** The first topic test a learner takes finishes the topic and opens the next one, whatever the
  score. Its percent is the topic's mid-course score. Topic status gains `COMPLETED` (tested, below 70%) next to `PASSED`.
  A topic below 70% may be tested again; only the first test counts for the score.
- **Mid-course remediation.** A first topic test below `midRemediationBelow` (default 60%) creates a `MID_COURSE` flag, open
  while the blocking reviews made by that test are `PENDING`. It never locks the final test.
- **Course test.** Opens when every topic of the course has been tested; scores and pending reviews do not matter.
  Errors: `403 COURSE_TEST_LOCKED`, `409 COURSE_ALREADY_PASSED`, `409 REMEDIATION_REQUIRED` (retest before the reviews of the
  last course test are finished).
- **Total** = `processWeight` Process + `midWeight` mid + `finalWeight` final (defaults 0.2 / 0.3 / 0.5).
  - Process = `practiceShare` Practice (mean of first counted Practice submissions of the course's lessons) +
    `practiceTestShare` milestone practice tests (defaults 0.64 / 0.36); free practice tests do not count. The number of
    milestones comes from Content. A request reads it live; applying a course-test result runs outside a request, where
    Content refuses the call for lack of a learner token, so it uses the count kept when the course test was assigned
    (`course_milestone_counts`).
  - mid = mean first-test percent of the course's topics that have a topic test.
  - final = the newest course test.
  - A component the learner has not done is 0. A component the course does not have is left out and its weight shared out
    among the others, inside Process and in the total.
- **Decision.** After each course test: total >= `passThreshold` (default 0.70) -> `PASSED` (final; the course is passed).
  Otherwise `REMEDIATION`: a `POST_COURSE` flag, the most frequent error codes of the test, and the blocking reviews already
  made for its wrong knowledge points. When those reviews are no longer `PENDING` the status reads `RETEST_READY` and the
  learner takes the course test again with another code. A retest replaces the final component.
- A configuration change moves the totals of undecided courses; a `PASSED` course keeps its decision.

## `GET /api/learning/courses/{courseId}/score`

Role `CUSTOMER`, own score only. `GET /api/learning/admin/learners/{userId}/courses/{courseId}/score`: `EXAMINER`.

```json
{
  "courseId": "…", "status": "REMEDIATION",
  "components": { "practice": 0.8, "practiceTest": null, "process": 0.8, "mid": 0.75, "finalTest": 0.6 },
  "weights": { "process": 0.2, "mid": 0.3, "finalTest": 0.5, "practiceShare": 1.0, "practiceTestShare": 0.0 },
  "total": 0.685, "passThreshold": 0.7, "midRemediationBelow": 0.6,
  "topErrorCodes": ["TFNG_NG_AS_FALSE"],
  "remediation": [{ "kind": "POST_COURSE", "topicId": null, "open": true, "createdAt": "…" }],
  "decidedAt": "…"
}
```

`status`: `IN_PROGRESS`, `REMEDIATION`, `RETEST_READY`, `PASSED`. A component is `null` when the course does not have it.
`404 COURSE_NOT_FOUND`.

## `GET` / `PUT /api/learning/admin/score-config?courseId=`

Role `CONTENT_AUTHOR`. Without `courseId` it is the default every course without its own configuration follows. `GET` adds
`inherited` (true when the course has none of its own). `PUT` replaces the whole configuration; values are fractions of 1:

```json
{ "processWeight": 0.2, "midWeight": 0.3, "finalWeight": 0.5, "practiceShare": 0.64, "practiceTestShare": 0.36,
  "passThreshold": 0.7, "midRemediationBelow": 0.6 }
```

The three weights must add up to 1, the two shares to 1, `passThreshold` in (0, 1]. Otherwise `400 INVALID_SCORE_CONFIG`; a
missing value is rejected by validation. The change is attributed to the verified admin.
