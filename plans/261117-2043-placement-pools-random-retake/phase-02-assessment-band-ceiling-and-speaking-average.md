---
phase: 2
title: "Assessment band ceiling and speaking average"
status: pending
priority: P1
dependencies: [1]
effort: "L"
---

# Phase 2: Assessment band ceiling and speaking average

## Overview
Assessment remembers which pool an attempt was taken in, caps the placement band at that pool's ceiling, stops averaging in grades that never happened (Speaking, and Writing that could not be graded), stores what it graded so the read model never drifts, hard-limits placement attempts per learner, and lets a learner cancel an in-progress placement attempt.

## Requirements
- Functional:
  - Stored band `= min(computedBand, ceiling(pool))`; no floor. `reachedCeiling` is true when the computed band is at or above the ceiling and the pool is not `BAND_5_9`.
  - An attempt without a pool (older attempts, other types) is not capped.
  - Speaking: `assessment.placement.speaking-band` becomes optional with no default. Not configured: Speaking is left out of the average and of the result's `skills`. Configured: used as today.
  - Writing: essay not sent keeps 0.0. Essay sent but not gradable (LLM unavailable, daily quota, any error handed over by the gate job): Writing is left out of the average and the result is `partial`. The default Writing band (`assessment.placement.default-writing-band`, 5.5) is no longer applied to placement.
  - Skills and bands used for the overall are stored at grading time; the read model reads them. Legacy results (no stored rows) read as before: skills list including Speaking at 0.0.
  - Hard limit: a learner may start at most `assessment.placement.limit-per-24h` (default 3) placement attempts in any rolling 24 hours; the next start answers `429 PLACEMENT_LIMIT_REACHED`, whatever Learning says.
  - Cancel: `POST /api/assessments/attempts/{id}/cancel` lets the owner cancel their own `IN_PROGRESS` placement attempt (status `CANCELLED`); any other type or status is refused.
- Non-functional: no HTTP call or lock during grading (verified: `PlacementGradingService` makes no HTTP call; the pool is captured when the attempt starts); the count-and-create in `AttemptCreator` is serialized per learner (there is no lock there today, only `@Transactional`).

