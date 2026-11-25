---
title: 'Placement test pools, random no-repeat assignment, retake and band ceiling'
description: >-
  Three placement exam pools (0-3, 3-5, 5-9) the learner picks from, random exam
  per request without repeats, retake up to 3 times per rolling 24 hours, band
  capped at the pool ceiling, Speaking and ungradable Writing left out of the
  average
status: pending
priority: P2
branch: main
tags:
  - placement
  - content-service
  - assessment-service
  - learning-service
  - frontend
  - tdd
blockedBy: []
blocks: []
created: '2026-11-17T14:03:55.207Z'
createdBy: 'ck:plan'
source: skill
---

# Placement test pools, random no-repeat assignment, retake and band ceiling

Source: [brainstorm report](../reports/brainstorm-261117-2043-placement-test-pools-random-retake-report.md). Mode: `--tdd` (each phase writes its failing tests first). This plan is **workstream 1** of the report; Speaking AI for placement (workstream 2) is a separate plan.

## Overview

Today there is one placement exam (`PLACEMENT-4SKILLS`), `GetPlacementTestUseCase` returns `packages.getFirst()`, and a learner who already has a placement gets `409 PLACEMENT_ALREADY_DONE`. Operations want an exam pool per band range and to let learners retake at once without meeting the same exam.

What changes for the learner: they pick one of three pools, get a random exam of that pool they have not had (the one given longest ago once all are used), may take it up to 3 times per rolling 24 hours, and the band they receive never exceeds the pool's ceiling (3.0 / 5.0 / 9.0), with a hint to try a higher pool when they reach it.

## Scope decisions (brainstorm, validation and red team, all confirmed by the user)

| Decision | Value |
| --- | --- |
| Pools | `BAND_0_3`, `BAND_3_5`, `BAND_5_9`; learner picks; ceilings 3.0 / 5.0 / 9.0 |
| Pick rule | Random among the pool's published exams not yet assigned to the learner; all used: the one assigned longest ago |
| Used exams | Every assignment counts, abandoned ones included, except an assignment the learner releases because the attempt could not be started |
| Retake limit | Up to 3 assignments per **rolling 24 hours** in Learning, plus a hard backstop of 3 placement attempts per rolling 24 hours in Assessment (Assessment accepts any version id, so Learning alone cannot enforce it). The no-repeat rule stays advisory (Learning only) |
| Stored band | The latest completed result replaces it (existing `LearnerPlacement.record`). The pool picker warns when the pool's ceiling is below the learner's current band |
| Band | Computed as today, then `min(band, pool ceiling)`; no floor. The result flags `reachedCeiling` (computed band at or above the ceiling, pool not the highest) so the client suggests a higher pool |
| Speaking | Left out of the average while it has no graded band (no more default 0.0). An explicitly configured `assessment.placement.speaking-band` keeps working |
| Writing | Essay not sent: stays 0.0. Essay sent but not gradable (LLM unavailable, daily quota, any grading error): Writing is left out of the average and the result is `partial` (no more default 5.5) |
| Seeded exam | The existing exam is migrated to `BAND_5_9` (ceiling 9.0, so nothing changes for it). Phase 6 seeds five new exams by migration, **published on deploy** (user decision): two shortened for `BAND_0_3`, two shortened for `BAND_3_5`, one full-length for `BAND_5_9`, so each pool opens with two exams. Empty pools stay disabled in the picker if a pool is ever empty |
| Seed content gates | Because publishing on deploy leaves no later review, an IELTS-qualified reviewer signs off every exam before the migrations are merged, and the 12 Listening files (recorded by the user from scripts this plan produces) are uploaded and checked before the release |
| Answer review | Per-question answers on the placement result stay. With two exams per pool a third retake within 24 hours repeats the oldest exam and a repeated exam can be memorised; a third exam per pool is recommended later |
| Author view | No separate page. The existing admin Packages page gets a "Kho" column, a pool filter and, under the placement type filter, a summary of published exams per pool with a warning below 2 (user chose this light option over a dedicated page) |
| Dropped (YAGNI) | Publish-time minimum-exams check (the summary strip on the Packages page replaces it); admin endpoint to change a package's pool after creation; dedicated placement management page with audio status and band distribution per pool (revisit once real learner data exists) |
| Out of scope | Speaking AI grading for placement, per-pool conversion tables, adaptive testing |

