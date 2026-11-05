---
phase: 4
title: "Practice clearance (derived, no migration)"
status: pending
priority: P1
effort: "3h"
dependencies: [2]
---

# Phase 4: Practice clearance (derived, no migration)

Work package B with phase 3. Repo: backend. Paths under `services/learning-service/src/`.

## Overview

A failed counted attempt with no pending blocking review clears the lesson's Practice with reason
`ATTEMPT_NOT_BLOCKED`. The reason is computed on every read and never stored, so `lesson_practice_passes`, its CHECK
and Flyway stay untouched, and a rollback leaves no unreadable rows.

Consequence, decided: a lesson cleared this way also counts as practice-cleared for the topic test
(`AssignTopicTestUseCase` reads the same clearance); pending blocking reviews still stop the topic test as today.

## Changes

### 1. `main/java/.../domain/vo/PracticePassReason.java`

```java
public enum PracticePassReason {
    FIRST_SUBMISSION, REVIEW_FINISHED, ALL_SETS_ATTEMPTED, NO_PRACTICE, ATTEMPT_NOT_BLOCKED
}
```

### 2. `main/java/.../domain/service/PracticeClearance.java`

- `STRENGTH` becomes `List.of(FIRST_SUBMISSION, REVIEW_FINISHED, ALL_SETS_ATTEMPTED, ATTEMPT_NOT_BLOCKED)`.
- `ReviewFact`:

```java
    public record ReviewFact(ReviewStatus status, LearningSkill skill, boolean blocking) {
        public ReviewFact(ReviewStatus status, LearningSkill skill) { this(status, skill, true); }
        public ReviewFact(ReviewStatus status) { this(status, null, true); }
    }
```
- In `skillReason(...)`, replace the final `return null;` with:

```java
        boolean noBlockingPending = ofSkill.stream()
                .noneMatch(review -> review.status() == ReviewStatus.PENDING && review.blocking());
        boolean failedHere = attempts.stream().anyMatch(attempt -> attempt.countedAsEvidence()
                && sets.contains(attempt.packageId()) && !attempt.passedSkills().contains(skill));
        // A failed first try with nothing blocking lets the learner go on; suggested reviews do not hold it back.
        if (failedHere && noBlockingPending) return PracticePassReason.ATTEMPT_NOT_BLOCKED;
        return null;
```
- In `wholeLesson(...)`, just before the final `return new Clearance(PracticeStatus.REQUIRED, null);` add:

```java
        boolean noBlockingPending = reviews.stream()
                .noneMatch(review -> review.status() == ReviewStatus.PENDING && review.blocking());
        if (noBlockingPending && attempts.stream().anyMatch(attempt -> attempt.countedAsEvidence() && !attempt.passed())) {
            return new Clearance(PracticeStatus.PASSED, PracticePassReason.ATTEMPT_NOT_BLOCKED);
        }
```
- Update the class Javadoc: add one sentence describing `ATTEMPT_NOT_BLOCKED` and that it is never stored.

### 3. `main/java/.../application/service/PracticeProgress.java`

- In `derive(...)`: `new PracticeClearance.ReviewFact(review.status(), review.skill(), review.blocking())`.
- In `persistPassed(...)`: store only strong reasons:

```java
        states.forEach((id, state) -> {
            // The soft reason is derived on every read: storing it would make it sticky and break a rollback.
            if (state.status() == PracticeStatus.PASSED && state.reason() != PracticePassReason.ATTEMPT_NOT_BLOCKED) {
                newlyPassed.put(id, state.reason());
            }
        });
```

### 4. `main/java/.../infrastructure/persistence/JdbcPracticeAttemptRepository.java`, `findForTopic`

Load every counted, submitted lesson attempt (failed ones included): delete the line
`AND (passed = TRUE OR cardinality(passed_skills) > 0)` from the `'PASS'` select. Nothing else changes (free-practice
attempts have `lesson_id IS NULL` by constraint, so `lesson_id IN (...)` already excludes them).

### 5. `main/java/.../domain/repository/PracticeAttemptRepository.java`

Javadoc of `FirstPass`: "A counted first submission of a lesson set, passed or not; `passedSkills` lists the skills it
passed." No signature change.

## Tests first

