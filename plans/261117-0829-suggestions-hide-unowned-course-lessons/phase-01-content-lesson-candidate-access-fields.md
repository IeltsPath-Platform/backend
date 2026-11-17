---
phase: 1
title: Content lesson candidate access fields
status: completed
priority: P2
dependencies: []
effort: S
---

# Phase 1: Content lesson candidate access fields

## Overview
`POST /internal/.../suggestions/candidates` adds `courseId` and `skillPosition` to each lesson candidate, so Learning can apply its paywall rule without one HTTP call per lesson. Additive change to an internal contract.

## Requirements
- Functional: each `lessons[]` item carries `courseId` (UUID, the topic's course) and `skillPosition` (int, 1-based position of the lesson among the course's lessons of the same topic skill, same definition as lesson summaries).
- `skillPosition` MUST equal what `GET` lesson summary / lesson content returns for the same lesson (one definition: `RANKED_COURSE_LESSONS` in `JdbcLearningContentReader`).
- Non-functional: still one query; no N+1; existing fields and ordering unchanged.

## Architecture
`JdbcSuggestionCandidateRepository.lessons(...)` joins the ranked-lessons subquery on `l.id` and selects `t.course_id`, `rl.skill_position`. Reuse the SQL definition instead of copying the window function: `RANKED_COURSE_LESSONS` is `private static final` in `JdbcLearningContentReader` (line 65), so widen it to package-private (both adapters live in `infrastructure/persistence/adapter`) and reference it from the suggestion repository. Do not change its partition/order. <!-- Updated: Validation Session 1 - constant is private -->

Note: the ranking partitions by `t.skill` (topic skill). Lessons whose topic has no skill get a position within the `NULL` partition — same as today for summaries; keep identical.

## Related Code Files
- Modify: `services/content-service/src/main/java/com/ieltspath/content/infrastructure/persistence/adapter/JdbcSuggestionCandidateRepository.java` (lessons query + row mapper)
- Modify: `services/content-service/src/main/java/com/ieltspath/content/infrastructure/persistence/adapter/JdbcLearningContentReader.java` (share `RANKED_COURSE_LESSONS`)
- Modify: `services/content-service/src/main/java/com/ieltspath/content/application/result/SuggestionCandidatesResult.java` (`LessonCandidate` + `UUID courseId, Integer skillPosition`)
- No DTO change: `InternalSuggestionController` returns `SuggestionCandidatesResult` directly, so the record change is the wire change. <!-- Updated: Validation Session 1 - no separate DTO exists -->
- Tests: `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/SuggestionCandidateRepositoryIntegrationTest.java`, `.../api/controller/InternalSuggestionControllerTest.java`

## Implementation Steps (tests first)
1. **Red — repository IT:** in `SuggestionCandidateRepositoryIntegrationTest`, seed one course with ≥ 4 published lessons of one skill (two topics so ordering crosses topics); assert each lesson candidate's `courseId` equals the course and `skillPosition` equals 1..4 in topic-then-lesson order. Add one assertion that the value equals the reader's lesson summary `skillPosition` for the same lesson (guards against two definitions drifting).
2. **Red — controller test:** `InternalSuggestionControllerTest` expects `$.lessons[0].courseId` and `$.lessons[0].skillPosition` in JSON.
3. **Green:** extend the record, DTO, query, mapper; share the ranking SQL.
4. Run `mvn -q -pl services/content-service -am test` (Testcontainers needs Docker; if Docker is absent, say the IT was skipped).

## Success Criteria
- [ ] Both new tests fail before the change and pass after.
- [ ] Existing content-service tests and `ArchitectureTest` pass.
- [ ] Query plan unchanged in shape: one statement for lessons.

## Risk Assessment
- Window function over all course lessons per call: same cost class as lesson summaries; candidates are already limited by KP and band. If slow, restrict the ranked subquery to `t.course_id` values of the candidate topics.
- Field name drift between Content DTO and Learning record → covered by Learning client test in phase 2.