## Phases

| Phase | Name | Status |
| --- | --- | --- |
| 1 | [Content placement pools](./phase-01-content-placement-pools.md) | Pending |
| 2 | [Assessment band ceiling and speaking average](./phase-02-assessment-band-ceiling-and-speaking-average.md) | Pending |
| 3 | [Learning assignment and retake](./phase-03-learning-assignment-and-retake.md) | Pending |
| 4 | [Frontend pool picker and retake](./phase-04-frontend-pool-picker-and-retake.md) | Pending |
| 5 | [Verification and contracts](./phase-05-verification-and-contracts.md) | Pending |
| 6 | [Seed placement exams and recording scripts](./phase-06-seed-placement-exams-and-recording-scripts.md) | Pending |

Build order: 1 first (2, 3 and 6 need the pool column); 2, 3 and 6 are independent and can run in parallel (6 is mostly authoring and review, the longest path); 4 needs 2 and 3; 5 last. **Release order: audio uploaded first, then Content (with the phase 6 migrations), then Assessment, then Learning together with the frontend** (the old route disappears with Learning, the new routes appear with it).

## Public contract changes (the user approved the route removal; web is the only consumer)

- Learning: `GET /api/learning/placement-test` is **replaced** by `GET /api/learning/placement-test/options`, `POST /api/learning/placement-test/assignments` and `POST /api/learning/placement-test/assignments/{id}/release`. `409 PLACEMENT_ALREADY_DONE` is no longer returned; `429 PLACEMENT_LIMIT_REACHED` is new.
- Assessment: the placement result gains `pool`, `ceiling`, `reachedCeiling` and `partial`, and its `skills` list may omit Speaking or Writing; starting a placement attempt can answer `429 PLACEMENT_LIMIT_REACHED`; new `POST /api/assessments/attempts/{id}/cancel` for the caller's own in-progress placement attempt. All additive.
- Content internal: new `placement-packages?pool=` response (`PlacementPackageResponse` with `pool`), `package-versions/{id}` gains `placementPool`. `TopicTestPackageResponse` (shared with topic and course tests) is unchanged.
- Content authoring: `AuthorPackageRequests.CreatePackage` (admin `POST /api/content/admin/packages`, used by the admin form) and the legacy `CreateContentPackageRequest` gain `placementPool` (required for `PLACEMENT_TEST`, rejected otherwise). The author list `GET /api/content/admin/packages` gains an optional `placementPool` filter and each summary carries `placementPool` (additive).
- Rule file: `AGENTS.md` §3.8 states "placement chỉ làm được một lần (`409 PLACEMENT_ALREADY_DONE`)". Updating it needs the user's approval; it is listed in phase 5 but not edited until approved.

## Cross-plan notes

- `261023-0050-content-author-workspace-completion` (in-progress) built the package authoring API and admin screens. This plan extends `ContentPackageAuthoringController`, `ContentPackageController`, the two request records and the admin `PackageCreateForm`; rebase on its changes, not a blocker.
- `261103-1156-ai-speaking-assessment` (in-progress) excludes placement Speaking on purpose. Its follow-up for placement will rely on this plan's rule "Speaking is left out until graded".
- `261007-2315-placement-test-gate` introduced placement; this plan lifts its "once only" rule.

## Acceptance criteria

