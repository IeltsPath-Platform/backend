# Progress snapshots v1

Learning Service writes a snapshot of each recently active learner every week and every month, and serves the weekly
report, the weekly history and the monthly report from them. There is no email and no PDF: this is data for in-app screens.
FE: "Your week" (arrows per skill, focus points, next-week suggestions) and the monthly screen with trend charts.

All routes are under `/api/learning`, role `CUSTOMER`, and show the verified caller's own data only. Staff read the latest week
of a learner in the staff overview (see [learning-staff-area-v1](learning-staff-area-v1.md)).

## What a snapshot holds

| Field | Meaning |
| --- | --- |
| `skills` | Mastery per block: `LISTENING`, `READING`, `WRITING`, `SPEAKING`, `GRAMMAR`, `VOCABULARY`. Each block has `mastery` (mean over judged points weighted by evidence, `null` if none is judged), `tierCounts`, `kpCount`. |
| `tierCounts` | Knowledge points per tier: `PRIORITY`, `CONSOLIDATE`, `MAINTAIN`, `INSUFFICIENT`. |
| `answered`, `correct` | Questions answered in the period and how many were right. |
| `activeMinutes` | Sum of the time spent on each answer, each capped at 10 minutes so a question left open does not inflate it. |
| `focusKnowledgePointIds` | The three weakest points still being built (`PRIORITY` or `CONSOLIDATE`), lowest mastery first. |
| `estimatedBand` | The estimated overall band (see the band rules in [learner-report-v1](learner-report-v1.md)), or `null`. An estimate, not an exam result. |

A snapshot takes the learner's mastery as it stands when the run happens (Monday 00:30 for the week before, the first of the
month 00:30 for the month before, in `learning.snapshots.zone`, default `Asia/Ho_Chi_Minh`) and counts the answers of the
period. The run has no learner request, so it reads each point's skill and course band from Learning's own catalog (filled
whenever a learner opens the course list), not from Content; until a learner has opened the list after a fresh database or
migration, a snapshot carries no skill or band, and the next run replaces it. Only learners with an answer in the last 14
days get one. A rerun replaces a snapshot, never duplicates it. Weekly
snapshots are kept 12 weeks and monthly ones 36 months. The monthly snapshot is built from the month's own data, not from the
weekly ones, because weeks cross month ends.

## `GET /report/weekly?weekStart=2026-10-12`

`weekStart` must be a Monday (else `400 INVALID_WEEK_START`); without it, the week before the current one.

```json
{ "weekStart": "2026-10-12",
  "snapshot": { "skills": [{ "skill": "READING", "mastery": 0.72, "tierCounts": { "PRIORITY": 0, "CONSOLIDATE": 3,
                 "MAINTAIN": 2, "INSUFFICIENT": 1 }, "kpCount": 6 }],
                "tierCounts": { "PRIORITY": 1, "CONSOLIDATE": 4, "MAINTAIN": 3, "INSUFFICIENT": 2 },
                "answered": 84, "correct": 61, "accuracy": 0.726, "activeMinutes": 190, "estimatedBand": 6.0,
                "focusKnowledgePointIds": ["…", "…", "…"] },
  "changes": [{ "skill": "READING", "mastery": 0.72, "previousMastery": 0.60, "direction": "UP" }],
  "nextWeek": { "…": "same body as GET /api/learning/suggestions" } }
```

- `snapshot` is `null` when the learner has none for that week (they were not active); `changes` is then empty.
- `changes.direction`: `UP` or `DOWN` when mastery moved by 0.02 or more since the week before, `SAME` when it moved less,
  `NEW` when there is no earlier week to compare, `NONE` when the block has no judged point.
- `accuracy` is `correct / answered`, `null` when nothing was answered.
- `nextWeek` is the suggestions body of `GET /api/learning/suggestions` as of the request.

## `GET /report/history?weeks=4`

`weeks` 4 to 12 (default 4), else `400 INVALID_WEEKS`. The weekly snapshots of the last `weeks` weeks that exist, oldest first:

```json
{ "weeks": [{ "weekStart": "2026-09-28", "estimatedBand": 5.5, "accuracy": 0.70, "activeMinutes": 160,
              "mastery": { "READING": 0.60, "LISTENING": 0.55, "WRITING": null, "SPEAKING": null,
                           "GRAMMAR": null, "VOCABULARY": null } }] }
```

## `GET /report/monthly?monthStart=2026-09-01`

`monthStart` must be the first of a month (else `400 INVALID_MONTH_START`); without it, the month before the current one.
Response `{ "monthStart": "2026-09-01", "snapshot": { … } }` with the same snapshot body; `snapshot` is `null` if there is none.

## Projected date of the target band

`GET /api/learning/report` has `target.projection` (see [learner-report-v1](learner-report-v1.md)): a straight line fitted to
the estimated band of the four most recent weeks. With fewer than four weeks that have a band the projection is
`{ "date": null, "reason": "NOT_ENOUGH_DATA" }`; with a line that does not rise `{ "date": null, "reason": "NO_PROGRESS" }`.
If the fitted band is already at the target, the date is the latest week. A projection is a guide, not a promise.
