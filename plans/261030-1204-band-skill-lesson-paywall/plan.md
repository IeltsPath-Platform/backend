---
title: "Band-skill lesson paywall: 3 free lessons, unlock by Points"
description: "First 3 lessons per (band, skill) free; unlock a skill or a band combo with Points (lifetime); Premium opens all"
status: pending
priority: P2
branch: ""
tags: [paywall, points, learning, access, content, tdd]
blockedBy: [261030-1200-course-cover-description]
blocks: []
created: "2026-10-30T05:21:49.791Z"
createdBy: "ck:plan"
source: skill
---

# Band-skill lesson paywall: 3 free lessons, unlock by Points

Source: [brainstorm report](../reports/brainstorm-261030-1204-band-skill-lesson-paywall-report.md). Mode: `--tdd` (each phase writes failing tests first).

## Overview

A lesson opens when any of: Premium is active; it is one of the first 3 lessons of its (band, skill) in learning order; the learner unlocked that skill of that band; the learner unlocked the band combo. Unlocks are bought with Points (existing wallet), lifetime. Premium-only topics (`required_feature_key`) stay Premium-only. Existing "pass previous lesson first" rules stay.

Price is computed server-side. Combo price = `combo_unlock_points` minus Points already paid for that band's skills (min 0). Combo covers every skill of the band, including skills added later.

Design rules added by the red-team review:
- Content-service computes `skillPosition`, `primarySkill`, `premiumOnly` per lesson; learning-service only decides.
- A purchase is recorded as a durable PENDING row before the debit and completed by replaying the same debit, so Points are never lost.
- `premium()` is called last and only when needed; an Access outage never blocks free, owned or grandfathered lessons.
- Lessons a learner already started before go-live stay open (migration sets `lesson_progress.grandfathered`). Topics without a course are not gated; Premium-only topics are checked first and stay Premium-only. Practice and review of completed lessons stay allowed, re-reading them after Premium expires does not.

## Phases

| Phase | Name | Depends on | Status |
|-------|------|------------|--------|
| 1 | [Access unlock debit](./phase-01-access-unlock-debit.md) | - | Pending |
| 2 | [Content unlock prices and lesson paywall facts](./phase-02-content-unlock-prices.md) | cover-description plan | Pending |
| 3 | [Learning unlock purchase](./phase-03-learning-unlock-purchase.md) | 1, 2 | Pending |
| 4 | [Learning lesson paywall gate](./phase-04-learning-lesson-paywall-gate.md) | 2, 3 | Pending |
| 5 | [Frontend paywall and admin prices](./phase-05-frontend-paywall-and-admin-prices.md) | 3, 4 | Pending |
| 6 | [Content audit and verification](./phase-06-content-audit-and-verification.md) | 1-5 | Pending |

## Dependencies

- `261030-1200-course-cover-description` edits the same course DTOs, `CourseForm` and learner course mapping, and claims migration V86 in content-service. This plan uses V87. Only phase 1 (access-service) can start before it finishes; phase 2 waits for it, phases 3-6 follow phase 2.

## Acceptance criteria

- Free learner opens only the first 3 lessons of each (band, skill); every other lesson is refused by the API with code `PAYWALL`, not just hidden in UI.
- Skill or combo purchase opens the rest; thiếu Point is rejected; Premium opens all; Premium-only topics need Premium.
- Same `Idempotency-Key` never debits twice; combo upgrade price follows the deduction rule.
- UI tells "cần mở khóa" apart from "chưa học xong bài trước".
- Backend tests (access, content, learning) and FE tests of touched files pass.
- A crash between debit and activation never loses Points; Access being down never blocks free, owned or grandfathered lessons.

## Out of scope

Direct course keys, refund of unlocks, gating practice sets / mock tests / grading per skill, Speaking content, per-lesson free flags, mobile.

## Unresolved questions

- Point price per skill and per combo, and Point cost of one AI grading (price scale). Set in phase 6 after the audit.
- None blocking. The three earlier open questions were settled in Validation Session 1 (below). Price values stay a phase 6 task.

