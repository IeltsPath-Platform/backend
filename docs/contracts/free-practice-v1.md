# Free practice and the learner tree v1

A learner can practise any knowledge point (KP) of a course they are taking, without waiting for a lesson or a review. Free
practice never blocks the learner's path: a failed free set creates no review and locks nothing. FE: the knowledge tree screen
with a "Practise" button on each point.

Both routes are under `/api/learning`, role `CUSTOMER`.

## `GET /tree?courseId=<uuid>`

The learner's knowledge tree for one course: every KP with the learner's standing, what it builds on, whether free practice
is open on it, and where it is taught (its chặng and lessons).

```json
{ "courseId": "…",
  "knowledgePoints": [{
    "knowledgePointId": "…", "code": "R60_TFNG_F_VS_NG", "name": "…", "skill": "READING", "kind": "STRATEGY",
    "cefrLevel": "B1", "canDo": "…", "mastery": 0.72, "tier": "CONSOLIDATE", "evidenceCount": 9, "nextReviewAt": "2026-10-25T00:00:00Z",
    "prerequisiteIds": ["…"], "practiceAvailable": true, "freePreview": true,
    "blockingReview": { "reviewId": "…", "status": "DONE", "lessonId": "…", "createdAt": "2026-11-10T08:00:00Z",
                        "doneAt": "2026-11-11T09:30:00Z", "setsAnswered": 2, "lastCorrect": 8, "lastTotal": 10 },
    "topic": { "topicId": "…", "name": "Demo IELTS Reading", "position": 1 },
    "lessons": [{ "lessonId": "…", "title": "Câu chủ đề nằm ở đâu", "position": 1,
                  "topicId": "…", "topicName": "Demo IELTS Reading", "topicPosition": 1 }] }] }
```

- `cefrLevel` (`A1` to `C2`) and `canDo` come from Content and are `null` when it has not set them.
- `mastery`, `tier`, `nextReviewAt` are `null` and `evidenceCount` is 0 for a point the learner has not touched.
- `tier` is `PRIORITY`, `CONSOLIDATE`, `MAINTAIN` or `INSUFFICIENT` ("not enough data").
- `practiceAvailable` is true when more unseen practice sets remain than the sets kept in reserve for reviews (see below).
  It does not look at pending reviews: starting on a point with a blocking review is still `403 REVIEW_REQUIRED`.
  It is also an upper bound: it does not subtract sets the start route will skip (above the course band, needing a plan the
  learner lacks, or holding an essay). Treat `practiceAvailable: true` as "may be possible" and handle
  `409 FREE_PRACTICE_EXHAUSTED` from the start route.
- `freePreview` (added 2026-11-15) is true when a paying course teaches the KP in one of the first lessons of its skill, the
  lessons it shows free ("Học thử miễn phí" on the course page): the earliest lesson that teaches it is within the first
  `PaywallPolicy.FREE_LESSONS_PER_SKILL` of its skill. It is a fact about the KP, not about the learner: it stays true for a
  Premium learner, and `locked` says what this learner can open. It is false for a KP of a free course (all of it is free, there
  is no preview) and for a KP no lesson teaches.
- `blockingReview` (added 2026-11-15) is the learner's **blocking** review of the KP, kept after it is finished so the tree can
  keep showing it; `null` when the learner never had one. `status` is `PENDING` (it holds back lessons, Practice and topic tests
  of its skill), `DONE` (a set was passed) or `SKIPPED` (no unseen package was left). A KP holds at most one `PENDING` review;
  when it has none, the latest finished one is returned. Optional reviews (review basket, free practice test) never appear
  here. `lessonId` is the lesson that raised it, `createdAt` and `doneAt` (`null` while pending) bound it in time,
  `setsAnswered` counts the sets submitted, and `lastCorrect`/`lastTotal` score the latest one (`null` before any). `reviewId`
  opens it at `/learn/reviews/{reviewId}`. A `referencePoints` entry carries the same field, since a diagnosis can send the
  learner back to a point of a lower course.
- `topic` is the chặng that owns the KP (`null` only if Content names none). `topic.position` and `lessons[].topicPosition`
  count the chặng from 1 inside its own skill, the number the course page shows ("Chặng 03"); `lessons[].position` counts the
  lesson from 1 inside its chặng. `lessons` are the lessons of this course that teach the KP, in the order of their chặng and
  then of the lesson, and are empty for a KP no lesson teaches. This says where a KP is taught; `prerequisiteIds` says only in
  which order the points build on each other, and the two orders do not have to agree.
- `referencePoints` (added 2026-11-15) lists the points of **lower courses** that the course builds on, and what those build on
  in turn, so the chart can show the order of learning back to the start. A course does not re-teach what a lower course
  teaches, so they are not in `knowledgePoints`; they are named only for reference. They carry no standing of the learner and
  cannot be practised from this tree:

  ```json
  "referencePoints": [{
    "knowledgePointId": "…", "code": "G40_LINKING", "name": "…", "skill": "ALL", "cefrLevel": "A2",
    "prerequisiteIds": ["…"],
    "course": { "courseId": "…", "name": "Foundation 4.0", "bandLevel": 4.0 },
    "topic": { "topicId": "…", "name": "…", "position": 2 } }]
  ```

  `prerequisiteIds` are what the reference point builds on; an id may name a point of the course, another reference or one that
  is not listed. `topic.position` counts the chặng from 1 inside its skill **in its own course**. Only a point that some other
  course owns is listed (one Content no longer lists is left out), and the list is empty when the course builds on nothing
  outside it. At most three levels are followed (what the course builds on, what that builds on, and once more). A client that
  does not know the field ignores it; `knowledgePoints` and its `prerequisiteIds` are unchanged.
- The tree costs a fixed number of calls to Content (the tree and the prerequisites), one more call per chặng of the course for
  its lessons, up to three more for the levels of `referencePoints` (none when the course builds on nothing outside it), and one
  query for the profiles, however many points it has. A chặng whose lessons cannot be read is left out of `lessons`; the tree is
  still returned.
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

## Error-trap fields (2026-10-25)

The learner tree lists only parent knowledge points at the top level. A parent may include `errorTraps`, containing its
trap child KPs with at least one learner evidence row. Each child has `{knowledgePointId, code, name, tier, mastery,
evidenceCount}`. Child points do not affect the tree's prerequisites or practice availability. Learner report uses the
same nested shape. A report parent's `errors` contains only the main error code selected from its trap child mastery;
the stored `top_error_codes` field remains available for staff statistics.

Practice attempt results may include `error: {code, name, fixHint}` when an incorrect answer has a recognized active
error type. Missing/unavailable error text omits the field and does not fail submission.
