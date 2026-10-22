# Learner report and mastery rules v1

Learning Service keeps one mastery profile per learner and knowledge point (KP). The report API reads those profiles; the
rule API lets Admin change the thresholds and the review schedule. FE: the learner report screen (colour by tier, common
errors, next action), the staff learner profile, and the Admin rule screen.

## Mastery profile

A profile exists for every KP that has evidence for the learner. It is written inside the transaction that wrote the
evidence, for only the KPs that transaction touched: lesson exercise, Practice, review set, lesson essay and assessment
result. A retake adds events but no evidence, so it changes no profile.

| Field | Meaning |
| --- | --- |
| `mastery` | Score 0–1 with three decimals. Under `DEEPTUTOR`: the recency-weighted calculator (last five outcomes). Under `MP1`: the weighted share of the newest valid evidence, and it falls as evidence ages. |
| `tier` | `PRIORITY` below `priority-below`, `CONSOLIDATE` below `maintain-from`, otherwise `MAINTAIN`. The tier is cut on the stored three-decimal value. Under `MP1` a point with too little evidence has `INSUFFICIENT` ("chưa đủ dữ liệu", no review, not in the basket), and a judged point changes tier only past the threshold by 0.03. |
| `success_streak` | Grows by one when the newest evidence is right and the tier did not fall; resets to 0 when it is wrong; unchanged when it is right but the tier fell. |
| `next_review_at` | `PRIORITY`: none (a blocking review covers it). `CONSOLIDATE`: last evidence + `consolidate-days`. `MAINTAIN`: last evidence + `review-intervals-days[min(streak, last)]`. |
| `median_duration_ms`, `top_error_codes` | From the question event log of the last 90 days: median time and the three most frequent error codes. |

The review schedule is a default convention, not tuned on learner data. Admin can change it (below).

A regraded assessment result rebuilds the affected profiles from the remaining evidence: the streak becomes the run of
right answers at the end of the recent history, and a KP left without evidence loses its profile. Profiles for evidence
older than this feature are built once, in the background, at startup.

## `GET /api/learning/report`

Role `CUSTOMER`. Always the verified caller's own report; there is no user parameter.

`GET /api/learning/admin/learners/{userId}/report` — role `EXAMINER`; the same body for any learner.

```json
{
  "userId": "…", "generatedAt": "2026-10-16T10:00:00Z",
  "knowledgePoints": [{
    "knowledgeId": "…", "code": "R60_TFNG_F_VS_NG", "name": "…", "skill": "READING", "band": 6.0,
    "score":    { "mastery": 0.72, "tier": "CONSOLIDATE", "evidenceCount": 9 },
    "errors":   [{ "code": "TFNG_NG_AS_FALSE", "count": 3 }],
    "evidence": [{ "questionVersionId": "…", "correct": false, "durationMs": 41000,
                   "source": "practice_set", "errorCode": "TFNG_NG_AS_FALSE", "at": "…" }],
    "action":   { "type": "REVIEW_REQUIRED", "reviewId": "…", "dueAt": null }
  }]
}
```

- One entry per KP that has a profile, weakest first (tier, then score, then code).
- `code`, `name`, `skill`, `band` come from Content's topic sequence; they are `null` when Content does not list the KP.
  `band` is the band of the KP's course.
- `errors`: the three most frequent error codes of the last 90 days with their counts.
- `evidence`: the five newest question events of the KP (`durationMs` and `errorCode` may be null; `correct` is null for
  an ungraded item). An essay event also has `revisionNo` and, for a revised essay, `revisionDelta` (see
  [essay-revision-v1](essay-revision-v1.md)); the two fields are absent on other events.
- `action.type`:

  | Type | When |
  | --- | --- |
  | `REVIEW_REQUIRED` | A blocking review is pending for the KP. `reviewId` is set. |
  | `CONSOLIDATE` | Tier `CONSOLIDATE` and no pending review. `dueAt` is `next_review_at`. |
  | `MAINTAIN_DUE` | Tier `MAINTAIN` and `next_review_at` has passed. |
  | `NONE` | Anything else. `dueAt` is `next_review_at` when there is one. |

  `DIAGNOSING` is reserved for the backtracking diagnostic and is not returned yet.

`GET /api/learning/mastery` is unchanged.

## Skill view, focus, estimated band and target

The report body also carries four blocks added after `knowledgePoints`; the fields above are unchanged. They are on both
`GET /api/learning/report` and the staff route.