## Red Team Review

### Session — 2026-10-30
**Findings:** 11 (9 accepted, 2 rejected)
**Severity breakdown:** 1 Critical, 4 High, 6 Medium (the 2 rejected are Medium)

| # | Finding | Severity | Disposition | Applied To |
|---|---------|----------|-------------|------------|
| 1 | Debit then activation gap can lose Points | Critical | Accept | Phase 3 |
| 2 | `premium()` on the hot path breaks free lessons when Access is down | High | Accept | Phase 4 |
| 3 | Learners mid-course locked after deploy / order drift | High | Accept | Phase 4, plan.md |
| 4 | Lesson entry-point list incomplete | High | Accept | Phase 4 |
| 5 | Position computed in learning-service is heavy and duplicates ordering | High | Accept | Phase 2, 4 |
| 6 | Phase 3 cannot start before cover plan (dependency text wrong) | Medium | Accept | plan.md |
| 7 | No per-course list of skills that have lessons | Medium | Accept | Phase 2, 3 |
| 8 | Topic without a course has no rule | Medium | Accept | Phase 4 |
| 9 | Client-side price math duplicates the server | Medium | Accept | Phase 5 |
| R1 | Replay another user's Idempotency-Key | Medium | Reject: `DebitPointsUseCase.java:31-33,51-57` binds actor, user, type, amount, reference | - |
| R2 | Learner reads lessons straight from content-service | Medium | Reject: only `Internal*` and authoring lesson controllers exist in content-service | - |

### Whole-Plan Consistency Sweep (red team)

Decision delta: (a) content computes positions, (b) PENDING-then-ACTIVE purchase, (c) premium last + grandfather + no-course rule, (d) no FE price math, (e) only phase 1 starts before the cover plan.
Searched `plan.md` and phases 1-6 for the superseded terms `LessonPositions`, `topic.lessons`, `lessons[]`, `unlockPricing`, "Phases 1 and 3 can start", "premium() per request". Remaining hits: none. Phase 1 (access-service) and phase 6 needed no structural change; phase 6 scenarios were extended. Unresolved contradictions: none.

## Validation Log

### Validation Session 1 — 2026-10-30

Mode: prompt. Red Team Review already carried codebase evidence, so the full verification pass was limited to the claims behind the open questions.

### Verification Results
- Claims checked: 3
- Verified: 2 | Failed: 1 | Unverified: 0
- Tier: Full (scoped to the open questions)
- Failures: `lesson_progress` has no `created_at` (only `completed_at`, `updated_at`): `learning-service/.../V1__learning_schema.sql:18-28`. A column added now would be backfilled at migration time, so a date cutoff is unreliable. Resolved by decision 3.
- Verified: multi-skill topic exists (`content-service/.../V23__seed_multi_skill_topic.sql:7`, topic without skill in course 5.5); Premium-only topics are READING topics of course 5.5 (`V15`, `V21:4`).

### Decisions

| # | Question | Decision | Applied To |
|---|----------|----------|------------|
| 1 | Multi-skill lessons | Primary skill = `topic.skill`, else first of LISTENING, READING, WRITING, SPEAKING; position counts in that list; owning any skill the lesson teaches opens it | Phase 4 |
| 2 | Existing Premium-only topics | Stay Premium-only, not unlockable with Points, excluded from positions; Premium check runs before the no-course rule | Phase 2, 4 |
| 3 | Grandfathering | Migration `V39` adds `lesson_progress.grandfathered`, true for all existing rows; no date config | Phase 4, 6 |
| 4 | After Premium expires | Lessons beyond free/owned/grandfathered lock again, re-reading included; practice and review of completed lessons stay allowed | Phase 4 |

### Whole-Plan Consistency Sweep

Searched `plan.md` and phases 1-6 for the superseded terms `grandfather-before`, `created_at` (for progress), "Confirm.", "default is", and `courseId == null` ordering. Remaining hits after edits: none. Phase 4 rule order, Phase 6 scenarios and the plan.md overview now agree on the migration-flag grandfathering. Unresolved contradictions: none.
