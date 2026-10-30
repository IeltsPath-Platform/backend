---
phase: 4
title: "Learning lesson paywall gate"
status: done
priority: P1
effort: "7h"
dependencies: [2, 3, 7]
---

# Phase 4: Learning lesson paywall gate

## Overview

Enforce the open/locked rule on the server at every lesson entry point, and report paywall state in lesson lists. Position facts come from content-service (phase 2); this phase only decides. Every topic has one skill and every lesson teaches only that skill, and there are no Premium-only topics, so one rule covers all course content.

## Requirements

- Functional: a lesson is open if the first applicable rule below says so (cheapest first):
  1. `courseId == null`: open (topic without a course is not gated).
  2. `skillPosition <= 3` (`FREE_LESSONS_PER_SKILL`, one constant): open.
  3. Learner owns the lesson's skill, or owns the combo (ACTIVE unlocks only): open.
  4. Grandfathered (`lesson_progress.grandfathered = true`): open.
  5. `AccessClient.premium()` is true: open.
  Otherwise 403 `LearningGateException` code `PAYWALL`.
- Grandfathering (validated): migration `V39__lesson_progress_grandfathered.sql` adds `grandfathered BOOLEAN NOT NULL DEFAULT false` and sets it `true` for every existing row. New rows are false. No date configuration.
- Premium expiry (validated): once Premium ends, lessons beyond the free/owned/grandfathered set lock again, including re-reading lessons completed under Premium (the gate applies to `authorizeToRead` as well). Practice and review of completed lessons stay allowed (exempt paths below).
- `premium()` is called only when no earlier rule opened the lesson, at most once per request (memoised). If Access is unavailable at that point, the request fails with the existing `AccessUnavailableException` (503) for that lesson only; free, owned and grandfathered lessons never depend on Access.
- Entry points (verified in code):

| Entry point | Behaviour |
|---|---|
| `GetLessonUseCase`, `GetLessonResourcesUseCase` (via `authorizeToRead`), `GetLessonPracticeSetsUseCase` (`:44`), `SubmitLessonExerciseUseCase`, `SubmitLessonEssayUseCase` (`:106`), `CompleteLessonUseCase` | Gated: PAYWALL |
| `GetTopicLessonsUseCase` | Not blocked; each summary carries `paywall` and `freePreview` flags, separate from progress `LOCKED` |
| `StartFreePracticeUseCase` (`execute`, free practice by knowledge point) | Gated by the knowledge point rule below: PAYWALL. It needs no completed lesson, so without this it is a way around the paywall. Check runs right after the learner lock, before hearts are touched |
| `GetLearnerTreeUseCase` | Not blocked; each `Node` carries `locked` (knowledge point rule) and `practiceAvailable` is false when locked |
| `PracticeAccess` (`:26`), `StartPracticeAttemptUseCase` (`:45`), `GetReviewUseCase` (`:87`), `GetStudyContextUseCase` (`:52`), `SubmitTheoryCheckUseCase` (`:63`), `ErrorTrapReviews` (`:120`) | Exempt by design: they require a completed lesson (PracticeAccess `PRACTICE_LOCKED`) or an existing review item, so a never-opened paywalled lesson is unreachable. Practice and review of completed lessons stay allowed after Premium expires |

- Knowledge point rule (free practice and tree `locked`), using the phase 2 fact, same order of checks as lessons: no course or `earliestLessonPosition == null` -> open; `earliestLessonPosition <= 3` -> open; learner owns the skill of the point's topic or the combo -> open; a grandfathered `lesson_progress` row of a lesson that teaches the point -> open; Premium -> open; otherwise locked. Learning-service never maps knowledge point -> lesson itself apart from reading its own progress rows. In the tree, Premium is asked once per request and only when some node needs it; an Access failure there shows `locked = true` for those nodes and never fails the tree. Free practice follows the lesson rule: 503 when Access is down and nothing earlier opened it.
- Progression is per skill (phase 7), so the gate here only meets `TOPIC_LOCKED` inside the same skill. Phase 4 must not start before phase 7.
- Assessment results (`ApplyAssessmentResultUseCase`: placement, topic and course gates, mock, quiz) are not gated and keep writing evidence for any knowledge point, locked ones included. This is intentional: assessments measure the learner and are not content access. The report only lists points that already have evidence, so it needs no flag.
- Non-functional: paywall check runs before any progress side effect; existing progress gates (`LESSON_LOCKED`, `PRACTICE_REQUIRED`, `REVIEW_REQUIRED`) keep meaning and order. Courses no longer have Premium-only topics (phase 2), so Premium only ever means "opens everything".