- Each pool serves only its own exams; a pool with no published exam answers `404 NO_PLACEMENT_TEST` for that pool and is disabled in the picker.
- Consecutive assignments of one learner never repeat an exam until the pool is exhausted; then the least recently assigned exam comes first. A released assignment is not counted as used.
- A repeated request within the reuse window returns the same assignment and does not use up a retake.
- The 4th assignment within 24 hours answers `429 PLACEMENT_LIMIT_REACHED` in Learning, and the 4th placement attempt within 24 hours is refused by Assessment even when the learner skips Learning.
- Pool `BAND_0_3` with near-perfect answers stores a band of at most 3.0 and reports `reachedCeiling`; a computed band of exactly 3.0 reports it too; pool `BAND_5_9` never does.
- With no configured Speaking band, a learner with Listening 6.0, Reading 6.0, Writing 6.0 gets overall 6.0, not 4.5. An essay that could not be graded leaves Writing out and sets `partial`; no essay sent still counts 0.0.
- The result's skill list and overall always agree, read from what was stored at grading time, never recomputed from configuration.
- Legacy placement results read as before (skills list including Speaking at 0.0 as it was graded).
- A new result replaces the stored band; course recommendation, suggestions and the learner report behave correctly with a lowered band.
- An in-progress retake can be cancelled; the learner never gets stuck on an empty test; a "Làm lại" entry exists wherever a placement result is shown.
- An admin can create a placement package with a pool through the admin form; the stored package keeps it.
- The admin Packages page shows each placement package's pool, filters by pool, and under the placement type shows the published exam count per pool with a warning below 2.
- After the release each pool has two published exams (phase 6); the audio of every seeded exam is reachable before the release; every seeded exam was signed off by a reviewer before its migration was merged.

## Open questions (operations)

- Who signs off the exam content before the migrations are merged (an IELTS-qualified reviewer), and by when.
- The user records the 12 Listening files from the scripts in `docs/placement-exams/` and uploads them; target date.
- Exact boundary semantics at 3 and 5 (the pools are labels; the ceiling decides).
- Value of `ASSESSMENT_PLACEMENT_SPEAKING_BAND` on the live servers.
- Whether retakes count several times in `BandStatistics`.
- Approval to edit `AGENTS.md` §3.8.

## Validation Log

### Session 1 — 2026-11-17

**Verification Results**
- Tier: Full (5 phases; Fact Checker, Flow Tracer, Scope Auditor, Contract Verifier)
- Claims checked: 34 | Verified: 31 | Failed: 3 | Unverified: 0
- Verified: migration numbers free (Content V137, Assessment V24, Learning V55); `ContentPackageJpaEntity` / `ContentPackagePersistenceMapper` / `ContentPackageJpaRepository`; `PackageVersionContentResponse` for `package-versions/{id}`; `AttemptCreator` is a class in `application/usecase`; `PlacementGradingService` makes no HTTP call; `assessment.placement.speaking-band` exists only as an `@Value` default of 0.0 (no config file sets it); `overall_band` is `NUMERIC(3,1)`; `AssessmentCompletedV2.Data` carries `packageVersionId`; gateway `default-filters` apply rate-limit tier `default`; existing `TopicTestAssignment` / `AssignTopicTestUseCase` pattern; frontend callers of the old route.
- Failures (all Contract Verifier, caller lists missing, now added): (1) Content placement listing also used by `InternalLearningContentControllerTest` and `PlacementTestSeedTest`; (2) Assessment `PackageVersion` constructed in `ContentPackageClient`, `AutoGradingIntegrationTest`, `GateEssayGradingIntegrationTest`, `StartAssessmentAttemptUseCaseTest`; (3) Learning old route referenced by `LearningSecurityWebMvcTest` and `GetPlacementTestUseCaseTest`.

**Decisions:** old route removed (web only); 15-minute reuse window kept; higher-pool hint when the computed band reaches or exceeds the ceiling (flag `reachedCeiling`); corrections applied.

**Superseded by the red team session below:** the "local day" retake window (now rolling 24 hours), the seeded exam pool (now `BAND_5_9`), and "Writing default band unchanged" (now Writing left out when ungradable).

