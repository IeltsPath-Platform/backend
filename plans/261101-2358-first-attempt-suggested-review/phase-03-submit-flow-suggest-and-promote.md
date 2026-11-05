---
phase: 3
title: "Submit flow: suggest and promote"
status: pending
priority: P1
effort: "4h"
dependencies: [1, 2]
---

# Phase 3: Submit flow: suggest and promote

Work package B together with phase 4 (ship both in one commit set; tests below are red until phase 4 is in).
Repo: backend. Paths under `services/learning-service/src/`.

## Overview

The first counted failure creates non-blocking suggestions. A KP weak again while a suggestion (`PRACTICE` or
`ERROR_PATTERN`) of it is pending gets that review promoted. The response says which reviews block.

## Changes

### 1. `main/java/.../domain/vo/PracticeSubmission.java`

```java
    public record ReviewCreated(UUID reviewId, UUID knowledgePointId, String stage, boolean blocking) {
        public ReviewCreated(UUID reviewId, UUID knowledgePointId, String stage) {
            this(reviewId, knowledgePointId, stage, true);
        }
    }
```

### 2. `main/java/.../api/dto/response/PracticeSubmissionResponse.java`

`public record ReviewCreated(UUID reviewId, UUID knowledgePointId, String stage, boolean blocking) {}` and map
`review.blocking()` in `from(...)`.

### 3. `main/java/.../application/usecase/SubmitPracticeAttemptUseCase.java`

Add `private static final Set<String> SUGGESTION_TRIGGERS = Set.of("PRACTICE", "ERROR_PATTERN");`.

Replace the block that starts at `List<PracticeReviewCandidate> candidates = List.of();` (line ~191) and ends with the
`created` list (line ~219) by:

```java
        List<PracticeReviewCandidate> candidates = List.of();
        List<PromotedReview> promoted = List.of();
        if (counted && !graded.passed()) {
            Set<UUID> kps = new HashSet<>();
            for (var item : outcomes) kps.addAll(item.knowledgePointIds());
            Set<UUID> excluded = new HashSet<>(revealed);
            excluded.add(attempt.packageId());
            Map<UUID, Integer> availability = kps.isEmpty() ? Map.of() : content.practiceSetAvailability(
                    kps.stream().sorted(Comparator.comparing(UUID::toString)).toList(),
                    excluded.stream().sorted(Comparator.comparing(UUID::toString)).toList(), MIN_SET_QUESTIONS);
            Set<UUID> blockingKps = new HashSet<>();
            Set<UUID> suggestionKps = new HashSet<>();
            Set<UUID> otherOptionalKps = new HashSet<>();
            for (PendingReview review : reviews.findPending(userId)) {
                if (review.blocking()) blockingKps.add(review.knowledgePointId());
                else if (SUGGESTION_TRIGGERS.contains(review.triggerKind())) suggestionKps.add(review.knowledgePointId());
                else otherOptionalKps.add(review.knowledgePointId());
            }
            var needs = reviewNeeds(graded, outcomes, outcomesBySkill, blockingKps, availability);
            // Weak again while the system's suggestion is still pending: that review now blocks.
            Set<UUID> repeatKps = needs.stream().map(PracticeReviewRule.Need::knowledgePointId)
                    .filter(suggestionKps::contains).collect(Collectors.toSet());
            promoted = reviews.promoteSuggestionsToBlocking(userId, repeatKps);
            // First weakness: a suggestion. A point holding a review the learner picked gets no second review.
            var fresh = needs.stream().filter(need -> !suggestionKps.contains(need.knowledgePointId())
                    && !otherOptionalKps.contains(need.knowledgePointId())).toList();
            Set<UUID> wrongInLesson = fresh.isEmpty() ? Set.of() : mistakes.of(userId, lesson);
            Map<UUID, LearningSkill> kpSkills = new HashMap<>();
            if (!fresh.isEmpty()) evidence.findMasteryHistories(userId).stream().filter(history -> history.skill() != null)
                    .forEach(history -> kpSkills.put(history.knowledgePointId(), history.skill()));
            candidates = fresh.stream().map(need -> {
                var start = ReviewRule.initialStage(need.kpPercent(), wrongInLesson.contains(need.knowledgePointId()));
                return new PracticeReviewCandidate(UUID.randomUUID(), attempt.lessonId(), need.knowledgePointId(),
                        kpSkills.getOrDefault(need.knowledgePointId(), attempt.skill()), attempt.id(), need.kpPercent(),
                        start.stage(), start.theoryReason(), false);
            }).toList();
        }
        String transcript = ItemGrading.orderedSections(version).stream().filter(section -> section.audio() != null)
                .map(section -> section.audio().transcript()).findFirst().orElse(null);
        List<PracticeSubmission.ReviewCreated> created = new ArrayList<>();
        candidates.forEach(candidate -> created.add(new PracticeSubmission.ReviewCreated(candidate.reviewId(),
                candidate.knowledgePointId(), candidate.stage().name(), false)));
        promoted.forEach(review -> created.add(new PracticeSubmission.ReviewCreated(review.reviewId(),
                review.knowledgePointId(), review.stage().name(), true)));
```