## Tests first (red)

- Pure `PaywallPolicy` table tests: no course; position 1-3 vs 4; premium; owned skill; combo owned; owning a different skill does not open; PENDING/CANCELLED unlock does not count; grandfathered progress row; skill with fewer than 3 lessons.
- Order-of-checks tests: lesson at position 2 opens with Access down; lesson at position 6 with Access down returns 503 not PAYWALL; `premium()` called once per request.
- Entry-point tests: free learner gets `PAYWALL` on position 4 for get, resources, practice sets, exercise submission, essay submit, complete; allowed on position 3; allowed after unlock; Premium allowed; after Premium expiry a lesson beyond the free set that was completed under Premium returns `PAYWALL` for get and resources too, while practice and review of it still work.
- Migration test: existing `lesson_progress` rows end `grandfathered = true`, rows inserted afterwards `false`.
- Exempt-path tests: each exempt entry point is unreachable for a paywalled lesson the learner never completed.
- `GetTopicLessonsUseCase`: `paywall`/`freePreview` flags correct; progress `LOCKED` unchanged.
- Error mapping: 403 body has code `PAYWALL`.
- Free practice: free learner gets `PAYWALL` for a point whose `earliestLessonPosition` is 4 or more, allowed at 3 or less, allowed after skill unlock, allowed with Premium, allowed for a point with a grandfathered lesson; no hearts are spent on a refused start; point with no lesson or no course is not gated.
- Tree: `locked` true for the same points and false after unlock; `practiceAvailable` false when locked; Access down does not fail the tree; assessment evidence on a locked point still shows its mastery in the tree.

## Related Code Files

- Create: `domain/service/PaywallPolicy.java` (pure), `application/service/PaywallGate.java` (loads unlock set, memoised premium), constant `FREE_LESSONS_PER_SKILL = 3`, migration `V39__lesson_progress_grandfathered.sql`.
- Modify: `domain/exception/LearningGateException.java` (add `PAYWALL` message), `application/service/LessonAccess.java` (`authorize`/`authorizeToRead`/`reauthorize` call the gate before `gate.authorize`), `GetTopicLessonsUseCase.java` + `TopicLessonsResult`/response (flags), `StartFreePracticeUseCase.java` (call the gate), `GetLearnerTreeUseCase.java` (`Node.locked`) + its response DTO, `api/exception/GlobalExceptionHandler.java` if the body needs a field. Content record fields come from phase 2.

## Implementation Steps

1. Write failing tests (policy -> order of checks -> entry points -> exempt paths -> free practice and tree).
2. Implement `PaywallPolicy` and `PaywallGate`.
3. Wire into `LessonAccess`; confirm each gated use case goes through it and each exempt one meets its precondition (the table above).
4. Add paywall flags to topic-lessons result; add the knowledge point rule to free practice and the tree.
5. Run full learning-service tests.

## Success Criteria

- [x] Policy, order-of-checks, entry-point, free-practice and tree tests green.
- [x] Direct API calls to a locked lesson return 403 `PAYWALL` on every gated entry point.
- [x] Learners with progress before go-live keep access to those lessons.
- [x] Access outage does not affect free, owned or grandfathered lessons.
- [x] No existing gate test changed behaviour.

## Completion

- Added the shared request-scoped paywall gate and pure lesson policy. Lesson reads, resources, practice sets, submissions and completion enforce it before progress side effects; lesson lists expose `paywall` and `freePreview` separately from progress status.
- Added V39 grandfathering for existing lesson progress. Free practice checks the same point-level gate before hearts or attempts; the knowledge tree marks locked points and disables free practice, while Access outages only lock points needing Premium.
- Focused Phase F tests: 84 passed, 0 failures/errors/skips, including PostgreSQL V39 upgrade, premium expiry, free-practice no-side-effect and tree-outage checks.
- Full learning-service reactor: 1,035 tests passed, 0 failures/errors/skips. Surefire waited 30 seconds for broker-listener shutdown, then reported BUILD SUCCESS.
- Implementation commit is recorded in the acceptance report.

## Risk Assessment

- Position drift when authors reorder or insert lessons: the free set follows order, not lesson identity; grandfathering protects lessons already started.
- Grandfathering is decided by the migration itself, so there is nothing to configure at deploy. The flag must be set before the gate code serves traffic: ship migration and gate together.
- A new lesson entry point added later without `LessonAccess` would leak; the entry-point table is the checklist for reviewers.
- A knowledge point shared by topics of different skills is gated through the topic that carries it in the content payload; the phase 2 check lists such points.
