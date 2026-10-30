---
phase: 2
title: "Content unlock prices and lesson paywall facts"
status: pending
priority: P1
effort: "5h"
dependencies: ["261030-1200-course-cover-description"]
---

# Phase 2: Content unlock prices and lesson paywall facts

## Overview

Store the two unlock prices on `courses`, and let content-service compute, per lesson, the facts the paywall needs (course, primary skill, position within the skill, premium-only). Learning-service receives them on the lesson payloads it already fetches, so it needs no extra call and no ordering logic of its own.

## Requirements

- Functional:
  - `courses.skill_unlock_points`, `courses.combo_unlock_points` (nullable; null = not for sale). Admin create/update accepts them.
  - Internal course payload also carries `skills[]`: primary skills that have at least one published lesson in an active, non-premium-only topic of the course (used by `SKILL_NOT_AVAILABLE`).
  - Internal lesson payloads (`GET /lessons/{id}` at `InternalLearningContentController.java:127` and `GET /topics/{id}/lessons`) add `courseId`, `primarySkill`, `skillPosition`, `premiumOnly`.
- Position definition (single source of truth): 1-based rank among published lessons of the same course and primary skill, in active topics without `required_feature_key`, ordered by `(topic.sort_order, topic.id, lesson.sort_order, lesson.id)`. Primary skill = `topic.skill`, else the lesson's first skill in the order LISTENING, READING, WRITING, SPEAKING. Lessons of premium-only topics have `premiumOnly = true` and no position. Topics with no course: `courseId = null`, no position.
- Non-functional: positive integers only; combo price >= skill price when both set; old callers unaffected (new fields optional); positions computed in SQL in one query per request, not per lesson.

## Tests first (red)

In `content-service` tests:
- Course use case/controller: create/update with prices, null prices, zero or negative rejected (400), combo < skill rejected.
- Reader test for positions: two topics of one skill with 2 + 3 lessons -> positions 1..5; unpublished lesson and inactive topic do not shift positions; premium-only topic flagged and excluded from counting; multi-skill lesson uses primary skill; other skill counts independently; topic without course -> no position.
- Course payload `skills[]` excludes a skill with no published lesson (Speaking today).

## Related Code Files

- Create: `content-service/src/main/resources/db/migration/V87__course_unlock_prices.sql` (two nullable INT columns + CHECK > 0).
- Modify: `Course.java`, `CourseJpaEntity.java`, `CourseRepositoryAdapter.java`, `CreateCourseCommand/UpdateCourseCommand`, `CreateCourseUseCase/UpdateCourseUseCase`, `CourseResult`, `CreateCourseRequest/UpdateCourseRequest/CourseResponse`, `CourseController` (same files as the cover plan; add fields next to its columns).
- Modify: `api/dto/internal/TopicSequenceResponse.java` (`Course` record: prices + `skills`), `TopicSequenceResult.java`, `JdbcLearningContentReader.java` (course SQL near line 160; lesson and topic-lessons queries add position via window function), the internal lesson response DTOs.
- Modify: `learning-service/.../LearningContentClient.java` records `Course`, `Lesson`, `LessonSummary` (keep old constructors; new fields default to null/empty so mixed-version deploys parse).

## Implementation Steps

1. Write failing tests.
2. Migration V87, aggregate/entity/DTO fields, validation.
3. Position query (window function over the filtered lesson set) and wiring into both lesson payloads.
4. Course `skills[]` and prices into the internal course payload.
5. Client records with defaults.

## Success Criteria

- [ ] Admin API round-trips both prices; invalid values rejected.
- [ ] Lesson payloads return correct `skillPosition`, `primarySkill`, `premiumOnly` for the cases above.
- [ ] No extra learning-service HTTP call is needed to learn a lesson's position.
- [ ] Existing content and learning tests green.

## Risk Assessment

- The position rule must match what learners see in the topic list; it is defined only here. Learning-service must never recompute it.
- Constructor overloads in learning-service records: keep old signatures to avoid mass edits.
