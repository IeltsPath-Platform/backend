---
title: Hide unowned-course lessons from learning suggestions
description: >-
  Suggestions stop offering lessons of a course the learner cannot open (e.g. a
  lower-band course not bought); free-preview lessons stay
status: completed
priority: P2
branch: main
tags:
  - learning-service
  - content-service
  - paywall
  - suggestions
  - tdd
blockedBy: []
blocks: []
created: '2026-11-17T03:48:41.385Z'
createdBy: 'ck:plan'
source: skill
---

# Hide unowned-course lessons from learning suggestions

Source: [brainstorm report](../reports/brainstorm-261117-0829-cross-course-kp-paywall-leak-report.md). Mode: `--tdd` (each phase writes failing tests first).

## Overview

Suggestions (`GET /api/learning/suggestions`) ask Content for lesson candidates with band ≤ the learner's current band, so a learner who bought IELTS 6.5 but not 5.0 is offered 5.0 lessons and gets `403 PAYWALL` on click. The KP lookup walks back into lower-band courses on purpose (prerequisites may live there); only the offer of closed lessons is wrong.

Fix: Content returns each lesson candidate's `courseId` and `skillPosition`; Learning drops lesson candidates that `PaywallGate` says are closed, before `SuggestionRules.choose`, so another candidate takes the slot. Free-preview lessons (first 3 per skill), free courses, owned skill/combo unlocks and Premium stay suggested.

## Scope decisions (verified 2026-11-17)

- **Out — review theory gating.** Every review points at a lesson the learner worked in (`CompletedLessonLocator.teaching` falls back to the root review's lesson; `ReviewRule`, `ErrorTrapReviews` use lessons answered in), so a learner who never studied the lower course sees no lower-course theory. The only remaining case is content studied under Premium that later expired, which `LessonAccess.authorizePracticeSets` keeps available by design; Premium rules are deferred by the user.
- **Out — practice tests in suggestions.** Their item `path` is `null` (not actionable).
- **Out — practice-set suggestions.** They route to `/review-basket/{kp}/start` (`StartBasketReviewUseCase`), which is not paywall-gated but only starts when the learner has completed a lesson teaching the KP (`lessonLocator.teaching(...)` else `409 NO_LESSON_FOR_KNOWLEDGE_POINT`) and serves bank questions, not course lessons. <!-- Updated: Validation Session 1 - corrected gate source -->
- **Accepted limit — multi-skill lessons.** Content gives one `skill` per lesson candidate (null when several); such lessons beyond the free preview are hidden from a skill-only buyer. They stay reachable from the course page.
- **Kept unchanged:** diagnostics/reviews walking prerequisites into lower-band KPs; 3 free-preview lessons per skill.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Content lesson candidate access fields](./phase-01-content-lesson-candidate-access-fields.md) | Completed |
| 2 | [Learning suggestion paywall filter](./phase-02-learning-suggestion-paywall-filter.md) | Completed |
| 3 | [Verification and docs](./phase-03-verification-and-docs.md) | Completed |

Order: 1 → 2 → 3. Phase 2 tolerates a Content without the new fields (`courseId == null` ⇒ `PaywallPolicy.isOpen` returns true, today's behaviour), so deploy order is not critical.

## Acceptance criteria

- Learner with an ACTIVE combo unlock of course 6.5, nothing on 5.0, no Premium: suggestions contain no lesson of 5.0 with `skillPosition > 3`; a 5.0 lesson at `skillPosition ≤ 3` may still appear.
- Same learner with Premium: 5.0 lessons may appear (today's behaviour).
- A free course's lessons are never filtered.
- Access unavailable while deciding Premium: suggestions still return (HTTP 200) and closed paid lessons are left out (fail-closed); no 503.
- `docs/contracts/learning-content-internal-v1.md` documents the two new lesson-candidate fields in the same PR.

## Dependencies

None. No unfinished plan touches `GetSuggestionsUseCase` lesson candidates or the Content candidate query (scanned `plans/*/plan.md`; `261101-2358-first-attempt-suggested-review` only mentions the suggestions strip cosmetically).

## Open questions

- Premium expiry vs review theory of lessons studied under Premium: deferred until Premium rules are reworked.
- Diagnostic review of a lower-course KP falls back to the root lesson's whole theory (`LESSON_FALLBACK`), not about that KP: content-quality issue, not a leak; separate ticket if wanted.

## Validation Log

### Session 1 — 2026-11-17

**Verification Results**
- Claims checked: 14
- Verified: 12 | Failed: 2 | Unverified: 0
- Tier: Standard
- Failures:
  - Practice-set suggestions "gated by `knowledgePointOpen` in `StartFreePracticeUseCase`": wrong. Route `ReviewBasketController.java:48` → `StartBasketReviewUseCase.java:64-82`, no paywall; requires a completed lesson teaching the KP. Conclusion (no lesson leak) unchanged.
  - Phase 1 "modify the internal response DTO": none exists; `InternalSuggestionController.java:21-23` returns `SuggestionCandidatesResult` directly.
- Extra facts: `RANKED_COURSE_LESSONS` is `private` (`JdbcLearningContentReader.java:65`); Learning binds the response by Jackson (`RestLearningContentClient.java:260-266`), missing fields ⇒ null; only `SuggestionsAndOverviewIntegrationTest.java:94-96` builds `LessonCandidate`; it mocks only `LearningContentClient` (line 61); `PaywallGate.parseSkill` is private.

**Questions asked: 3**
1. Access unavailable during suggestion filtering → hide Premium-only lessons, still return 200 (fail-closed). Confirmed.
2. Multi-skill lesson candidates (`skill == null`) hidden from skill-only buyers beyond the free preview → accepted.
3. Correct the two failed claims → yes; also note `parseSkill` reuse and `AccessClient` mock in the IT.

**Propagated:** phase 1 (DTO step removed, ranking constant visibility), phase 2 (skill parsing, IT mock note).

### Whole-Plan Consistency Sweep
- Searched all plan files for `StartFreePracticeUseCase`, `DTO`, `knowledgePointOpen`, `503`, `skill == null`: no stale statements remain after edits.
- Unresolved contradictions: 0.

### Review Session 1 — 2026-11-17 (during implementation)

- Code review found: suggestions gated `skill` (single skill of the lesson's own points) while the reading gate uses `LESSON_SKILLS` (exercise questions + text-block points). A lesson could be suggested and then refused with `403 PAYWALL`.
- Decision (user): Content adds `skills` (the reading gate's set, same `LESSON_SKILLS` definition); Learning gates on `skills`; `skill` stays for display. Contract `learning-content-internal-v1.md` updated in the same change. This extends the approved phase 1 contract by one additive field.
- Not changed (noted): a failing `getFreeCourseIds` call still fails the suggestions request, as it does for reading; completed lessons may 403 after Premium lapses, as validated.
