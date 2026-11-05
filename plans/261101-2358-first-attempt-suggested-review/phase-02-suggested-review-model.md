---
phase: 2
title: "Suggested review model"
status: pending
priority: P1
effort: "3h"
dependencies: [1]
---

# Phase 2: Suggested review model

Work package A. Repo: backend. Paths are under `services/learning-service/src/main/java/com/ieltspath/learning/`
unless stated.

## Overview

A non-blocking review with `trigger_kind = PRACTICE` is a "suggestion": it can sit at the theory stage, a failed set
turns it blocking, and a repository method promotes suggestions (`PRACTICE` or `ERROR_PATTERN`) to blocking. Nothing
calls the new behavior yet; learners see no change after this phase.

## Changes

### 1. `domain/vo/PracticeReviewCandidate.java`

Add `boolean blocking` as the last component:

```java
public record PracticeReviewCandidate(UUID reviewId, UUID lessonId, UUID knowledgePointId, LearningSkill skill,
                                      UUID sourceAttemptId, double kpPercent, ReviewStage stage,
                                      TheoryReason theoryReason, boolean blocking) {}
```

Its only constructor call is `SubmitPracticeAttemptUseCase` line ~210: pass `true` there in this phase (phase 3
changes it to `false`).

### 2. `domain/vo/PracticeReviewState.java`

```java
public record PracticeReviewState(UUID lessonId, UUID knowledgePointId, ReviewStatus status, LearningSkill skill,
                                  boolean blocking) {
    public PracticeReviewState(UUID lessonId, UUID knowledgePointId, ReviewStatus status, LearningSkill skill) {
        this(lessonId, knowledgePointId, status, skill, true);
    }
    public PracticeReviewState(UUID lessonId, UUID knowledgePointId, ReviewStatus status) {
        this(lessonId, knowledgePointId, status, null, true);
    }
}
```

### 3. New `domain/vo/PromotedReview.java`

```java
/** A suggested review that a repeat weakness just made blocking. */
public record PromotedReview(UUID reviewId, UUID knowledgePointId, ReviewStage stage) {}
```

### 4. `domain/repository/ReviewItemRepository.java`

Add:

```java
    /**
     * Makes the learner's optional PENDING reviews of these knowledge points blocking when the system raised them
     * itself (trigger {@code PRACTICE} or {@code ERROR_PATTERN}); reviews the learner picked from the basket stay
     * optional. The trigger is kept. Returns the reviews it changed.
     */
    List<PromotedReview> promoteSuggestionsToBlocking(UUID userId, Collection<UUID> knowledgePointIds);
```

Leave `promoteToBlocking` as it is (the assessment path uses it).

### 5. `infrastructure/persistence/JdbcReviewItemRepository.java`

- `insertPracticePending`: add the column and the 9th parameter:

```java
                INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill,
                                          trigger_kind, source_attempt_id, stage, theory_reason, blocking)
                VALUES (?, ?, ?, ?, 'PENDING', ?, 'PRACTICE', ?, ?, ?, ?)
                ON CONFLICT (user_id, knowledge_point_id) WHERE status = 'PENDING' DO NOTHING
```
  and `statement.setBoolean(9, review.blocking());`.
- `findPracticeByLessons` (line ~65): select `blocking` too and build
  `new PracticeReviewState(lessonId, kpId, status, skill, row.getBoolean("blocking"))`.
- `findForTopic` (line ~95): the select already has `blocking`; pass `row.getBoolean("blocking")` as the 5th argument
  of `new PracticeReviewState(...)`.
- New method:

```java
    @Override
    public List<PromotedReview> promoteSuggestionsToBlocking(UUID userId, Collection<UUID> knowledgePointIds) {
        if (knowledgePointIds.isEmpty()) return List.of();
        return jdbc.query("""
                UPDATE review_items SET blocking = TRUE
                WHERE user_id = :userId AND status = 'PENDING' AND blocking = FALSE
                  AND trigger_kind IN ('PRACTICE', 'ERROR_PATTERN') AND knowledge_point_id IN (:kpIds)
                RETURNING id, knowledge_point_id, stage
                """, Map.of("userId", userId, "kpIds", knowledgePointIds), (row, index) -> new PromotedReview(
                row.getObject("id", UUID.class), row.getObject("knowledge_point_id", UUID.class),
                ReviewStage.valueOf(row.getString("stage"))));
    }
```
- `save(ReviewItem review)`: write the flag. Add `state.put("blocking", review.blocking());` and `blocking = :blocking`
  to the `UPDATE review_items SET ...` statement (keep the `WHERE id = :reviewId AND status = 'PENDING'`).

### 6. `domain/aggregate/ReviewItem.java`

- `private final boolean blocking;` becomes `private boolean blocking;`.
- Add `private static final String PRACTICE_TRIGGER = "PRACTICE";`.
- In `recordSetResult`, replace the failure branches with:

