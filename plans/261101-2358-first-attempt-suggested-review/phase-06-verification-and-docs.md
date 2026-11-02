---
phase: 6
title: "Verification and docs"
status: pending
priority: P2
effort: "1h"
dependencies: [1, 2, 3, 4, 5]
---

# Phase 6: Verification and docs

Work package D (Codex) plus a manual check (Claude/user, not Codex). Repo: backend.

## Codex: contract doc

`backend/docs/contracts/lesson-learning-v1.md`:

1. In the practice submission section (find "reviewsCreated"), document each entry as
   `{reviewId, knowledgePointId, stage, blocking}` and add: "`blocking` is `false` for a suggested review from a first
   counted failure; the next lesson stays open. A knowledge point that is weak again in another counted set while its
   suggestion (or an `ERROR_PATTERN` review) is pending has that review promoted; it is listed with `blocking: true`.
   A knowledge point counts as weak with at least 2 wrong answers in the set and under 70%."
2. In the practice clearance paragraph (line ~192, "Practice clearance is one-way: ..."), append: "After those reasons,
   a counted failed attempt clears the Practice with `ATTEMPT_NOT_BLOCKED` when no blocking review of the skill is
   pending. This reason is derived on every read and never stored, so it goes away if a review of the skill becomes
   blocking, until that review is finished."
3. In the review rules (lines ~233 and ~270, "Optional reviews end `SKIPPED` on a failed set" / "An optional review is
   one set..."), add: "Exception: a suggested review from Practice (optional, trigger `PRACTICE`) can start at the
   theory stage, and a failed set makes it blocking at the theory stage instead of `SKIPPED`."

Do not re-run documentation build scripts.

## Manual check (not Codex)

On the running stack with a lesson whose practice sets map 2+ questions to one KP and have a spare set:
fail once (suggestion, no banner, next lesson open), fail the same KP again (blocking, banner), fail the suggested
review set (blocking at theory), retake a revealed set (nothing changes), and confirm the suggestion shows in the
review basket without a duplicate.

## Pass-through metric (manual, after a week live)

```sql
SELECT count(*) FILTER (WHERE NOT EXISTS (
           SELECT 1 FROM review_items r WHERE r.source_attempt_id = a.id AND r.blocking))::float
       / NULLIF(count(*), 0) AS soft_share
FROM practice_attempts a
WHERE a.origin = 'LESSON' AND a.counted_as_evidence AND a.submitted_at IS NOT NULL AND a.passed = FALSE
  AND a.submitted_at > now() - interval '7 days';
```

Revisit the gate if `soft_share` is above 0.8.

## Success Criteria

- [ ] Contract updated as written; whole learning-service suite and frontend suites green (packages A-C).
- [ ] Manual check recorded in this file by Claude after merge.
