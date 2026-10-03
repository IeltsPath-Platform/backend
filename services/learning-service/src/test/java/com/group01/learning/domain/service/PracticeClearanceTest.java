package com.group01.learning.domain.service;

import com.group01.learning.domain.vo.PracticePassReason;
import com.group01.learning.domain.vo.PracticeStatus;
import com.group01.learning.domain.vo.ReviewStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PracticeClearanceTest {
    private final PracticeClearance rule = new PracticeClearance();
    private final UUID set = UUID.randomUUID();

    @Test
    void firstCountedPassingSubmissionWins() {
        var result = rule.derive(true, Set.of(set),
                List.of(new PracticeClearance.AttemptFact(set, true, true)), Set.of(set), List.of(), null);
        assertEquals(PracticeStatus.PASSED, result.status());
        assertEquals(PracticePassReason.FIRST_SUBMISSION, result.reason());
        assertEquals(PracticeStatus.REQUIRED, rule.derive(true, Set.of(set),
                List.of(new PracticeClearance.AttemptFact(set, true, false)), Set.of(), List.of(), null).status());
    }

    @Test
    void finishedPracticeReviewPassesLesson() {
        var result = rule.derive(true, Set.of(set), List.of(), Set.of(),
                List.of(new PracticeClearance.ReviewFact(ReviewStatus.SKIPPED)), null);
        assertEquals(PracticePassReason.REVIEW_FINISHED, result.reason());
    }

    @Test
    void allRevealedSetsPassOnlyAfterPracticeReviewsFinish() {
        var pending = List.of(new PracticeClearance.ReviewFact(ReviewStatus.PENDING));
        assertEquals(PracticeStatus.REQUIRED,
                rule.derive(true, Set.of(set), List.of(), Set.of(set), pending, null).status());
        assertEquals(PracticePassReason.ALL_SETS_ATTEMPTED,
                rule.derive(true, Set.of(set), List.of(), Set.of(set), List.of(), null).reason());
    }

    @Test
    void emptyCatalogPassesAfterLessonCompletion() {
        assertEquals(PracticeStatus.LOCKED,
                rule.derive(false, Set.of(), List.of(), Set.of(), List.of(), null).status());
        assertEquals(PracticePassReason.NO_PRACTICE,
                rule.derive(true, Set.of(), List.of(), Set.of(), List.of(), null).reason());
    }

    @Test
    void storedPassWinsAfterCatalogChanges() {
        var result = rule.derive(true, Set.of(set), List.of(), Set.of(), List.of(),
                PracticePassReason.ALL_SETS_ATTEMPTED);
        assertEquals(PracticeStatus.PASSED, result.status());
        assertEquals(PracticePassReason.ALL_SETS_ATTEMPTED, result.reason());
    }
}
