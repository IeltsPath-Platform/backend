---
phase: 7
title: "Per-skill topic progression"
status: done
priority: P1
effort: "3h"
dependencies: []
---

# Phase 7: Per-skill topic progression

Runs BEFORE phase 4 (numbered last only to keep earlier cross-references stable). Independent of purchase code, so it can start with phase 1.

## Overview

Today a course has exactly one `IN_PROGRESS` topic, ordered across all skills, so a learner cannot reach Reading while a Listening topic is unfinished. The paywall needs the four skills of a band to be independent: each skill progresses on its own, sequential only inside that skill. Without this, "3 free lessons per skill" and "buy one skill" cannot work, because `TOPIC_LOCKED` fires before any paywall rule. Each topic has exactly one skill (phase 2), so the group key is simply the topic's skill.

## Requirements

- Functional:
  - `TopicStatusDeriver` opens one topic per `(courseId, topic skill)` instead of one per `courseId`. Within a group the order is still `sequenceOrder`, then topic id; `PASSED` / `COMPLETED` / `IN_PROGRESS` / `LOCKED` keep their meaning.
  - Topics with `courseId == null` keep one shared group, as today (a topic outside a course may have no skill).
  - `TopicProgress.skills()` stays a set for storage compatibility; for course topics it holds exactly one skill after phase 2.
- Learners already mid-course: no data migration. Status is derived from `passedAt` / `testedAt`, and a topic that is the first unfinished of its course is also the first unfinished of its own skill group, so every current `IN_PROGRESS` topic stays `IN_PROGRESS`. Topics of other skills that were `LOCKED` only by order now open (their lessons are still subject to the paywall).
- Non-functional: no schema change; `LessonAccessGate`, `AssignTopicTestUseCase`, `GetTopicLessonsUseCase`, `ListCoursesUseCase`, `RefreshLearningTopicsUseCase`, `GetSuggestionsUseCase` read statuses through `LearnerCurriculum.status(es)` and need no code change unless tests show an assumption of one open topic per course.

## Tests first (red)

- `TopicStatusDeriver` table tests: four skills each get their own `IN_PROGRESS` topic; second topic of a skill is `LOCKED` until the first is tested or passed, while other skills are unaffected; no-course topics share one group; single-skill course behaves exactly as before.
- `LearnerCurriculum`: statuses for a learner whose current topic is mid-course stay unchanged after the change.
- `AssignTopicTestUseCase` / `LessonAccessGate`: a Reading topic is reachable while a Listening topic is unfinished.
- `GetSuggestionsUseCase.currentBand` still returns the band of the course when several topics of it are `IN_PROGRESS`.
- Regression: existing curriculum and gate tests pass unchanged.

## Related Code Files

- Modify: `learning-service/.../domain/service/TopicStatusDeriver.java` (group key), `domain/aggregate/LearnerCurriculum.java` (class comment says one open topic per course).
- Read to confirm no hidden assumption: `AssignTopicTestUseCase.java:90`, `GetSuggestionsUseCase.java:111`, `ListCoursesUseCase.java:57`, `RefreshLearningTopicsUseCase.java:76`, `GetTopicLessonsUseCase.java:43`, `LessonAccessGate.java:53`, `CourseOutcomeService`.

## Implementation Steps

1. Write failing deriver and curriculum tests.
2. Change the group key from `courseId` to `(courseId, skill)`.
3. Run learning-service tests; fix any consumer test that assumed one open topic per course.
4. Update the class comments that describe the old rule.

## Success Criteria

- [x] A learner can have one `IN_PROGRESS` topic in each of the four skills of a course.
- [x] Existing in-progress learners keep every current `IN_PROGRESS` topic.
- [x] All existing learning-service tests green.

## Risk Assessment

- A topic test needs every lesson of the topic completed (`AssignTopicTestUseCase`, `TEST_LOCKED`). A free learner whose topic has lessons beyond position 3 can never test that topic, so later topics of that skill stay `LOCKED`. Decided as expected (their lessons are past position 3 and paywalled anyway); the UI says so. The topic and course test stay unreachable until the skill is unlocked. Do not change the topic-test rule.
- Course progress counts `PASSED` / `COMPLETED` topics (`ListCoursesUseCase`), so a free learner never completes a course. Expected.
- Existing multi-skill topics (see the phase 2 data audit) must be fixed before this phase ships, otherwise they land in an arbitrary group. Learners with such a topic need the content owner's fix first.
- Rollback: restore the old group key; no data to undo.

## Completion

- `TopicStatusDeriver` keys active progression by `(courseId, skill)`; topics without a course retain one shared group.
- Added interleaved four-skill, same-skill sequencing, passed-topic and no-course regression cases. Updated the curriculum comment and service README.
- Focused gate/progression tests: 33 passed. Full learning-service reactor: 1,002 passed, 0 failures/errors/skips. Testcontainers ran; Surefire waited 30 seconds for broker-listener shutdown after the suite before reporting BUILD SUCCESS.
- Commit: `b302c4f0`.
