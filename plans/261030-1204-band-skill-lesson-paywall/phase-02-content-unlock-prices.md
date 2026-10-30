---
phase: 2
title: "Content unlock prices and lesson paywall facts"
status: pending
priority: P1
effort: "6h"
dependencies: ["261030-1200-course-cover-description"]
---

# Phase 2: Content unlock prices and lesson paywall facts

## Overview

Store the unlock prices (one per skill per course, plus one combo per course, editable by R&D in admin), enforce one skill per topic, and let content-service compute, per lesson, the facts the paywall needs (course, skill, position within the skill). Learning-service receives them on the lesson payloads it already fetches, so it needs no extra call and no ordering logic of its own.

## Requirements

- Functional:
  - Prices (decided): one unlock price per `(course, skill)` in new table `course_skill_prices(course_id, skill, unlock_points)` (PK `(course_id, skill)`, `unlock_points > 0`), plus `courses.combo_unlock_points` (nullable). No row or null = not for sale. R&D sets and changes them in admin at any time, per skill per course, with no deploy or migration; no price is seeded.
  - Course create/update accepts `skillUnlockPoints` as a map `{LISTENING, READING, WRITING, SPEAKING -> integer or null}` and `comboUnlockPoints`. The map replaces all skill prices of the course (a skill missing or null has no price); unknown skill keys are rejected with 400. A skill with no published lesson may be priced in advance; it is still not purchasable (`SKILL_NOT_AVAILABLE`, phase 3).
  - Price sanity checked on every save (whichever price is edited): each price a positive integer; combo >= the highest skill price set; when two or more skill prices are set, combo <= their sum (a combo dearer than buying every skill makes no sense). The price guide against Premium is a phase 6 task, not a check: Premium is priced in Access, so there is no cross-service validation.
  - One skill per topic (decided): every topic that belongs to a course has exactly one `skill`, and every lesson in it teaches only that skill. A lesson has no skill column: `LessonContentResult` derives its skills from the skills of its knowledge points (`ALL` ignored). So the rule is enforced on knowledge points: `CreateTopicUseCase` / `UpdateTopicUseCase` reject a course topic without a `skill`; `LessonAuthoringUseCase.knowledgePoints(lessonId, ids)` rejects a knowledge point whose skill (other than `ALL`) differs from the topic's skill; knowledge points owned by a course topic must carry the topic's skill or `ALL` (find the create/update path of topic knowledge points). A lesson's `skill` in the payload is its topic's skill.
  - Topics no longer carry a Premium gate: migration clears `topics.required_feature_key` for all topics (`V14`, `V15`, `V21` set it). Checked: `CreateTopicRequest`, `UpdateTopicRequest` and the admin `TopicForm` do not carry the key, so no API or form change is needed. Keep reading the field in `TopicJpaEntity` / topic payloads so mixed-version deploys still parse; learning-service simply receives null. `required_feature_key` on practice sets, packages and questions is a separate concern and stays.
  - Internal course payload also carries `skills[]`: skills of the course's active topics that have at least one published lesson (used by `SKILL_NOT_AVAILABLE`).
  - Internal lesson payloads (`GET /lessons/{id}` at `InternalLearningContentController.java:127` and `GET /topics/{id}/lessons`) add `courseId`, `skill`, `skillPosition`.
  - Knowledge points in the topic-sequence payload (`TopicSequenceResponse` / `LearningContentClient.KnowledgePoint`) add `earliestLessonPosition` (Integer; the lowest `skillPosition` among the lessons of that topic that teach the point; null when no lesson teaches it or the topic has no course). Learning-service uses it to gate free practice and mark tree nodes, so it never maps knowledge point -> lessons itself.
- Position definition (single source of truth): 1-based rank among published lessons of the same course and skill, in active topics, ordered by `(topic.sort_order, topic.id, lesson.sort_order, lesson.id)`. Topics with no course: `courseId = null`, no position.
- Non-functional: positive integers only; old callers unaffected (new fields optional); positions computed in SQL in one query per request, not per lesson.

## Tests first (red)

