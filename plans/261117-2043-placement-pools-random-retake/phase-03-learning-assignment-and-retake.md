---
phase: 3
title: "Learning assignment and retake"
status: pending
priority: P1
dependencies: [1]
effort: "L"
---

# Phase 3: Learning assignment and retake

## Overview
Learning becomes the issuer of placement exams: it lists the pools, assigns a random exam of the chosen pool without repeats, enforces up to 3 assignments per rolling 24 hours, lets a failed start be released, and no longer refuses a learner who already has a placement.

## Requirements
- Functional:
  1. `GET /api/learning/placement-test/options` returns for the caller: `pools[{pool, examCount}]`, `limitPer24h`, `retakesLeft`, `nextRetakeAt` (when the oldest counted assignment ages out, null if not at the limit), `hasPlacement`, `currentBand` (from `learner_placements`, null if none) and the open assignment if any.
  2. `POST /api/learning/placement-test/assignments` with `{pool}` returns `{assignmentId, pool, packageId, packageVersionId, retakesLeft, nextRetakeAt}`.
  3. `POST /api/learning/placement-test/assignments/{id}/release` (caller's own assignment, no result yet) marks it released: it is not counted in the 24-hour limit and not treated as a used exam. `204`.
  4. Choice rule: candidates are the pool's published exams. If the learner has never been assigned (and not released) some of them, pick one of those at random; otherwise pick the exam whose latest counted assignment is oldest.
  5. Reuse window: a request for the same pool within `reuse-window-minutes` of a counted assignment that has no result and is not released returns that assignment and does not count again. The frontend calls assign only when the learner has no `IN_PROGRESS` placement attempt (it resumes that one instead).
  6. Limit: more than `limit-per-24h` counted assignments in the previous 24 hours answers `429 PLACEMENT_LIMIT_REACHED`.
  7. A pool with no published exam answers `404 NO_PLACEMENT_TEST`.
  8. A learner who already has a placement may assign again; `PLACEMENT_ALREADY_DONE` is no longer returned.
  9. When the placement result arrives, the matching assignment is marked completed (best effort).
- Non-functional: per-user advisory lock around the write (AGENTS §3.8/3.9); **no HTTP call while holding the lock or inside the transaction, and no time zone lookup at all** (the window is a plain rolling 24 hours, so the learner's time zone is irrelevant and cannot be used to reset the limit); idempotent under concurrent requests; deterministic in tests (injected `Clock` and random source).

## Architecture
- Order inside `AssignPlacementTestUseCase`:
  1. Cheap non-authoritative DB checks outside any lock: reuse-window hit returns immediately; at the limit rejects immediately. Neither calls Content.
  2. Fetch candidates from Content through the client (outside lock and transaction). Pool counts and candidate lists are cached for a short TTL (`learning.placement.catalog-cache-seconds`, default 30) so `options` and repeated calls do not fan out per request.
  3. Open a transaction, take the learner lock, re-check reuse and limit, choose, insert.
  Candidate staleness between 2 and 3 is acceptable: worst case the learner gets an exam unpublished a second ago; Assessment refuses to start it and the frontend releases the assignment (requirement 3).
- Client safety: the client keeps only items whose returned `pool` equals the requested pool. If Content does not return a `pool` field (old Content), the call fails closed as `NO_PLACEMENT_TEST`, never as "all exams are in this pool".
- Table `placement_assignments(id, user_id, package_id, package_version_id, pool, assigned_at, completed_at, released_at, attempt_id)`; index `(user_id, assigned_at DESC)` and `(user_id, package_id)`. `attempt_id` is filled only when the result arrives.
- Port `PlacementAssignmentRepository` in `domain/repository`; JDBC adapter in `infrastructure/persistence` (matches this service's style); follow the naming and structure of the existing `TopicTestAssignment` aggregate and `AssignTopicTestUseCase`; port method `LearningContentClient.getPlacementPackages(pool)` returning a new record `PlacementPackage(packageId, packageVersionId, code, pool)`, mapped to `GET /internal/learning-content/placement-packages?pool=` through the existing client (already wrapped by `ResilientCall`). The old `getPlacementTestPackages()` and its `TestPackage` use are removed when `GetPlacementTestUseCase` goes (`TestPackage` stays for topic and course tests).
- Random source behind a small port so tests inject a fixed picker.
- `ApplyAssessmentResultUseCase.applyPlacement` additionally marks the user's latest not-completed, not-released assignment for that `packageVersionId` with `assigned_at <= completedAt` as completed and stores the attempt id (same ordering guard the topic and course assignment repositories use). Marking is best effort: any failure is caught and logged, never rolling back the band update. A replayed or late event matches at most one row and is a no-op otherwise.
- Config in `infra/config-server/config-repo/learning-service.yaml`: `learning.placement.limit-per-24h` (default 3), `learning.placement.reuse-window-minutes` (default 15), `learning.placement.catalog-cache-seconds` (default 30). Gateway: `default-filters` already apply rate-limit tier `default` to every route; keep it for the three routes and say so in the contract (AGENTS §3.9).
- Consumers of the placement band stay unchanged but get tests (see steps): course recommendation (`ListCoursesUseCase.recommended`), suggestions band fallback (`GetSuggestionsUseCase`), learner report (`GetLearnerReportUseCase`, `BandEstimator`), snapshots (`SnapshotService`).

## Related Code Files
- Create: migration `V55__placement_assignments.sql` in `services/learning-service/src/main/resources/db/migration/`; `PlacementPool` (domain vo), `PlacementAssignment` (domain), `PlacementAssignmentRepository`, JDBC adapter, `AssignPlacementTestUseCase`, `ReleasePlacementAssignmentUseCase`, `GetPlacementTestOptionsUseCase`, result records, `PlacementPicker` port + implementation, catalog cache, request/response DTOs, `PlacementPackage` record.
- Modify: `PlacementController.java` (replace the GET with the three routes), `GetPlacementTestUseCase.java` (delete), `PlacementTestResponse.java`/`PlacementTestResult.java` (replace), `LearningContentClient.java` and `RestLearningContentClient.java`, `ApplyAssessmentResultUseCase.java`, `learning-service.yaml`.
- Tests: use-case Mockito tests, repository and migration Testcontainers tests, controller tests, `ApplyAssessmentResultUseCase` test, consumer tests, `ArchitectureTest`. Existing tests that reference the old route and must be replaced or updated: `GetPlacementTestUseCaseTest` (6 references; delete with the use case) and `LearningSecurityWebMvcTest` (4 references; point them at the new routes and keep the role checks).

## Implementation Steps (tests first)
1. **Red (use case, Mockito, fixed clock and picker):** first assignment picks from the pool; two more never repeat; once all used, the oldest-assigned is chosen; a released assignment is neither counted nor used; a request within the reuse window returns the same assignment; after the window a new one; the 4th counted assignment inside 24 hours is `PLACEMENT_LIMIT_REACHED` and an assignment exactly 24 hours old no longer counts; empty pool is `NO_PLACEMENT_TEST`; learner with a placement can assign; Content is called only when a new assignment will be created and never while the lock is held; a Content item without `pool` or with another pool is dropped and an old-Content response fails closed.
2. **Red (Testcontainers):** repository queries (latest counted assignment per package, count in a window, release), migration applies, two concurrent assign calls for one learner produce one assignment (reuse window plus lock).
3. **Red (controller):** roles (`CUSTOMER` only), invalid pool is `400`, ownership on release, response shapes, error codes, `options` fields including `currentBand`.
4. **Red:** `applyPlacement` marks the right assignment completed, ignores an assignment assigned after the result, is idempotent on redelivery, and a failing marking does not roll back the band update.
5. **Red (consumers):** with a lowered stored band, course recommendation, suggestions band and the learner report behave correctly; the report must not claim Speaking is included when no Speaking band was graded (check the `BandEstimator` fallback to the placement band for Speaking).
6. **Green:** implement migration, domain, repository, use cases, client method and cache, controller, config.
7. Remove `PLACEMENT_ALREADY_DONE` from the use cases that raised it; keep the error constant only if another caller still uses it; delete the old route tests.
8. Update contracts: add `docs/contracts/placement-retake-v1.md` (routes, errors, rules, rolling window, release) and `learning-content-internal-v1.md` consumer notes.
9. Run `mvn -q -pl services/learning-service -am test` (Testcontainers) and `ArchitectureTest`.

## Success Criteria
- [ ] New tests failed before and pass after; learning suite passes except failures that already exist at the base commit (`EssayKnowledgeJudgeTest`, the hardcoded migration version in `LearningServiceApplicationTests`).
- [ ] No HTTP call (Content or user-service) inside the lock or transaction; no time zone dependency.
- [ ] Old `GET /api/learning/placement-test` removed with the frontend switched in phase 4 (web is the only consumer).
- [ ] The hard limit lives in Assessment as well (phase 2); this phase documents that Learning's checks alone are not authoritative.

## Risk Assessment
- Abandoned exams count as used, so an exhausted pool starts repeating after `pool size` assignments; phase 6 seeds two exams per pool, so the third assignment in 24 hours already repeats the oldest one; add a third exam per pool when content is available.
- The no-repeat rule is advisory: a learner can start any published version id in Assessment directly. The attempt limit is enforced there; repeats are accepted risk.
- The reuse window can hand back an exam the learner already finished if the result event is delayed; `completed_at` on result and the `IN_PROGRESS` resume rule in the frontend limit this to event latency.
- Rolling 24 hours replaces "per local day": a learner who used 3 at 23:50 can retake at 23:50 the next day, not at midnight. Simpler, not gameable, and noted in the contract.
