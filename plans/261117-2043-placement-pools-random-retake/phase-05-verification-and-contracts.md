---
phase: 5
title: "Verification and contracts"
status: pending
priority: P2
dependencies: [1, 2, 3, 4, 6]
effort: "M"
---

# Phase 5: Verification and contracts

## Overview
Close the loop: contracts and the docs that state the old rule match the code, all affected suites run, the real flow is checked through the gateway, and operations get a short checklist before any pool is opened.

## Requirements
- Functional: every public contract change in `plan.md` is documented in the same PRs as the code; the full flow works on the Docker stack.
- Non-functional: no change to the architecture dependency table (Learning to Content already exists; Assessment never calls Learning).

## Related Code Files
- Modify/create: `docs/contracts/learning-content-internal-v1.md`, `docs/contracts/content-package-authoring-v1.md`, `docs/contracts/assessment-grading-v1.md`, `docs/contracts/assessment-completed-v2.md`, new `docs/contracts/placement-retake-v1.md`; check `docs/contracts/learner-report-v1.md` (mentions the placement band as the report's starting point).
- Documents that still state the old "once only" rule or the old defaults and must change: `docs/contracts/lesson-learning-v1.md:380` (the `409 PLACEMENT_ALREADY_DONE` row), `docs/system-architecture.md:239-240` (old route and 409), `services/learning-service/README.md:49` (old route), `docs/local-development.md:144-145` and `services/assessment-service/README.md:25-26` (Speaking defaults to 0), and **`AGENTS.md` §3.8 (line 233, "placement chỉ làm được một lần (`409 PLACEMENT_ALREADY_DONE`)")**. `AGENTS.md` is a rule file: show the proposed wording to the user and edit only after approval.

## Implementation Steps
1. Re-read the contracts against the final code and fix drift; update every document listed above (except `AGENTS.md` until approved).
2. Run in `backend`: `mvn -q -pl services/content-service,services/learning-service,services/assessment-service -am test` and the three `ArchitectureTest` classes. Report failures that exist at the base commit separately (`EssayKnowledgeJudgeTest`, `LearningServiceApplicationTests` migration version) instead of calling the run green.
3. Run in `frontend`: `npm run typecheck`, `npm run lint`, `npm run build`.
4. End-to-end on the Docker stack (gateway on the host ports in use; the stack previously needed non-default host ports for Eureka, gateway, Redis and RabbitMQ because other containers hold the defaults):
   - Use the exams seeded by phase 6 (two per pool; locally the audio keys may point at placeholder files, or run without `CONTENT_MEDIA_BASE_URL` and skip the Listening playback check).
   - As a fresh learner: options show three pools with counts; assign pool 0-3 twice, the exams differ; start and submit an attempt with near-perfect answers and confirm the band is at most 3.0 with `reachedCeiling`; assign a 4th time inside 24 hours and see `429 PLACEMENT_LIMIT_REACHED`; start a placement attempt directly in Assessment past the limit and see Assessment refuse it.
   - Release an assignment after a failed start and confirm it is not counted; cancel an in-progress retake and confirm the page falls back to the picker.
   - Submit with the essay left out of grading (stop the LLM or exhaust the quota) and confirm Writing is left out and the result is `partial`.
   - Confirm the assignment is marked completed after the result, that the result's skills and overall agree and stay the same after changing the Speaking configuration, and that suggestions use the new band.
   - Confirm a legacy placement attempt (no pool) still reads as before.
   - Create a placement package with a pool through the admin form and see it listed under that pool; on the Packages page filter by type `PLACEMENT_TEST` and check the pool column, the pool filter and the per-pool published count (after phase 6, 2 / 2 / 2, no warning).
5. `graphify update .` in `backend` and `frontend` after the code changes (repo rule).
6. Release order: audio uploaded and checked, then Content (including the phase 6 migrations), then Assessment, then Learning together with the frontend. Rollback: roll Learning and the frontend back together. Put this and the checklist below in the PR descriptions.
7. Ship checklist for operations (a pool is opened only when its checks pass):
   - Each pool opens with two exams (phase 6; the pre-existing exam is in `BAND_5_9`). With two, a third retake in 24 hours repeats an exam and a learner can memorise it, because the per-question answers stay visible on the result; plan a third exam per pool when content is available. Phase 6 gates (reviewer sign-off, 12 audio files uploaded and checked) must be done before the release.
   - Check whether `ASSESSMENT_PLACEMENT_SPEAKING_BAND` is set on the live servers and what it is.
   - Expect higher placement bands for new learners because Speaking and ungradable Writing are no longer averaged in as 0.0 and 5.5.
   - Confirm no non-web client calls the old `GET /api/learning/placement-test` (the user stated none exist).

## Success Criteria
- [ ] Contracts and the stale documents agree with the code; `AGENTS.md` wording proposed and approved or deferred explicitly.
- [ ] All affected suites pass apart from the listed base-commit failures; frontend gates pass.
- [ ] The end-to-end scenario above passes through the gateway.
- [ ] Release order, rollback note and the operations checklist are in the PRs.

## Risk Assessment
- The end-to-end run needs cloned exam data; it must stay on local databases and never be committed as a migration.
- Changes in two repos (`backend`, `frontend`) ship in one release. Learning without the new frontend breaks the old page (the old route is gone); the new frontend without Learning calls routes that do not exist yet. Deploy Learning and the frontend together, after Content and Assessment.
- If operations skip the exam-count checklist, a pool with one exam repeats it on every retake.