Then, further down:
- `PracticeSubmission response = new PracticeSubmission(..., created, ...)` keeps using `created` (wrap as
  `List.copyOf(created)` if the record requires an immutable list).
- Keep `reviews.insertPracticePending(userId, candidates);`.
- **Delete** the two lines `reviews.promoteToBlocking(userId, candidates.stream()... , "PRACTICE");`.

Imports: `PromotedReview`, `PendingReview`, `java.util.stream.Collectors` if not present.

The learner lock taken at the start of `execute` (line ~118) and `@Transactional` (line ~116) cover the promotion.

## Tests first (with phase 4 they all turn green)

### A. `test/java/.../infrastructure/persistence/PracticeGatedLessonUnlockIntegrationTest.java`

Imports to add: `PracticePassReason`, `PracticeSubmission`.

1. **Replace** the method `aFailedPracticeSendsTheLearnerToTheReviewAndTheSecondLessonOpensWhenItIsDone` (line ~250)
   with:

```java
    @Test
    void aFailedFirstPracticeSuggestsAReviewAndKeepsTheSecondLessonOpen() {
        UUID user = UUID.randomUUID();
        PackageVersion first = practiceSet(kpA, kpA, kpA, kpA);
        PackageVersion second = practiceSet(kpA, kpA);
        topics.execute(user);
        exercise.execute(user, lesson1, block1, answers(List.of(question1a, question1b), "A", "A"));
        var attempt = start.execute(user, lesson1, first.packageId());
        var submitted = practice.execute(user, attempt.attemptId(), answers(items(first), "A", "A", "B", "B"));

        assertFalse(submitted.passed());
        var created = submitted.reviewsCreated().getFirst();
        assertFalse(created.blocking());
        assertEquals("PRACTICE", created.stage());
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?",
                Boolean.class, created.reviewId()));
        assertEquals(List.of("COMPLETED", "AVAILABLE"), statuses(user));
        assertDoesNotThrow(() -> getLesson.execute(user, lesson2));
        assertEquals(PracticePassReason.ATTEMPT_NOT_BLOCKED, practiceSets.execute(user, lesson1).practicePassReason());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM lesson_practice_passes WHERE user_id = ?",
                Integer.class, user));

        var review = getReview.execute(user, created.reviewId());
        assertEquals(second.packageId(), review.set().packageId());
        var done = submitReview.execute(user, created.reviewId(), new SubmitReviewCommand(review.set().reviewSetId(),
                UUID.randomUUID(), answers(items(second), "A", "A").answers()));
        assertEquals("DONE", done.reviewStatus());
        assertEquals(List.of("COMPLETED", "AVAILABLE"), statuses(user));
    }
```

2. **Add**:

