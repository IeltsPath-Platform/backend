# Overview statistics v1

Additive read for the learner overview. The service reads its own database and derives the user from the verified
internal JWT; caller-supplied user IDs and identity headers do not select another learner. No requests to other services
or new grading workflows are involved.

## `GET /api/learning/statistics/overview`

Role `CUSTOMER`. No query parameters.

```json
{
  "generatedAt": "2026-11-08T05:00:00Z",
  "dataFrom": "2026-10-15T00:00:00Z",
  "totalStudySeconds": 86400,
  "completedLessons": 27
}
```

- `dataFrom` is the global study-tracking policy's availability timestamp, as recorded by
  `study_tracking_policy.started_at`; it is not the learner's account creation time or a claim that all earlier activity
  was recorded. Imported historical totals, when present, are also included.
- `totalStudySeconds` sums all the learner's `study_daily_totals.active_ms` for scope `ALL`, then converts the sum to whole
  seconds. Course scopes are excluded to avoid counting the same time twice. There is no date-window limit.
- `completedLessons` counts distinct lesson IDs with a non-null `lesson_progress.completed_at`, across every course.
  It does not depend on progress pages, Content metadata, or the number of knowledge points/skills in a lesson.
- A learner without records receives zero counts and the tracking availability timestamp.

## Stored diagnostic placement

`GET /api/learning/report` and its existing staff counterpart add `placementBand: number | null`, copied from the
learner's stored diagnostic placement. It is separate from `estimatedBand` and does not change the estimation rules.
See [learner-report-v1](learner-report-v1.md).

The existing owner-service security rules and error envelopes continue to apply. These endpoints do not change study
streaks, mastery, wallet balances, achievements, or grading state.

## `GET /api/assessments/me/statistics/bands/availability`

Authenticated owner-only read for choosing band-history filters. Required `from` and `to` are ISO local dates;
`timezone` is an IANA zone ID and defaults to `Asia/Ho_Chi_Minh`, matching the existing band-history endpoint.
The dates must satisfy `from <= to` and `DAYS.between(from, to) <= 365`. Invalid dates, range or timezone return
`400` using the existing error envelope; unauthenticated requests return `401`.

```json
{
  "options": [
    {"seriesKind": "MOCK", "skill": "READING", "latestSubmittedAt": "2026-11-08T05:00:00Z"}
  ]
}
```

- Each option is a distinct eligible `(seriesKind, skill)` pair. No eligible results returns `{"options":[]}`.
- Series kinds are `PLACEMENT`, `MOCK`, `OFFICIAL_PRACTICE`; skills are `LISTENING`, `READING`, `WRITING`,
  `SPEAKING`, `OVERALL`. `PLACEMENT/SPEAKING` and `PLACEMENT/OVERALL` are always excluded.
- Only the verified caller's attempts submitted from local midnight on `from` inclusive to local midnight after
  `to` exclusive qualify. Daylight-saving transitions follow the requested zone.
- Select each attempt's highest `COMPLETED` result version before checking eligibility. An ineligible completed
  regrade never revives an older result. A newer pending version does not hide the last completed result.
- A skill needs a stored band in `[0,9]` with grading source `AUTO`, `AI` or `HUMAN`. Overall additionally needs
  all four eligible skill scores on that same result and a stored overall band in `[0,9]`; it is not estimated.
- `latestSubmittedAt` is the maximum eligible attempt submission instant for that pair, serialized as an ISO instant.
  Results are ordered by `seriesKind`, then `skill` lexically. The response has at most 13 options.
- The existing `GET /api/assessments/me/statistics/bands` history, pagination and summary contract is unchanged.
  Availability and history reuse the same eligibility query; availability groups all options in one database query.