```java
        if (passed) {
            status = ReviewStatus.DONE;
        } else {
            // A suggested review from Practice that fails its set is weak for real: it becomes blocking and reads the
            // theory first, like any blocking review after its first failed set.
            if (!blocking && PRACTICE_TRIGGER.equals(triggerKind)) blocking = true;
            if (!blocking) {
                failedSets++;
                status = ReviewStatus.SKIPPED;
            } else if (++failedSets == 1) {
                stage = ReviewStage.THEORY;
                theoryReason = (double) correct / total < ReviewRule.LOW_SCORE ? TheoryReason.LOW_SCORE
                        : TheoryReason.SECOND_FAIL;
            }
        }
```
- Update the class Javadoc paragraph about optional reviews: add "A suggestion from Practice (optional, trigger
  `PRACTICE`) may start at the theory stage and becomes blocking when it fails a set."
- Keep `startWithTheory`'s guard as it is (suggestions get their stage when inserted, not through that method).

## Tests first

### `test/java/com/ieltspath/learning/domain/aggregate/ReviewItemTest.java` (exists), add:

```java
    private static ReviewItem optional(String trigger) {
        return ReviewItem.restore(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ReviewStatus.PENDING, null, 0, LearningSkill.READING, ReviewStage.PRACTICE, null, 0, trigger, false);
    }

    @Test
    void aSuggestionFromPracticeThatFailsItsSetBecomesBlockingAtTheTheoryStage() {
        ReviewItem review = optional("PRACTICE");
        var set = review.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertEquals(ReviewStatus.PENDING, review.recordSetResult(set.id(), UUID.randomUUID(), 1, 2));
        assertTrue(review.blocking());
        assertEquals(ReviewStage.THEORY, review.stage());
        assertEquals(TheoryReason.SECOND_FAIL, review.theoryReason());
        assertEquals(1, review.failedSets());
    }

    @Test
    void aSuggestionThatFailsBadlyReadsTheTheoryForTheLowScore() {
        ReviewItem review = optional("PRACTICE");
        var set = review.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        review.recordSetResult(set.id(), UUID.randomUUID(), 0, 2);
        assertEquals(TheoryReason.LOW_SCORE, review.theoryReason());
    }

    @Test
    void aSuggestionThatPassesIsDoneAndStaysOptional() {
        ReviewItem review = optional("PRACTICE");
        var set = review.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertEquals(ReviewStatus.DONE, review.recordSetResult(set.id(), UUID.randomUUID(), 2, 2));
        assertFalse(review.blocking());
    }

    @Test
    void aBasketReviewThatFailsIsStillSkipped() {
        ReviewItem review = optional("CONSOLIDATE");
        var set = review.assignSet(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertEquals(ReviewStatus.SKIPPED, review.recordSetResult(set.id(), UUID.randomUUID(), 0, 2));
        assertFalse(review.blocking());
    }
```

(If `ReviewSet` exposes its id under another accessor than `id()`, use that accessor.)

### New `test/java/com/ieltspath/learning/infrastructure/persistence/JdbcReviewItemRepositoryIntegrationTest.java`

Copy the class-level annotations, the PostgreSQL container field and the `@DynamicPropertySource properties(...)`
method from `PracticeGatedLessonUnlockIntegrationTest` (lines 1-58). Autowire `ReviewItemRepository reviews` and
`JdbcTemplate jdbc`. Insert rows with:

```java
    private UUID review(UUID user, UUID kp, String trigger, boolean blocking) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, trigger_kind, "
                + "blocking, stage) VALUES (?, ?, ?, ?, 'PENDING', 'READING', ?, ?, 'PRACTICE')",
                id, user, kp, UUID.randomUUID(), trigger, blocking);
        return id;
    }
```

Tests:
1. `insertPracticePendingStoresTheBlockingFlag`: insert two candidates (`blocking` false and true) with
   `reviews.insertPracticePending`; `SELECT blocking FROM review_items WHERE id = ?` returns false and true.
2. `promotionTouchesOnlyReviewsTheSystemRaised`: one user, four KPs: `PRACTICE`/false, `ERROR_PATTERN`/false,
   `CONSOLIDATE`/false, `PRACTICE`/true. Call `promoteSuggestionsToBlocking(user, all four)`. Returned list has exactly
   the first two (compare `reviewId` sets) with stage `PRACTICE`. Database: first two `blocking = true` with their
   original `trigger_kind`; the `CONSOLIDATE` one still false.
3. `promotingNothingReturnsNothing`: `promoteSuggestionsToBlocking(user, List.of())` returns an empty list.
4. `saveWritesTheBlockingFlag`: insert a `PRACTICE`/false review, load it with `reviews.findOwned(user, id)`,
   `assignSet`, `recordSetResult(..., 0, 2)`, `reviews.save(review)`; reload: `blocking() == true`,
   `stage() == THEORY`.
5. `practiceStatesCarryTheBlockingFlag`: insert a `PRACTICE`/false review for a known lesson id;
   `findPracticeByLessons(user, List.of(lessonId))` returns one state with `blocking() == false`.

## Verify

```bash
mvn -q -pl services/learning-service -am test -Dtest='ReviewItemTest,JdbcReviewItemRepositoryIntegrationTest,PracticeReviewRuleTest' -Dsurefire.failIfNoSpecifiedTests=false
mvn -q -pl services/learning-service -am test
```

## Success Criteria

- [ ] New tests pass; the whole learning-service suite passes with no edits to other tests (behavior unchanged).