```java
    @Test
    void theSamePointWeakAgainInAnotherSetTurnsTheSuggestionIntoABlockingReview() {
        UUID user = UUID.randomUUID();
        PackageVersion first = practiceSet(kpA, kpA, kpA, kpA);
        PackageVersion second = practiceSet(kpA, kpA, kpA, kpA);
        PackageVersion third = practiceSet(kpA, kpA);
        topics.execute(user);
        exercise.execute(user, lesson1, block1, answers(List.of(question1a, question1b), "A", "A"));
        var suggested = practice.execute(user, start.execute(user, lesson1, first.packageId()).attemptId(),
                answers(items(first), "A", "A", "B", "B")).reviewsCreated().getFirst();
        var again = practice.execute(user, start.execute(user, lesson1, second.packageId()).attemptId(),
                answers(items(second), "A", "A", "B", "B"));

        assertEquals(List.of(suggested.reviewId()), again.reviewsCreated().stream()
                .map(PracticeSubmission.ReviewCreated::reviewId).toList());
        assertTrue(again.reviewsCreated().getFirst().blocking());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ?", Integer.class, user));
        assertEquals("REVIEW_REQUIRED", assertThrows(LearningGateException.class,
                () -> getLesson.execute(user, lesson2)).getCode());
        assertEquals(List.of("COMPLETED", "LOCKED"), statuses(user));

        var review = getReview.execute(user, suggested.reviewId());
        assertEquals(third.packageId(), review.set().packageId());
        assertEquals("DONE", submitReview.execute(user, suggested.reviewId(), new SubmitReviewCommand(
                review.set().reviewSetId(), UUID.randomUUID(), answers(items(third), "A", "A").answers())).reviewStatus());
        assertEquals(List.of("COMPLETED", "AVAILABLE"), statuses(user));
    }

    @Test
    void failingTheSetOfASuggestedReviewMakesItBlockingAtTheTheoryStage() {
        UUID user = UUID.randomUUID();
        PackageVersion first = practiceSet(kpA, kpA, kpA, kpA);
        PackageVersion second = practiceSet(kpA, kpA);
        topics.execute(user);
        exercise.execute(user, lesson1, block1, answers(List.of(question1a, question1b), "A", "A"));
        UUID reviewId = practice.execute(user, start.execute(user, lesson1, first.packageId()).attemptId(),
                answers(items(first), "A", "A", "B", "B")).reviewsCreated().getFirst().reviewId();

        var review = getReview.execute(user, reviewId);
        var failed = submitReview.execute(user, reviewId, new SubmitReviewCommand(review.set().reviewSetId(),
                UUID.randomUUID(), answers(items(second), "B", "B").answers()));

        assertEquals("PENDING", failed.reviewStatus());
        assertEquals("THEORY", failed.stage());
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?",
                Boolean.class, reviewId));
        assertEquals("REVIEW_REQUIRED", assertThrows(LearningGateException.class,
                () -> getLesson.execute(user, lesson2)).getCode());
    }

    @Test
    void aFailedPracticeWithoutTwoWrongAnswersOnAPointCreatesNoReviewAndOpensTheSecondLesson() {
        UUID user = UUID.randomUUID();
        PackageVersion first = practiceSet(kpA, kpA);
        topics.execute(user);
        exercise.execute(user, lesson1, block1, answers(List.of(question1a, question1b), "A", "A"));
        var submitted = practice.execute(user, start.execute(user, lesson1, first.packageId()).attemptId(),
                answers(items(first), "A", "B"));

        assertFalse(submitted.passed());
        assertTrue(submitted.reviewsCreated().isEmpty());
        assertEquals(List.of("COMPLETED", "AVAILABLE"), statuses(user));
        assertEquals(PracticePassReason.ATTEMPT_NOT_BLOCKED, practiceSets.execute(user, lesson1).practicePassReason());
    }

    @Test
    void aWeakPointPromotesItsPendingErrorPatternReviewButLeavesABasketReviewOptional() {
        UUID user = UUID.randomUUID();
        PackageVersion first = practiceSet(kpA, kpA, kpB, kpB);
        practiceSet(kpA, kpB);
        topics.execute(user);
        exercise.execute(user, lesson1, block1, answers(List.of(question1a, question1b), "A", "A"));
        UUID errorReview = UUID.randomUUID();
        UUID basketReview = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, trigger_kind, "
                + "blocking, stage) VALUES (?, ?, ?, ?, 'PENDING', 'READING', 'ERROR_PATTERN', false, 'PRACTICE')",
                errorReview, user, kpA, lesson1);
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, trigger_kind, "
                + "blocking, stage) VALUES (?, ?, ?, ?, 'PENDING', 'READING', 'CONSOLIDATE', false, 'PRACTICE')",
                basketReview, user, kpB, lesson1);

        var submitted = practice.execute(user, start.execute(user, lesson1, first.packageId()).attemptId(),
                answers(items(first), "B", "B", "B", "B"));

        assertEquals(List.of(errorReview), submitted.reviewsCreated().stream()
                .map(PracticeSubmission.ReviewCreated::reviewId).toList());
        assertTrue(submitted.reviewsCreated().getFirst().blocking());
        assertEquals("ERROR_PATTERN", jdbc.queryForObject("SELECT trigger_kind FROM review_items WHERE id = ?",
                String.class, errorReview));
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?",
                Boolean.class, basketReview));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ?", Integer.class, user));
        assertEquals("REVIEW_REQUIRED", assertThrows(LearningGateException.class,
                () -> getLesson.execute(user, lesson2)).getCode());
    }
```

