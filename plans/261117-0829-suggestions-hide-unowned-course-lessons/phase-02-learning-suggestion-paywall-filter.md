---
phase: 2
title: Learning suggestion paywall filter
status: completed
priority: P2
dependencies:
  - 1
effort: M
---

# Phase 2: Learning suggestion paywall filter

## Overview
`GetSuggestionsUseCase` drops lesson candidates the learner cannot open, using the existing `PaywallGate` rule, before `SuggestionRules.choose`. No new paywall rule, no response-shape change.

## Requirements
- A lesson candidate is kept iff `PaywallGate.Request` says the lesson is open: same inputs as lesson reading (`courseId`, `skillPosition`, lesson skills, owned skill/combo unlocks, free course, grandfathered, Premium).
- Completed lessons: keep today's behaviour (they are candidates marked `seen`); do not filter them (learner already studied them).
- Lesson skills: candidate has one `skill` string (null when the lesson's KPs span several skills). Parse it with the existing `PaywallGate.parseSkill` (make it package-visible or expose a small static helper; do not write a second parser); a value outside `LearningSkill` or null ⇒ empty set ⇒ only free preview / combo / free course / Premium / grandfathered open it (conservative, accepted in validation). <!-- Updated: Validation Session 1 - reuse parseSkill -->
- Grandfathered: use the `LessonProgress` already loaded by `lessons.findCompleted(userId)` in `candidates(...)`; non-completed grandfathered lessons are treated as not grandfathered (documented edge, see risks).
- Access unavailable (`AccessUnavailableException` from Premium lookup): treat closed-to-purchases lessons as closed (drop them), keep returning 200. Catch once per request; do not retry.
- Old Content without the new fields: `courseId == null` ⇒ `PaywallPolicy.isOpen` is true ⇒ unchanged behaviour.

## Architecture
- Port record `LearningContentClient.LessonCandidate` gains `UUID courseId, Integer skillPosition` (nullable). `RestLearningContentClient` maps them (Jackson record binding; missing fields ⇒ null).
- `PaywallGate.Request`: add one overload that takes the raw facts, and make the two `lessonOpen(...)` overloads that duplicate the "own purchases → free course → Premium" sequence delegate to it (DRY):
  ```java
  public boolean lessonOpen(UUID courseId, Integer skillPosition, Set<LearningSkill> skills, boolean grandfathered) {
      Ownership ownership = ownership(courseId);
      if (PaywallPolicy.isOpen(courseId, skillPosition, skills, ownership.skills, ownership.combo, grandfathered, false)) return true;
      if (freeCourse(courseId)) return true;
      return PaywallPolicy.isOpen(courseId, skillPosition, skills, ownership.skills, ownership.combo, grandfathered, premium());
  }
  ```
  Behaviour of existing callers must not change (covered by existing PaywallGate/LessonAccess tests).
- `GetSuggestionsUseCase`: inject `PaywallGate`; in `candidates(userId, found)` build one `paywall.forLearner(userId)` and skip a `found.lessons()` item when not completed and not open. Wrap the open check so an `AccessUnavailableException` yields `false`.
- Cost: `ownership(courseId)` reads unlocks once per distinct course (request-scoped cache), bounded by the number of active courses with band ≤ current band (a handful); `getFreeCourseIds` once; Premium once. Write that bound in a one-line comment.

## Related Code Files
- Modify: `services/learning-service/src/main/java/com/ieltspath/learning/application/port/LearningContentClient.java`
- Modify: `services/learning-service/src/main/java/com/ieltspath/learning/infrastructure/client/RestLearningContentClient.java` (only if mapping is manual)
- Modify: `services/learning-service/src/main/java/com/ieltspath/learning/application/service/PaywallGate.java`
- Modify: `services/learning-service/src/main/java/com/ieltspath/learning/application/usecase/GetSuggestionsUseCase.java`
- Tests: new `services/learning-service/src/test/java/com/ieltspath/learning/application/usecase/GetSuggestionsUseCasePaywallTest.java` (Mockito); extend `.../infrastructure/persistence/SuggestionsAndOverviewIntegrationTest.java` if it stubs Content candidates; any test constructing `LessonCandidate` must be updated for the new arity.

## Implementation Steps (tests first)
1. **Red — use case (Mockito), one test per rule:**
   - combo unlock on course A, candidate lessons in course B at `skillPosition` 2 and 5 ⇒ only the position-2 lesson is suggested.
   - same, learner Premium ⇒ both suggested.
   - course B in `getFreeCourseIds` ⇒ both suggested.
   - skill unlock READING on B, candidate skill READING pos 5 ⇒ suggested; candidate skill LISTENING pos 5 ⇒ not.
   - candidate already completed in B pos 5, no access ⇒ still present as before (seen).
   - Access throws `AccessUnavailableException` ⇒ result returned, pos-5 lesson absent, pos-2 present.
   - candidate with `courseId == null` ⇒ suggested (rollout compatibility).
   - a closed lesson is replaced by the next eligible candidate for the same KP (proves filtering happens before `choose`).
2. **Red — PaywallGate:** a focused test that the new overload and `lessonOpen(LessonSummary, progress)` agree for the same facts.
3. **Green:** record fields, client mapping, gate overload + delegation, use-case filter.
4. Fix compile of every test/fixture creating `LessonCandidate` (only `SuggestionsAndOverviewIntegrationTest.java:94-96`; pass `null, null` to keep its current behaviour). That IT mocks only `LearningContentClient`; any new IT case that sets `courseId` must add `@MockitoBean AccessClient` so Premium lookup does not hit a real access-service. <!-- Updated: Validation Session 1 - IT wiring -->
5. Run `mvn -q -pl services/learning-service -am test` and `-Dtest=ArchitectureTest`.

## Success Criteria
- [ ] All new tests red before, green after; existing learning tests green.
- [ ] `ArchitectureTest` green (application uses only ports/services; no infra import).
- [ ] No repository or client call added inside a per-candidate loop beyond the gate's request-scoped per-course cache.

## Risk Assessment
- Multi-skill lessons (`skill == null`) beyond the free preview are hidden for a skill-only buyer even if one of their skills is owned. Acceptable: suggestions are optional; the lesson stays reachable from the course page. Revisit only if Content later returns the skill set.
- In-progress (not completed) grandfathered lessons may be hidden. Rare (pre-paywall learners); the lesson stays reachable from the course page.
- Fail-closed on Access outage hides paid lessons for Premium users during the outage only.
