# Placement assignment and retake v1

Updated: 2026-11-18. This contract covers Learning's pool-based exam assignment and Assessment's independent started-attempt limit. Placement is optional: it informs the learner's diagnostic band and course recommendation but does not gate courses, topics or lessons.

## Learning routes

All routes require a learner bearer token through Gateway and role `CUSTOMER`. The learner identity comes from the verified token. Content's internal placement catalogue is not exposed to learners.

### `GET /api/learning/placement-test/options`

Returns the available exam count for each pool, the Learning assignment limit, the learner's current quota and latest placement state. `nextRetakeAt` is null while an assignment is available; at the limit it is the earliest counted assignment's `assignedAt + 24 hours`.

```json
{
  "pools": [
    {"pool":"BAND_0_3","examCount":2},
    {"pool":"BAND_3_5","examCount":2},
    {"pool":"BAND_5_9","examCount":1}
  ],
  "limitPer24h":3,
  "retakesLeft":2,
  "nextRetakeAt":null,
  "hasPlacement":true,
  "currentBand":5.5,
  "openAssignment":null
}
```

`openAssignment`, when present, contains `{assignmentId, pool, packageId, packageVersionId, retakesLeft, nextRetakeAt}`. It is the learner's newest incomplete, unreleased assignment, including an assignment older than the 15-minute reuse window.

### `POST /api/learning/placement-test/assignments`

Request: `{"pool":"BAND_0_3"}`. Valid pools are `BAND_0_3`, `BAND_3_5` and `BAND_5_9`. Returns `{assignmentId, pool, packageId, packageVersionId, retakesLeft, nextRetakeAt}`. The returned `packageVersionId` is used to start an Assessment attempt; the client should resume an existing in-progress placement attempt before requesting another assignment.

Learning permits three unreleased assignments per learner in a rolling 24-hour window. The window counts `assignedAt > now - 24 hours`; released assignments are excluded. At the limit, the route returns HTTP 429 `PLACEMENT_LIMIT_REACHED`. When an incomplete assignment for the same pool is less than 15 minutes old, the same assignment is returned and does not consume another slot. Assessment separately limits started attempts, so releasing an assignment does not refund a started-attempt slot.

Assignment reads the published catalogue for the requested pool outside the database transaction. The catalog is cached for 30 seconds. If no published package is available, the route returns 404 `NO_PLACEMENT_TEST`. An unused package is selected at random; after all packages in the pool have been assigned, Learning chooses the package with the oldest latest assignment. Released assignments are excluded from this rotation.

### `POST /api/learning/placement-test/assignments/{assignmentId}/release`

Releases any owned, incomplete assignment and returns `204 No Content`. Use it when an assignment cannot be used to start its exam. A released assignment no longer appears as open, consumes no Learning quota and does not count in package rotation. Completed or unowned assignments return 404 `PLACEMENT_ASSIGNMENT_NOT_FOUND`.

## Assessment attempt limit and grading

Assessment independently allows three started `PLACEMENT` attempts per learner in a rolling 24-hour window. A fourth start returns HTTP 429 `PLACEMENT_LIMIT_REACHED`. Cancellation is available only to the owner of an in-progress placement attempt through `POST /api/assessments/attempts/{id}/cancel`; it does not refund the started-attempt quota. This Assessment limit applies even when an attempt was started without a Learning assignment.

Listening and Reading convert objective correct-answer counts to their existing IELTS bands. Writing uses graded submitted essays, with Task 2 weighted twice as much as Task 1. An essay not submitted contributes 0. If a submitted essay has no grade when the result is completed, Writing is omitted and `partial` is true. Speaking is omitted unless `assessment.placement.speaking-band` is configured; it has no default value.

Assessment averages the available skill bands, rounds to the nearest half band, then applies the pool ceiling: `BAND_0_3` → 3.0, `BAND_3_5` → 5.0, and `BAND_5_9` → 9.0. The persisted `overallBand` and `AssessmentCompleted.v2` event carry the capped value. The owned `GET /api/assessments/attempts/{id}/placement-result` response includes `pool`, `ceiling`, `reachedCeiling` and `partial`, in addition to `overallBand`, `completedAt`, skill bands and report sections. The pool and ceiling are null for a legacy placement attempt that has no pool.

Learning consumes completed Assessment events. For a placement result it best-effort matches the newest open assignment owned by the learner with the same package version and an assignment time no later than `completedAt`; on a match it records the assignment's completion time and attempt id. An assignment match failure is logged and does not roll back the learner's stored placement band.

## Errors

| HTTP | Code | Meaning |
| --- | --- | --- |
| 400 | `INVALID_PLACEMENT_POOL` | Assignment request has a pool outside the supported enum. |
| 404 | `NO_PLACEMENT_TEST` | No published package is available in the requested pool. |
| 404 | `PLACEMENT_ASSIGNMENT_NOT_FOUND` | Assignment is not open and owned by the learner. |
| 429 | `PLACEMENT_LIMIT_REACHED` | Learning assignment quota or Assessment started-attempt quota is exhausted. |

Content provides the internal published catalogue in [learning-content-internal-v1](learning-content-internal-v1.md) and retains `placementPool` in the package-version response. Assessment grading details are in [assessment-grading-v1](assessment-grading-v1.md); the event shape is defined by [assessment-completed-v2](assessment-completed-v2.md).