## Architecture
- Capture: `StartAssessmentAttemptUseCase` reads the package version (already fetched from Content) including `placementPool` and passes it to `AttemptCreator.create`; the attempt persists `placement_pool` for placement attempts only.
- Backstop: inside `AttemptCreator.create`, for `PLACEMENT`, take a transaction-scoped advisory lock keyed by the learner (mirror Learning's `LearnerLock` approach), count the learner's placement attempts created in the last 24 hours, refuse when at the limit. The no-repeat rule is not enforced here.
- Pure rule `PlacementBandCeiling` (domain service, no Spring): `apply(computed, pool) -> CeilingResult(band, reachedCeiling, uncapped)`. Pool ceilings: 3.0 / 5.0 / 9.0.
- `PlacementGradingService.finish` computes the average over the skills that were graded, applies the ceiling, and saves the result row with `placement_pool`, `uncapped_band` and `partial`. The bands used per skill are stored with the result: use the existing `skill_scores` table if the completer already writes placement skill bands there (check `AssessmentResultCompleter` and `SkillScoreJpaEntity`), otherwise add a small `placement_result_skills` table. `GetPlacementResultUseCase` / `skillBands` read these rows; they no longer recompute from `speakingBand` or `defaultWritingBand`.
- Gate-job path: `GradeGateEssayJobUseCase.handOver` currently completes a placement essay with the default Writing band when the LLM fails. For placement it must complete the job with "not graded" so `completeIfGraded` leaves Writing out and sets `partial`. Topic and course gate behaviour does not change.
- The Speaking item result is recorded as correct today (`finish`); when Speaking is not graded do not record it as correct (or record it as not judged), so Learning does not log a phantom correct answer.
- Read model: `PlacementResult` and its response DTO gain `pool` (nullable), `ceiling` (nullable), `reachedCeiling` and `partial`. `AssessmentCompleted.v2` keeps its shape; its `overallBand` carries the final (capped) value, which Learning stores via `LearnerPlacement.record`.

## Related Code Files
- Create: migration `V24__placement_pool.sql` in `services/assessment-service/src/main/resources/db/migration/` (`assessment_attempts.placement_pool VARCHAR(10)`, `assessment_results.placement_pool VARCHAR(10)`, `assessment_results.uncapped_band NUMERIC(3,1)` (same type as `overall_band`), `assessment_results.partial BOOLEAN NOT NULL DEFAULT FALSE`, plus the stored-skills table if `skill_scores` is not usable; index on attempts by `(user_id, attempt_type, created_at)` for the limit count); `PlacementPool.java` (domain vo); `PlacementBandCeiling.java` (domain service).
- Modify: `ContentPackageProvider.java` (`PackageVersion` gains `placementPool`; keep the existing convenience constructors so test callers keep compiling), `ContentPackageClient.java` (`VersionResponse` mapping and the canonical `new PackageVersion(...)` call), `StartAssessmentAttemptUseCase.java`, `AttemptCreator.java` (a class in `application/usecase`), the attempt aggregate/entity/mapper, `AssessmentAttemptController.java` (cancel route) and a cancel use case, `PlacementGradingService.java` (constructor property `speakingBand` becomes nullable `Double`; `finish`, `skillBands`, `bandsBySkill`, `completeIfGraded`), `GradeGateEssayJobUseCase.java` (placement hand-over), the result aggregate/entity/mapper, `PlacementResult.java`, `PlacementResultResponse.java`, `GetPlacementResultUseCase.java` (the only place that builds `PlacementResult`), `PlacementReportAssembler.java` if it lists skills, the global exception handler (`PLACEMENT_LIMIT_REACHED` as 429).
- Tests: `PlacementBandCalculatorTest` stays; new `PlacementBandCeilingTest`; `PlacementGradingService` tests; gate-job tests; start-attempt tests; limit and concurrency tests; cancel tests; result API test; repository Testcontainers test. Existing tests that build a `PackageVersion` and must still compile: `AutoGradingIntegrationTest`, `GateEssayGradingIntegrationTest`, `StartAssessmentAttemptUseCaseTest`.

## Implementation Steps (tests first)
1. **Red:** `PlacementBandCeilingTest` (table-driven): below ceiling unchanged and not reached; exactly at ceiling reached; above ceiling capped and reached with `uncapped` kept; `BAND_5_9` never reached; pool null unchanged; each pool's ceiling.
2. **Red:** grading tests: Listening 6.0, Reading 6.0, Writing 6.0, no Speaking configured, overall is 6.0 and `skills` has three entries; with `speaking-band=5.5` configured the four-skill mean; pool `BAND_0_3` with all-correct answers gives 3.0, `reachedCeiling`, `uncapped_band` stored; pool null gives the old result; **no essay sent** counts Writing 0.0; **essay sent but the gate job hands it over (LLM unavailable, quota)** leaves Writing out, `partial` true, no default 5.5; Speaking item not recorded as correct when ungraded.
3. **Red:** read-model tests: the result reads stored skills and overall that agree; changing the Speaking configuration after grading does not change an old result; a legacy result (no stored rows) still lists Speaking at 0.0.
4. **Red:** `StartAssessmentAttemptUseCase` test: placement version with pool stores it on the attempt; practice test version stores none.
5. **Red:** limit tests: three placement attempts in 24 hours are allowed, the fourth is `PLACEMENT_LIMIT_REACHED`, the first one ageing out of the window frees a slot, other attempt types are not counted, two parallel starts at the limit boundary create only one attempt (Testcontainers).
6. **Red:** cancel tests: owner cancels own `IN_PROGRESS` placement attempt; another user, another type, or a submitted attempt is refused; the cancelled attempt no longer counts as current for resume.
7. **Red:** result API test: new fields present for a capped attempt; null/false for a legacy attempt.
8. **Green:** migration, rule, capture at start, backstop, cancel, grading, gate-job hand-over, stored skills, read model, DTOs, config defaults.
9. Update `docs/contracts/assessment-grading-v1.md` (placement result, band rules, `partial`, limit error, cancel) and `docs/contracts/assessment-completed-v2.md` (the `overallBand` of a placement is the capped value).
10. Run `mvn -q -pl services/assessment-service -am test` and `ArchitectureTest`.

## Success Criteria
- [ ] New tests failed before and pass after; assessment suite and ArchitectureTest pass.
- [ ] A 0-3 pool attempt never stores a band above 3.0 and reports `reachedCeiling`; exactly 3.0 also reports it.
- [ ] Default configuration no longer drags the average down with a 0.0 Speaking band or a defaulted 5.5 Writing band.
- [ ] Skills list and overall agree for every new result and do not change when configuration changes.
- [ ] The fourth placement attempt in 24 hours is refused by Assessment itself.

## Risk Assessment
- Dropping the Speaking and Writing defaults changes the band of every new placement (higher than before for most learners). It does not touch stored bands. Announce to operations; if a server already sets the Speaking variable, nothing changes there.
- The cap hides how strong a learner really is when they pick too low a pool; mitigated by the `reachedCeiling` hint and the immediate retake.
- Two limits (Learning assignments, Assessment attempts) must use the same default; Learning counts assignments and Assessment counts started attempts, so the Assessment limit can only be reached when Learning is bypassed or both are used up.
- Stored skills need a one-time decision on `skill_scores` versus a new table; do it first, before the read model changes.
