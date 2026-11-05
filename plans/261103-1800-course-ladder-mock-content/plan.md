# Course ladder and mock content

Status: all phases done (2026-11-03). Phase 3 went with the `courses.free` flag (V109): content-service admin API and
`GET /internal/learning-content/courses/free`, learning-service `PaywallGate` and course list, frontend tile label and admin form.
The content spec and generator live outside the repo (session scratchpad, `coursegen/`); the generated SQL is the source.

## Goal
Five courses on whole bands, each usable end to end by a learner:
Foundation 4.0 (A1–B1, free) → 5.0 (B1) → 6.0 (B1–B2) → 7.0 (B2–C1) → 8.0 (C1–C2). Speaking stays out (not launched).

## Decisions (agreed with user)
- A course teaches only its band's points; prerequisites sit in a lower course or an earlier chặng of the same course. Chặng order follows prerequisites.
- .5 courses merge up: 5.5 → 6.0, 6.5 → 7.0. The active 5.5 and 6.5 rows keep their ids (learner data) and take the 6.0 / 7.0 code, name and band; the inactive 6.0 / 7.0 rows give their topics over and are deleted (no learner rows reference them, checked in learning_db).
- The 11 B1 grammar/vocabulary points (`G55_*`, `V55_*`) move to 5.0.
- `DEMO_WRITING_CRITERION_GRA` → `G60_PERFECT_TENSES` is dropped (a 6.0 point needed before it is taught).
- The uncommitted `V103__drop_prerequisites_taught_later.sql` is deleted (its local history row removed); its three links are kept by reordering chặng instead (scanning before Matching Information / Sentence Completion; opinion essay before the Writing chặng that needs it), and re-inserted where a local database already lost them.

## Phases
1. `V103__course_ladder.sql` (hand written): courses, topic moves and order, point moves, links, descriptions, milestone positions.
2. `V104`–`V108` (generated from a content spec in the scratchpad): lessons, exercises, practice sets (≥3 objective questions per point), topic tests, course tests for Foundation, 5.0, 6.0 gaps, 7.0 gaps, 8.0.
3. Free course flag (pending user decision: implicit "≤3 lessons per skill" vs a `free` column through content → learning → frontend).
4. Seed test that locks the ladder rules (prerequisite order, no cycles, every point taught and practisable, every chặng tested); run content-service suite.

## Acceptance
- Data checks: 0 prerequisite violations, 0 cycles, every active point has a lesson and a practice set of ≥3 questions, every active chặng has a published lesson and a topic test, every course has a course test.
- Migrations apply on a fresh database and on the local one; content-service tests pass.

## Out of scope
Speaking content, essay grading tasks in new lessons, practice-test milestones for new courses, lesson resources/vocabulary blocks.