`test/java/.../domain/service/PracticeClearanceTest.java` (exists; reuse its `mixed(...)` and `passed(...)` helpers;
note `passed(set, skills)` builds a counted attempt with `passedSkills = skills`). Add:

```java
    private PracticeClearance.Clearance reading(List<PracticeClearance.AttemptFact> attempts,
                                                List<PracticeClearance.ReviewFact> reviews) {
        return rule.derive(true, Map.of(readingSet, R), attempts, Set.of(readingSet), reviews, null);
    }

    private PracticeClearance.AttemptFact failed(UUID set) {
        return new PracticeClearance.AttemptFact(set, false, true, Set.of());
    }

    private static PracticeClearance.ReviewFact pending(boolean blocking) {
        return new PracticeClearance.ReviewFact(ReviewStatus.PENDING, LearningSkill.READING, blocking);
    }

    @Test
    void aFailedFirstTryWithNoReviewClearsThePractice() {
        var result = reading(List.of(failed(readingSet)), List.of());
        assertEquals(PracticeStatus.PASSED, result.status());
        assertEquals(PracticePassReason.ATTEMPT_NOT_BLOCKED, result.reason());
    }

    @Test
    void aFailedFirstTryWithOnlyASuggestionPendingClearsThePractice() {
        assertEquals(PracticePassReason.ATTEMPT_NOT_BLOCKED,
                reading(List.of(failed(readingSet)), List.of(pending(false))).reason());
    }

    @Test
    void aPendingBlockingReviewKeepsThePracticeRequired() {
        assertEquals(PracticeStatus.REQUIRED, reading(List.of(failed(readingSet)), List.of(pending(true))).status());
        assertEquals(PracticeStatus.REQUIRED, reading(List.of(failed(readingSet)), List.of(
                new PracticeClearance.ReviewFact(ReviewStatus.DONE, LearningSkill.READING), pending(true))).status());
    }

    @Test
    void aFinishedReviewNextToASuggestionClearsSoftly() {
        assertEquals(PracticePassReason.ATTEMPT_NOT_BLOCKED, reading(List.of(failed(readingSet)), List.of(
                new PracticeClearance.ReviewFact(ReviewStatus.DONE, LearningSkill.READING), pending(false))).reason());
    }

    @Test
    void withoutAnAttemptNothingClears() {
        assertEquals(PracticeStatus.REQUIRED, reading(List.of(), List.of()).status());
    }

    @Test
    void aMixedSetWithListeningFailedAndReadingPassedTakesTheWeakerReason() {
        var result = mixed(List.of(passed(mixedSet, R)), Set.of(mixedSet), List.of(
                new PracticeClearance.ReviewFact(ReviewStatus.PENDING, LearningSkill.LISTENING, false)));
        assertEquals(PracticeStatus.PASSED, result.status());
        assertEquals(PracticePassReason.ATTEMPT_NOT_BLOCKED, result.reason());
    }

    @Test
    void setsWithoutSkillsClearSoftlyAfterAFailedTry() {
        var result = rule.derive(true, Map.of(readingSet, Set.of()), List.of(failed(readingSet)), Set.of(),
                List.of(new PracticeClearance.ReviewFact(ReviewStatus.PENDING, null, false)), null);
        assertEquals(PracticePassReason.ATTEMPT_NOT_BLOCKED, result.reason());
    }
```

The integration tests of phase 3 (A, B, C) check the stored-nothing rule
(`lesson_practice_passes` count 0) and the gate end to end.

## Verify

```bash
mvn -q -pl services/learning-service -am test -Dtest='PracticeClearanceTest,PracticeGatedLessonUnlockIntegrationTest,PracticeIntegrationTest,MultiSkillLessonIntegrationTest,RemediationLadderIntegrationTest,LessonLearningWebMvcTest' -Dsurefire.failIfNoSpecifiedTests=false
mvn -q -pl services/learning-service -am test
```

## Success Criteria

- [ ] New clearance tests and all phase 3 tests pass; existing `PracticeClearanceTest` cases unchanged and green.
- [ ] No migration file; `lesson_practice_passes` untouched.
- [ ] Whole learning-service suite green.

## Risk Assessment

The gate becomes soft by design (Red Team finding 2); phase 6 records how to measure it.