```json
{ "skills": [{ "skill": "READING", "mastery": 0.72, "tierCounts": { "PRIORITY": 0, "CONSOLIDATE": 3, "MAINTAIN": 2,
                "INSUFFICIENT": 1 }, "kpCount": 6, "band": 6.0 }],
  "focus": [{ "knowledgeId": "…", "code": "R60_TFNG_F_VS_NG", "name": "…", "skill": "READING", "mastery": 0.31,
              "tier": "PRIORITY", "reviewRequired": true }],
  "estimatedBand": { "value": 6.0, "estimated": true, "speakingIncluded": false,
                     "nextBandHints": [{ "skill": "READING", "nextBand": 6.5, "knowledgeIds": ["…"] }] },
  "target": { "targetBand": 7.0, "gap": 1.0, "examDate": "2027-01-20",
              "projection": { "date": "2027-02-08", "reason": null } } }
```

- `skills`: always six blocks in the order `LISTENING`, `READING`, `WRITING`, `SPEAKING`, `GRAMMAR`, `VOCABULARY`. `mastery` is
  the mean over the block's judged points weighted by their evidence count, and is `null` when no point is judged
  (`INSUFFICIENT` points are counted in `tierCounts` but not in the mean). `band` is the block's estimated band, `null` for
  `GRAMMAR`, `VOCABULARY` and for a skill with no band.
- `focus`: at most three points in `PRIORITY` or `CONSOLIDATE`; a point with a blocking review (`reviewRequired`) comes first,
  then the lowest mastery.
- `estimatedBand`: always marked `estimated: true`; it is an estimate, not an exam result.
  - A skill reaches a band when at least 80% of its points of that band are `MAINTAIN`; the skill's band is the highest such
    band. When it reaches none, the placement band stands in, but only for a skill that has points; a skill with no points has no
    band (Speaking has none for now).
  - `value` is the mean of the skill bands rounded to a half band, `null` when no skill has one. Grammar and vocabulary do not
    enter it. `speakingIncluded` is true only when Speaking has a band.
  - `nextBandHints`: for a skill whose next band has at least 80% of its points at `CONSOLIDATE` or better, up to five points
    still to maintain there, weakest first.
- `target`: set only when User Service returned a goal (`GET /internal/users/{id}/learning-goal`); otherwise `null` and the
  report is otherwise complete (a slow or failing User Service never fails the report). `gap` is the target minus `estimatedBand.value` (`null` if either is unknown).
  `projection` is described in [progress-snapshots-v1](progress-snapshots-v1.md) and is `null` without a target band.

## Mastery rules (Content Author)

Role `CONTENT_AUTHOR` for all three routes.

| Route | Effect |
| --- | --- |
| `GET /api/learning/admin/rule-configs` | The rules in force and the keys the content author has overridden. |
| `PUT /api/learning/admin/rule-configs` | Changes any subset. A missing field keeps its value. |
| `POST /api/learning/admin/rule-configs/recompute` | Re-tiers every stored profile in the background. `202 {"status":"STARTED"}`, or `409 {"status":"ALREADY_RUNNING"}`. |

```json
{ "priorityBelow": 0.6, "maintainFrom": 0.8, "reviewIntervalsDays": [7, 14, 30, 45, 60], "consolidateDays": 3,
  "overridden": ["mastery.priority-below"] }
```

- Validation, on the rules as a whole: `0 < priorityBelow < maintainFrom <= 1`; `reviewIntervalsDays` is not empty,
  starts at one day or more and strictly increases; `consolidateDays >= 1`. A failure is `422 INVALID_RULE_CONFIG` with
  the reason, and nothing is stored.
- Storage keys (`learning_rule_configs`): `mastery.priority-below`, `mastery.maintain-from`,
  `mastery.review-intervals-days`, `review.consolidate-days`. A stored value wins over the service configuration
  `learning.mastery.*`; without a row the configuration applies. `learning.review-mastery-threshold` is the older name of
  `learning.mastery.priority-below` and is its default.
- A change takes effect for profiles written afterwards. `recompute` moves the tier and review date of the stored ones and
  never touches the score or the evidence under `DEEPTUTOR`. Under `MP1` it rebuilds each score from the evidence, because
  the formula decides the score. A profile that new evidence changed while the job ran is skipped, because it is
  already on the new rules.
- `priorityBelow` is also the line below which a wrong answer creates a blocking review, so changing it changes which
  reviews are created from then on. Existing reviews are not touched.
- A stored set that no longer validates as a whole is ignored and the service configuration applies.
