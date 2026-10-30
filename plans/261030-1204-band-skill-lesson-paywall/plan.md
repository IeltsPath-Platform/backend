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

A lesson opens when any of: Premium is active; it is one of the first 3 lessons of its (band, skill) in learning order; the learner unlocked that skill of that band; the learner unlocked the band combo. Unlocks are bought with Points (existing wallet), lifetime. Courses have no Premium-only topics (the topic gate is removed); every course topic follows this one rule. Each topic has exactly one skill and its lessons teach only that skill. Existing "pass previous lesson first" rules stay, but topic progression becomes per skill: the four skills of a band are independent and sequential only inside one skill (phase 7). Free practice by knowledge point follows the same paywall, and the knowledge tree marks locked points.

Prices are per skill and per course (different skills and courses can cost differently) and are edited by R&D in the admin course drawer at any time; nothing is hard-coded or seeded. Price is computed server-side. Combo price = `combo_unlock_points` minus Points already paid for that band's skills (min 0). Combo covers every skill of the band, including skills added later.

Design rules added by the red-team review:
- Content-service computes `skill` and `skillPosition` per lesson (and `earliestLessonPosition` per knowledge point); learning-service only decides.
- A purchase is recorded as a durable PENDING row before the debit and completed by replaying the same debit, so Points are never lost.
- `premium()` is called last and only when needed; an Access outage never blocks free, owned or grandfathered lessons.
- Lessons a learner already started before go-live stay open (migration sets `lesson_progress.grandfathered`). Topics without a course are not gated. Practice and review of completed lessons stay allowed, re-reading them after Premium expires does not.

## Phases

| Phase | Name | Depends on | Status |
|-------|------|------------|--------|
| 1 | [Access unlock debit](./phase-01-access-unlock-debit.md) | - | Pending |
| 2 | [Content unlock prices and lesson paywall facts](./phase-02-content-unlock-prices.md) | cover-description plan | Pending |
| 3 | [Learning unlock purchase](./phase-03-learning-unlock-purchase.md) | 1, 2 | Pending |
| 7 | [Per-skill topic progression](./phase-07-per-skill-topic-progression.md) (runs before phase 4; numbered last to keep references stable) | - | Pending |
| 4 | [Learning lesson paywall gate](./phase-04-learning-lesson-paywall-gate.md) | 2, 3, 7 | Pending |
| 5 | [Frontend paywall and admin prices](./phase-05-frontend-paywall-and-admin-prices.md) | 3, 4 | Pending |
| 6 | [Content audit and verification](./phase-06-content-audit-and-verification.md) | 1-5, 7 | Pending |

## Working notes for the implementer

- Read `backend/AGENTS.md` first (conventions, layering, test rules). Paths in the phases are relative to `backend/services/<service>/src/main` unless a phase says otherwise; frontend paths are under `frontend/src`.
- Run one service: `mvn -B -ntp -pl services/<service> -am test` (access-service, content-service, learning-service). CI runs `mvn -B -ntp verify` for the whole reactor. Integration tests use Testcontainers PostgreSQL, so Docker must be running.
- Migration numbers: access `V5`, content `V86` (cover plan), `V87`, `V88`, learning `V38`, `V39`. Re-check the latest number in each `db/migration` folder before creating a file; the other plan or a merge may have taken one.
- TDD: each phase lists the tests to write first. Do not weaken or delete an existing test; if one fails because behaviour intentionally changed, say so in the PR.
- Names in migrations, tests and comments describe behaviour, not phases or plan ids.
- Every line number cited in the phases was checked against the code on 2026-10-30; if the code has moved, find the method by name.

## Dependencies

- `261030-1200-course-cover-description` edits the same course DTOs, `CourseForm` and learner course mapping, and claims migration V86 in content-service. This plan uses V87. Only phase 1 (access-service) and phase 7 (learning-service progression, touches no course DTO) can start before it finishes; phase 2 waits for it, phases 3-6 follow phase 2, and phase 4 also needs phase 7.

## Acceptance criteria

- Free learner opens only the first 3 lessons of each (band, skill); every other lesson is refused by the API with code `PAYWALL`, not just hidden in UI.
- Skill or combo purchase opens the rest; thiếu Point is rejected; Premium opens all. No course topic is Premium-only.
- Every course topic has one skill and every lesson in it teaches only that skill; the content audit lists any existing violation.
- R&D sets, changes and clears the Points price of each skill of each course, and the combo price, from admin without a deploy; the first price set follows the phase 6 pricing guide (skill below Premium 30 days, combo between Premium 30 and 90 days, combo cheaper than all skills together).
- Same `Idempotency-Key` never debits twice; combo upgrade price follows the deduction rule.
- A learner can have an open topic in each of the four skills at once; finishing one skill's topic is never needed to open another skill.
- Free practice on a knowledge point of a locked lesson is refused with `PAYWALL` and spends no hearts; the knowledge tree marks those points `locked`.
- Learners mid-course keep every topic that is in progress when this ships.
- UI tells "cần mở khóa" apart from "chưa học xong bài trước".
- Backend tests (access, content, learning) and FE tests of touched files pass.
- A crash between debit and activation never loses Points; Access being down never blocks free, owned or grandfathered lessons.

## Out of scope

Direct course keys, refund of unlocks, gating mock tests / assessments / grading per skill, Speaking content, per-lesson free flags, mobile. Free practice by knowledge point is gated (phase 4); lesson practice sets and review of completed lessons are not.

