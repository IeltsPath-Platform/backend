# Assessment package statistics v1

How often each test was attempted, for the author screens that manage the practice library and the placement tests.
The route belongs to Assessment because it owns the attempts; the screens read Content for the tests and this route for
their numbers, so no service calls the other.

`GET /api/assessments/admin/package-stats?packageIds=<uuid>&packageIds=<uuid>` (`CONTENT_AUTHOR`).

- `packageIds` are the Content package ids (not version ids). At most 100 distinct ids; more answers `400` with a
  Vietnamese message. A missing or empty list answers `200` with `[]`.
- The numbers cover every version of the package and all time.

```json
[ { "packageId": "<uuid>", "started": 12, "submitted": 9, "averageBand": 6.5 } ]
```

- `started`: attempts begun, in any status.
- `submitted`: attempts with status `SUBMITTED`.
- `averageBand`: the average overall band of the latest `COMPLETED` result of each attempt, one decimal; null when no
  attempt has a graded result. A regrade replaces the earlier result of the same attempt.
- A package without any attempt is not in the list; the caller shows zero.
- Learners and anonymous callers are refused (`403` and `401`).