### Session 2 — 2026-11-17 (user decisions after the red team)
- Phase 6 added: five new placement exams seeded by migration, two per pool at launch (the existing exam is the second in pool 5-9); shortened exams for `BAND_0_3` and `BAND_3_5`; **published on deploy** (the user chose this over seeding drafts); the user records the audio from markdown scripts this phase produces. The earlier "at least 3 exams per pool" guidance became "two at launch, a third recommended".
- Risk accepted by the user: no post-deploy review of the content, so the review gate moves before the merge and the audio check before the release (phase 6, gates 1 to 3).

### Whole-Plan Consistency Sweep (validation session)
- Files reread: plan.md, phase-01 to phase-05; unresolved contradictions: 0 at that time.

## Red Team Review

### Session — 2026-11-17
**Findings:** 14 after deduplication from 27 raw (14 accepted, 0 rejected)
**Severity breakdown:** 2 Critical, 9 High, 3 Medium
**Reviewers:** Security Adversary (Fact Checker), Failure Mode Analyst (Flow Tracer), Assumption Destroyer (Scope Auditor); every finding carried `file:line` evidence; the controller re-verified findings 1, 6, 8 and 14 against the code.

| # | Finding | Severity | Disposition | Applied To |
|---|---------|----------|-------------|------------|
| 1 | Plan extended the wrong create-package endpoint; the admin form posts to `AuthorPackageRequests.CreatePackage`, and `CreateContentPackageCommand` has 5 callers | Critical | Accept | Phase 1, Phase 4 |
| 2 | Seeded exam backfilled to 3-5 would cap everyone at 5.0 with two empty pools | Critical | Accept (seed to `BAND_5_9`) | Phase 1, Phase 5, plan.md |
| 3 | Limit and no-repeat only advisory: Assessment accepts any version id; "id not exposed" was false | High | Accept (Assessment backstop) | Phase 2, Phase 3 |
| 4 | Assignment lifecycle: time-only reuse window, completion match without time guard and in the band transaction, failed start burns a retake | High | Accept | Phase 3, Phase 4 |
| 5 | Abandoned retake pins the page on an empty test; no retake entry after a placement exists; no cancel endpoint | High | Accept | Phase 2, Phase 4 |
| 6 | HTTP time zone lookup under the user lock; learner-editable time zone resets the daily limit | High | Accept (rolling 24 hours) | Phase 3, plan.md |
| 7 | "Latest wins" ordering, band can drop, consumers of the band not designed for it | High | Accept (warning, consumer tests, wait for applied band) | Phase 3, Phase 4 |
| 8 | Writing not sent is 0.0 and ungradable Writing defaulted to 5.5 | High | Accept (exclude ungradable, mark partial) | Phase 2, Phase 4 |
| 9 | Repeated exams plus answer review allow memorised retakes | High | Accept (keep review, 3 exams per pool guidance) | Phase 5, plan.md |
| 10 | Skills list recomputed from live configuration; legacy rows inconsistent | High | Accept (persist at grading) | Phase 2 |
| 11 | Release order and old clients; Learning trusted Content's filter | High | Accept | Phase 3, Phase 5, plan.md |
| 12 | Wrong method name and shared DTO not in the file lists | Medium | Accept | Phase 1, Phase 3 |
| 13 | Content fan-out on requests that will be rejected | Medium | Accept | Phase 3 |
| 14 | Rule file, contracts and READMEs still state the "once only" rule | Medium | Accept (`AGENTS.md` needs approval) | Phase 5 |

### Whole-Plan Consistency Sweep
- Files reread: plan.md, phase-01 to phase-05
- Decision deltas checked: 9 (rolling 24 hours, seed to `BAND_5_9`, Assessment backstop and `PLACEMENT_LIMIT_REACHED`, release endpoint, cancel endpoint, Writing exclusion and `partial`, stored skill bands, release order, dedicated Content DTO)
- Reconciled stale references: `retakesLeftToday`, "local day", `PLACEMENT_DAILY_LIMIT`, `StudyTimezones`, default Writing band, `BAND_3_5` backfill, `getPlacementPackages` naming, `CreateContentPackageRequest`-only authoring
- Unresolved contradictions: 0