## Unresolved questions

- The actual Point prices per skill per course and for each combo, the combo discount, and the Point cost of one AI grading. Set with the product owner in phase 6 following the pricing guide, then entered by R&D. The money prices of the Premium keys and Point keys are not known yet and are read from access-service in phase 6.
- None blocking. The three earlier open questions were settled in Validation Session 1 (below). Price values stay a phase 6 task.
- Existing data that breaks "one skill per topic": the topic without a skill seeded by `V23` in course 5.5 and any multi-skill lesson. Phase 2 lists them in an audit; the content owner decides how each is split or assigned a skill (not decided yet, and phase 7 must not ship before it is fixed).
- Free learners cannot take a topic test while any lesson of the topic is paywalled (`AssignTopicTestUseCase` needs every lesson completed), so they never finish such a topic or the course. Accepted as expected; UI copy should say so.

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

### Validation Session 2 — 2026-10-30

Mode: prompt. Triggered by reading the mastery, tree, report and assessment code against the plan. Verified in code: `TopicStatusDeriver` opens one topic per course across all skills, `LessonAccess` raises `TOPIC_LOCKED` from it before any paywall rule, `AssignTopicTestUseCase` needs every lesson of the topic completed, `StartFreePracticeUseCase` checks only course open/joined, `GetLearnerTreeUseCase` returns every point of the course, `GetLearnerReportUseCase` lists only points with a stored profile, `ApplyAssessmentResultUseCase` has no paywall check.

| # | Question | Decision | Applied To |
|---|----------|----------|------------|
| 5 | Topic order across skills | The four skills are independent; sequential only inside one skill; each skill opens its own first topic | Phase 7 (new), Phase 4 |
| 6 | Learners mid-course at go-live | Keep their place; status is derived, so no data migration, and current in-progress topics stay in progress | Phase 7 |
| 7 | Free practice by knowledge point | Gate it like lessons, using content-computed `earliestLessonPosition` | Phase 2, 4 |
| 8 | Tree and report | Add `locked` to the tree `Node`; the report needs no flag (lists only points with evidence); assessments stay ungated by design | Phase 4, 5 |

Mastery itself is unchanged: formulas and `kp_mastery_profiles` are not touched; free learners only produce less evidence.

### Whole-Plan Consistency Sweep (session 2)

Searched `plan.md` and phases 1-7 for "one open topic", "each (band, skill)" without the progression rule, and "gating practice sets". Phase 4 gate table, phase 5 FE scope, phase 6 scenarios and the plan overview now agree. Remaining unresolved: data that violates one skill per topic (listed above). Contradictions: none known after Session 3.

### Validation Session 3 — 2026-10-30

Mode: prompt. Two user decisions reverse validated decisions 1 and 2 of Session 1; Session 1 text above is kept as history.

| # | Question | Decision | Applied To |
|---|----------|----------|------------|
| 9 | Premium-only topics (supersedes 2) | Removed. No course topic is Premium-only: `topics.required_feature_key` is cleared by migration and no longer accepted. Every topic follows the one paywall; Points can unlock all of it. Practice-set, package and question keys are unchanged | Phase 2, 4, 5, 6 |
| 10 | Multi-skill lessons (supersedes 1) | One skill per topic, and every lesson in it teaches only that skill. Primary-skill ordering, multi-skill ownership rule and the no-skill group are dropped. Existing violations are audited and fixed by the content owner | Phase 2, 4, 7 |
| 11 | Free learner and topic test | Confirmed: free learners cannot reach the topic test of a topic with locked lessons, and never finish the course | Phase 7 |

### Validation Session 4 — 2026-10-30

Mode: prompt.

| # | Question | Decision | Applied To |
|---|----------|----------|------------|
| 12 | Premium scope | Premium keeps opening every lesson while active; kept | Phase 4 |
| 13 | Who sets prices | R&D, in admin, per skill per course plus a combo per course; no seeded or hard-coded prices. Storage becomes `course_skill_prices` instead of one skill price column | Phase 2, 3, 5, 6 |
| 14 | Price reasonableness | Code rejects nonsense (combo below the highest skill or above the sum of skills); phase 6 pricing guide ties prices to Premium 30/90-day prices, price per locked lesson and AI grading cost | Phase 2, 6 |

### Whole-Plan Consistency Sweep (session 4)

Searched `plan.md` and phases 1-7 for `skill_unlock_points`, `skillUnlockPoints` as a single value, "two price", "seed migration", "combo >= skill". Remaining hits: Session 1-3 history only. Unresolved contradictions: none.

### Whole-Plan Consistency Sweep (session 3)

Searched `plan.md` and phases 1-7 for `premiumOnly`, "Premium-only", "primary skill", "multi-skill", "non-premium-only". Remaining hits: Session 1 and Session 2 history above, the red-team table, and the phase 2 audit text that names the old seed. Phase 2 (payloads, validation, V87/V88), phase 4 (five-rule order), phase 7 (group key = topic skill) and the plan overview now agree. Unresolved contradictions: none.

### Whole-Plan Consistency Sweep

Searched `plan.md` and phases 1-6 for the superseded terms `grandfather-before`, `created_at` (for progress), "Confirm.", "default is", and `courseId == null` ordering. Remaining hits after edits: none. Phase 4 rule order, Phase 6 scenarios and the plan.md overview now agree on the migration-flag grandfathering. Unresolved contradictions: none.