3. **Change** `aReviewThatRunsOutOfPackagesEndsAndOpensTheSecondLesson` (line ~278): rename to
   `aSuggestedReviewThatRunsOutOfPackagesEndsWithoutHoldingTheSecondLesson`; answers `"B", "B"` instead of `"A", "B"`;
   replace the `REVIEW_REQUIRED` assertion (lines ~286-287) with `assertDoesNotThrow(() -> getLesson.execute(user, lesson2));`.
   Keep the `SKIPPED` assertion and the final `statuses` / `getLesson` assertions.

### B. `test/java/.../infrastructure/persistence/PracticeIntegrationTest.java` (method `practiceEvidenceReviewAndTopicClearanceFollowPackageHistory`)

- Line ~161: answers `"B", "B"` instead of `"A", "B"`.
- Line ~162: `assertEquals(0.0, failedResult.percent());`.
- Lines ~167-169: replace the `REVIEW_REQUIRED` gate assertion with

```java
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?",
                Boolean.class, failedResult.reviewsCreated().getFirst().reviewId()));
        assertDoesNotThrow(() -> start.execute(user, secondLesson, sparePackage));
```
- Keep the rest (mark DONE, topic test assignment).

### C. `test/java/.../infrastructure/persistence/MultiSkillLessonIntegrationTest.java` (method `aMixedSetIsScoredPerSkillAndOnlyTheFailedSkillGoesToReview`)

The Listening part has 3 items answered `A, B, B` (1/3, 2 wrong), so the Listening KP is still weak. Replace the last
assertion (line ~184) with:

```java
        assertFalse(result.reviewsCreated().getFirst().blocking());
        var catalog = practiceSets.execute(USER, MIXED);
        assertEquals(PracticeStatus.PASSED, catalog.practiceStatus());
        assertEquals(PracticePassReason.ATTEMPT_NOT_BLOCKED, catalog.practicePassReason());
```

### D. `test/java/.../infrastructure/persistence/RemediationLadderIntegrationTest.java`

The first practice must create a review at the PRACTICE stage (KP between 40% and 70% with 2 wrong):
- `failedReviewSetLeadsToTheoryThenNewSetsAndEndsOnlyWhenNoUnrevealedPackageIsLeft` (line ~103 and ~110):
  `PackageVersion first = practiceSet(kpA, kpA, kpA, kpA, kpA);` and answers `"A", "A", "A", "B", "B"`. After the
  `failed` assertions (line ~124) add
  `assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT blocking FROM review_items WHERE id = ?", Boolean.class, reviewId));`.
- `aReviewKeepsGivingNewSetsAfterEveryFailureUntilOneIsPassed` (line ~169 and ~175): same two changes for `first`.
- `newReviewsStartWithTheoryForLowScoresAndMistakesMadeInTheLesson` (line ~209 and ~216):
  `PackageVersion mixed = practiceSet(kpA, kpA, kpA, kpA, kpB, kpB);` and answers `"A", "A", "B", "B", "B", "B"`
  (kpA 2/4 with the lesson mistake, kpB 0/2). The expected map stays `{kpA: WRONG_IN_LESSON, kpB: LOW_SCORE}`.

### E. `test/java/.../api/LessonLearningWebMvcTest.java`, add

A test `practiceSubmissionResponseSaysWhetherACreatedReviewBlocks`: stub
`submitPractice.execute(eq(USER), eq(attemptId), any())` to return
`new PracticeSubmission(attemptId, 1, 2, 0.5, false, true, List.of(), null, List.of(new PracticeSubmission.ReviewCreated(reviewId, kpId, "PRACTICE", false)))`;
`POST /api/learning/practice-attempts/{id}/submissions` with the request body shape of the controller's practice
submission DTO (open the controller method to copy it); assert `$.reviewsCreated[0].blocking` is `false` and
`$.reviewsCreated[0].stage` is `"PRACTICE"`. Use the class's `authenticated(...)` helper.

## Success Criteria

- [ ] All tests in A-E pass together with phase 4.
- [ ] No other existing test changes.