In `content-service` tests:
- Course use case/controller: create/update with different prices per skill and per course, null prices, zero or negative rejected (400), unknown skill key rejected, combo below the highest skill price rejected, combo above the sum of two or more skill prices rejected; editing one skill price re-runs the combo checks; updating with a smaller map clears the missing skills; round trip returns the same map.
- Topic/knowledge point validation: course topic without skill rejected; attaching a knowledge point of another skill to a lesson rejected, `ALL` accepted; a topic's own knowledge point of another skill rejected; lesson payload reports the topic's skill.
- Reader test for positions: two topics of one skill with 2 + 3 lessons -> positions 1..5; unpublished lesson and inactive topic do not shift positions; another skill counts independently; topic without course -> no position.
- Course payload `skills[]` excludes a skill with no published lesson (Speaking today).
- Knowledge point fact: a point taught by lessons at positions 2 and 5 of its topic -> `earliestLessonPosition = 2`; taught by no lesson or topic without course -> null.
- Migration test: `required_feature_key` is null for every topic afterwards; practice-set and package keys untouched.

## Related Code Files

- Create: `content-service/src/main/resources/db/migration/V87__course_unlock_prices.sql` (`courses.combo_unlock_points` nullable INT + CHECK > 0, and table `course_skill_prices` with FK to `courses`) and `V88__clear_topic_premium_gate.sql` (`UPDATE topics SET required_feature_key = NULL`).
- Modify: `Course.java`, `CourseJpaEntity.java`, `CourseRepositoryAdapter.java`, `CreateCourseCommand/UpdateCourseCommand`, `CreateCourseUseCase/UpdateCourseUseCase`, `CourseResult`, `CreateCourseRequest/UpdateCourseRequest/CourseResponse`, `CourseController` (same files as the cover plan; add fields next to its columns). The `Course` aggregate holds the skill-price map; `CourseRepositoryAdapter` reads and writes `course_skill_prices` (replace the course's rows on update) through a new JPA entity.
- Modify: `CreateTopicUseCase`, `UpdateTopicUseCase`, `LessonAuthoringUseCase.knowledgePoints` and the topic knowledge point create/update path (one-skill validation).
- Modify: `api/dto/internal/TopicSequenceResponse.java` (`Course` record: `skillPrices` map, `comboUnlockPoints`, `skills`; knowledge point: `earliestLessonPosition`), `TopicSequenceResult.java`, `JdbcLearningContentReader.java` (course SQL near line 160; lesson and topic-lessons queries add position via window function), the internal lesson response DTOs.
- Modify: `learning-service/.../LearningContentClient.java` records `Course`, `Lesson`, `LessonSummary`, `KnowledgePoint` (keep old constructors; new fields default to null/empty so mixed-version deploys parse).

## Implementation Steps

1. Write failing tests.
2. Migrations V87 and V88, aggregate/entity/DTO fields, validation.
3. Run the data audit below and report violations before enabling the validation in production.
4. Position query (window function over the filtered lesson set) and wiring into both lesson payloads and the knowledge point fact.
5. Course `skills[]` and prices into the internal course payload.
6. Client records with defaults.

## Data audit (one skill per topic)

Existing data may break the new rule: `V23__seed_multi_skill_topic.sql` seeds topic `TREES_MULTI_SKILL` with `skill` NULL in course 5.5 and attaches READING, LISTENING and WRITING knowledge points to it, so its lessons teach several skills. Before go-live, list (as a query in `plans/reports/`, not a migration): course topics with null skill; knowledge points whose skill (other than `ALL`) differs from their topic's skill; lessons whose derived skills therefore span more than one skill. Each violation is fixed by the content owner (split the topic or lesson, or assign one skill); do not auto-fix.

## Success Criteria

- [ ] R&D can set, change and clear the price of each skill of each course, and the combo price, from admin without a deploy; invalid values and price combinations are rejected.
- [ ] Course topics always have one skill and their knowledge points never carry another skill, so lessons never teach another skill.
- [ ] No topic carries a Premium gate (`required_feature_key` null for every topic).
- [ ] Lesson payloads return correct `skill` and `skillPosition`; knowledge points return `earliestLessonPosition`.
- [ ] No extra learning-service HTTP call is needed to learn a lesson's position or a knowledge point's earliest position.
- [ ] Audit lists every existing violation; existing content and learning tests green.

## Risk Assessment

- The position rule must match what learners see in the topic list; it is defined only here. Learning-service must never recompute it.
- Constructor overloads in learning-service records: keep old signatures to avoid mass edits.
- Turning validation on before the audit is fixed would block editing of the offending topics and lessons; run the audit first and fix or accept the list.
- A knowledge point shared by topics of different skills is evaluated through the topic that carries it in the payload; check whether such points exist.
