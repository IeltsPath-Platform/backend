# Free practice and the learner tree v1

A learner can practise any knowledge point (KP) of a course they are taking, without waiting for a lesson or a review. Free
practice never blocks the learner's path: a failed free set creates no review and locks nothing. FE: the knowledge tree screen
with a "Practise" button on each point.

Both routes are under `/api/learning`, role `CUSTOMER`.

## `GET /tree?courseId=<uuid>`

The learner's knowledge tree for one course: every KP with the learner's standing, what it builds on, and whether free practice
is open on it.

```json
{ "courseId": "…",
  "knowledgePoints": [{
    "knowledgePointId": "…", "code": "R60_TFNG_F_VS_NG", "name": "…", "skill": "READING", "kind": "STRATEGY",
    "cefrLevel": "B1", "canDo": "…", "mastery": 0.72, "tier": "CONSOLIDATE", "evidenceCount": 9, "nextReviewAt": "2026-10-25T00:00:00Z",
    "prerequisiteIds": ["…"], "practiceAvailable": true }] }
```

- `cefrLevel` (`A1` to `C2`) and `canDo` come from Content and are `null` when it has not set them.
- `mastery`, `tier`, `nextReviewAt` are `null` and `evidenceCount` is 0 for a point the learner has not touched.
- `tier` is `PRIORITY`, `CONSOLIDATE`, `MAINTAIN` or `INSUFFICIENT` ("not enough data").
- `practiceAvailable` is true when more unseen practice sets remain than the sets kept in reserve for reviews (see below).
  It does not look at pending reviews: starting on a point with a blocking review is still `403 REVIEW_REQUIRED`.
  It is also an upper bound: it does not subtract sets the start route will skip (above the course band, needing a plan the
  learner lacks, or holding an essay). Treat `practiceAvailable: true` as "may be possible" and handle
  `409 FREE_PRACTICE_EXHAUSTED` from the start route.
- The tree costs a fixed number of calls to Content (the tree and the prerequisites) and one query for the profiles, however
  many points it has.
- `404 NOT_FOUND` for an unknown course, `403 COURSE_NOT_OPEN` for a closed course, `403 COURSE_NOT_JOINED` when the learner is
  not taking it.

## `POST /knowledge-points/{id}/free-practice`

Empty body. Starts a practice attempt on one unseen practice set of the point and returns it with `201`, in the shape of the
practice attempt view (see [lesson-learning-v1](lesson-learning-v1.md)). Answers are submitted through the existing Practice
attempt submission route; that response carries the `gamification` block ([gamification-v1](gamification-v1.md)).

- Free practice costs hearts like Practice: starting with no hearts is `403 HEARTS_DEPLETED`.
- The last unseen sets of every point are kept for reviews (`learning.free-practice.reserve`, default 2): free practice starts
  only while more unseen sets remain than that, otherwise `409 FREE_PRACTICE_EXHAUSTED`. A set the learner has seen, or that
  a review holds, is never offered again.
- A set above the band of the learner's course is never offered. A set that needs a feature (`requiredFeatureKey`) is offered
  only to a learner with an active plan; Access answers only yes or no for the plan, so any active plan counts, and when
  Access cannot answer the learner counts as having none. When every candidate is filtered out the answer is
  `FREE_PRACTICE_EXHAUSTED`.
- `403 REVIEW_REQUIRED` while a blocking review is pending for the point.
- `403 COURSE_NOT_OPEN`, `403 COURSE_NOT_JOINED` as above; `404 NOT_FOUND` for an unknown point; `409
  FREE_PRACTICE_UNAVAILABLE` for a point with no skill to practise.
- Its answers count as practice evidence for mastery (under `MP1`, with the practice weight and at most three practice
  answers in the mastery window), and they earn answer XP like any first submission.
