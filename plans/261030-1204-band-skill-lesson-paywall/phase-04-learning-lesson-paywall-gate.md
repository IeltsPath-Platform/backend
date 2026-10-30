---
phase: 4
title: "Learning lesson paywall gate"
status: pending
priority: P1
effort: "7h"
dependencies: [2, 3]
---

# Phase 4: Learning lesson paywall gate

## Overview

Enforce the open/locked rule on the server at every lesson entry point, and report paywall state in lesson lists. Position and premium-only facts come from content-service (phase 2); this phase only decides.

## Requirements

- Functional: a lesson is open if the first applicable rule below says so (cheapest first):
  1. `premiumOnly` (Premium-only topic, stays Premium-only and is never unlockable with Points): open iff grandfathered or `AccessClient.premium()`; no other rule applies. This runs before the no-course rule so a Premium-only topic without a course is still protected.
  2. `courseId == null`: open (topic without a course is not gated).
  3. `skillPosition <= 3` (`FREE_LESSONS_PER_SKILL`, one constant): open.
  4. Learner owns any skill the lesson teaches, or owns the combo (ACTIVE unlocks only): open.
  5. Grandfathered (`lesson_progress.grandfathered = true`): open.
  6. `AccessClient.premium()` is true: open.
  Otherwise 403 `LearningGateException` code `PAYWALL`.
- Skill rules (validated): primary skill = `topic.skill`, else the first of the lesson's skills in the order LISTENING, READING, WRITING, SPEAKING; position counts in the primary skill's list only; ownership of any skill the lesson teaches opens it.
- Grandfathering (validated): migration `V39__lesson_progress_grandfathered.sql` adds `grandfathered BOOLEAN NOT NULL DEFAULT false` and sets it `true` for every existing row. New rows are false. No date configuration.
- Premium expiry (validated): once Premium ends, lessons beyond the free/owned/grandfathered set lock again, including re-reading lessons completed under Premium (the gate applies to `authorizeToRead` as well). Practice and review of completed lessons stay allowed (exempt paths below).
- `premium()` is called only when no earlier rule opened the lesson, at most once per request (memoised). If Access is unavailable at that point, the request fails with the existing `AccessUnavailableException` (503) for that lesson only; free, owned and grandfathered lessons never depend on Access.
- Entry points (verified in code):

| Entry point | Behaviour |
|---|---|
| `GetLessonUseCase`, `GetLessonResourcesUseCase` (via `authorizeToRead`), `GetLessonPracticeSetsUseCase` (`:44`), `SubmitLessonExerciseUseCase`, `SubmitLessonEssayUseCase` (`:106`), `CompleteLessonUseCase` | Gated: PAYWALL |
| `GetTopicLessonsUseCase` | Not blocked; each summary carries `paywall` and `freePreview` flags, separate from progress `LOCKED` |
| `PracticeAccess` (`:26`), `StartPracticeAttemptUseCase` (`:45`), `GetReviewUseCase` (`:87`), `GetStudyContextUseCase` (`:52`), `SubmitTheoryCheckUseCase` (`:63`), `ErrorTrapReviews` (`:120`) | Exempt by design: they require a completed lesson (PracticeAccess `PRACTICE_LOCKED`) or an existing review item, so a never-opened paywalled lesson is unreachable. Practice and review of completed lessons stay allowed after Premium expires |

- Non-functional: paywall check runs before any progress side effect; existing progress gates (`LESSON_LOCKED`, `PRACTICE_REQUIRED`, `REVIEW_REQUIRED`) keep meaning and order; Premium-only topics are enforced server-side for the first time (they were only labelled before), so free learners now get 403 on them.

## Tests first (red)

- Pure `PaywallPolicy` table tests: no course; position 1-3 vs 4; premium; owned skill; combo; PENDING/CANCELLED unlock does not count; multi-skill lesson (primary skill L before R; owning only the non-primary skill opens it); premium-only lesson (owning the skill or combo does not open it; grandfathered or Premium does); premium-only lesson with no course is still protected; grandfathered progress row; skill with fewer than 3 lessons.
- Order-of-checks tests: lesson at position 2 opens with Access down; lesson at position 6 with Access down returns 503 not PAYWALL; `premium()` called once per request.
- Entry-point tests: free learner gets `PAYWALL` on position 4 for get, resources, practice sets, exercise submission, essay submit, complete; allowed on position 3; allowed after unlock; Premium allowed; after Premium expiry a lesson beyond the free set that was completed under Premium returns `PAYWALL` for get and resources too, while practice and review of it still work.
- Migration test: existing `lesson_progress` rows end `grandfathered = true`, rows inserted afterwards `false`.
- Exempt-path tests: each exempt entry point is unreachable for a paywalled lesson the learner never completed.
- `GetTopicLessonsUseCase`: `paywall`/`freePreview` flags correct; progress `LOCKED` unchanged.
- Error mapping: 403 body has code `PAYWALL`.

## Related Code Files

- Create: `domain/service/PaywallPolicy.java` (pure), `application/service/PaywallGate.java` (loads unlock set, memoised premium), constant `FREE_LESSONS_PER_SKILL = 3`, migration `V39__lesson_progress_grandfathered.sql`.
- Modify: `domain/exception/LearningGateException.java` (add `PAYWALL` message), `application/service/LessonAccess.java` (`authorize`/`authorizeToRead`/`reauthorize` call the gate before `gate.authorize`), `GetTopicLessonsUseCase.java` + `TopicLessonsResult`/response (flags), `api/exception/GlobalExceptionHandler.java` if the body needs a field. Content record fields come from phase 2.

## Implementation Steps

1. Write failing tests (policy -> order of checks -> entry points -> exempt paths).
2. Implement `PaywallPolicy` and `PaywallGate`.
3. Wire into `LessonAccess`; confirm each gated use case goes through it and each exempt one meets its precondition (the table above).
4. Add paywall flags to topic-lessons result.
5. Run full learning-service tests.

## Success Criteria

- [ ] Policy, order-of-checks and entry-point tests green.
- [ ] Direct API calls to a locked lesson return 403 `PAYWALL` on every gated entry point.
- [ ] Learners with progress before go-live keep access to those lessons.
- [ ] Access outage does not affect free, owned or grandfathered lessons.
- [ ] No existing gate test changed behaviour.

## Risk Assessment

- Position drift when authors reorder or insert lessons: the free set follows order, not lesson identity; grandfathering protects lessons already started.
- Grandfathering is decided by the migration itself, so there is nothing to configure at deploy. The flag must be set before the gate code serves traffic: ship migration and gate together.
- A new lesson entry point added later without `LessonAccess` would leak; the entry-point table is the checklist for reviewers.
