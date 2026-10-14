package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticePassReason;
import com.ieltspath.learning.domain.vo.PracticeStatus;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PracticeClearanceTest {
    private final PracticeClearance rule = new PracticeClearance();
    private final UUID set = UUID.randomUUID();

    private static final Set<LearningSkill> R = Set.of(LearningSkill.READING);
    private static final Set<LearningSkill> L = Set.of(LearningSkill.LISTENING);
    private static final Set<LearningSkill> W = Set.of(LearningSkill.WRITING);
    private static final Set<LearningSkill> RL = Set.of(LearningSkill.READING, LearningSkill.LISTENING);
    private final UUID readingSet = UUID.randomUUID();
    private final UUID listeningSet = UUID.randomUUID();
    private final UUID mixedSet = UUID.randomUUID();
    private final UUID writingSet = UUID.randomUUID();
    private final Set<UUID> required = Set.of(readingSet, listeningSet, mixedSet);

    private PracticeClearance.Clearance mixed(List<PracticeClearance.AttemptFact> attempts, Set<UUID> revealed,
                                              List<PracticeClearance.ReviewFact> reviews) {
        return rule.derive(true, Map.of(readingSet, R, listeningSet, L, mixedSet, RL, writingSet, W), attempts,
                revealed, reviews, null);
    }

    private PracticeClearance.AttemptFact passed(UUID set, Set<LearningSkill> skills) {
        return new PracticeClearance.AttemptFact(set, false, true, skills);
    }

    private PracticeClearance.AttemptFact failed(UUID set) {
        return new PracticeClearance.AttemptFact(set, false, true, Set.of());
    }

    private static PracticeClearance.ReviewFact review(ReviewStatus status) {
        return new PracticeClearance.ReviewFact(status, LearningSkill.READING);
    }

    @Test
    void oneSetPassingBothSkillsDoesNotClearALessonWithMoreSets() {
        var result = mixed(List.of(passed(mixedSet, RL)), Set.of(mixedSet), List.of());
        assertEquals(PracticeStatus.REQUIRED, result.status());
        assertEquals(PracticeStatus.REQUIRED,
                mixed(List.of(passed(readingSet, R), passed(listeningSet, L), passed(mixedSet, RL)),
                        Set.of(readingSet, listeningSet), List.of()).status(), "the mixed set was not submitted");
    }

    @Test
    void everyRequiredSetPassedAtFirstSubmissionClearsThePractice() {
        var result = mixed(List.of(passed(readingSet, R), passed(listeningSet, L), passed(mixedSet, RL)),
                required, List.of());
        assertEquals(PracticeStatus.PASSED, result.status());
        assertEquals(PracticePassReason.FIRST_SUBMISSION, result.reason());
    }

    @Test
    void aSetThatPassedOnlyOneOfItsSkillsDoesNotCountAsPassed() {
        var result = mixed(List.of(passed(readingSet, R), passed(listeningSet, L), passed(mixedSet, R)),
                required, List.of());
        assertEquals(PracticeStatus.PASSED, result.status());
        assertEquals(PracticePassReason.ALL_SETS_ATTEMPTED, result.reason());
    }

    @Test
    void everySetSubmittedWithNoReviewToDoClearsThePracticeEvenIfOneFailed() {
        // A failed set creates a review unless no other set is left to review with, so none pending means none needed.
        var result = mixed(List.of(passed(readingSet, R), passed(listeningSet, L), failed(mixedSet)), required,
                List.of());
        assertEquals(PracticeStatus.PASSED, result.status());
        assertEquals(PracticePassReason.ALL_SETS_ATTEMPTED, result.reason());
    }

    @Test
    void aSetNotYetSubmittedKeepsThePracticeRequired() {
        assertEquals(PracticeStatus.REQUIRED, mixed(List.of(), Set.of(), List.of()).status());
        assertEquals(PracticeStatus.REQUIRED, mixed(List.of(passed(readingSet, R)), Set.of(readingSet),
                List.of()).status());
    }

    @Test
    void writingSetsAreOptionalAndAWritingOnlyPracticeIsNoPractice() {
        var readingAndWriting = rule.derive(true, Map.of(readingSet, R, writingSet, W),
                List.of(passed(readingSet, R)), Set.of(readingSet), List.of(), null);
        assertEquals(PracticeStatus.PASSED, readingAndWriting.status());
        var writingOnly = rule.derive(true, Map.of(writingSet, W), List.of(), Set.of(), List.of(), null);
        assertEquals(PracticePassReason.NO_PRACTICE, writingOnly.reason());
    }

    @Test
    void aPendingReviewKeepsThePracticeRequiredUntilEveryReviewIsFinished() {
        var everySetDone = List.of(passed(readingSet, R), passed(listeningSet, L), failed(mixedSet));
        assertEquals(PracticeStatus.REQUIRED,
                mixed(everySetDone, required, List.of(review(ReviewStatus.PENDING))).status());
        assertEquals(PracticeStatus.REQUIRED, mixed(everySetDone, required,
                List.of(review(ReviewStatus.DONE), review(ReviewStatus.PENDING))).status());
        var finished = mixed(everySetDone, required,
                List.of(review(ReviewStatus.DONE), review(ReviewStatus.SKIPPED)));
        assertEquals(PracticeStatus.PASSED, finished.status());
        assertEquals(PracticePassReason.REVIEW_FINISHED, finished.reason());
    }

    @Test
    void firstCountedPassingSubmissionOfASetWithoutSkillsCountsAsPassed() {
        var result = rule.derive(true, Set.of(set),
                List.of(new PracticeClearance.AttemptFact(set, true, true)), Set.of(set), List.of(), null);
        assertEquals(PracticeStatus.PASSED, result.status());
        assertEquals(PracticePassReason.FIRST_SUBMISSION, result.reason());
        var notCounted = rule.derive(true, Set.of(set),
                List.of(new PracticeClearance.AttemptFact(set, true, false)), Set.of(set), List.of(), null);
        assertEquals(PracticePassReason.ALL_SETS_ATTEMPTED, notCounted.reason());
        assertEquals(PracticeStatus.REQUIRED, rule.derive(true, Set.of(set),
                List.of(new PracticeClearance.AttemptFact(set, true, false)), Set.of(), List.of(), null).status());
    }

    @Test
    void setsWithoutSkillsWaitForEveryReview() {
        var donePending = List.of(new PracticeClearance.ReviewFact(ReviewStatus.DONE),
                new PracticeClearance.ReviewFact(ReviewStatus.PENDING));
        assertEquals(PracticeStatus.REQUIRED,
                rule.derive(true, Set.of(set), List.of(), Set.of(set), donePending, null).status());
        var finished = rule.derive(true, Set.of(set), List.of(), Set.of(set),
                List.of(new PracticeClearance.ReviewFact(ReviewStatus.SKIPPED)), null);
        assertEquals(PracticePassReason.REVIEW_FINISHED, finished.reason());
    }

    @Test
    void aLessonWithoutRequiredSetsIsNoPracticeEvenBeforeItOpens() {
        assertEquals(PracticePassReason.NO_PRACTICE,
                rule.derive(false, Set.of(), List.of(), Set.of(), List.of(), null).reason());
        assertEquals(PracticePassReason.NO_PRACTICE,
                rule.derive(true, Set.of(), List.of(), Set.of(), List.of(), null).reason());
        assertEquals(PracticePassReason.NO_PRACTICE,
                rule.derive(false, Map.of(writingSet, W), List.of(), Set.of(), List.of(), null).reason());
    }

    @Test
    void practiceOpensWithTheLessonNotWithItsCompletion() {
        assertEquals(PracticeStatus.LOCKED,
                rule.derive(false, Set.of(set), List.of(), Set.of(), List.of(), null).status());
        assertEquals(PracticeStatus.REQUIRED,
                rule.derive(true, Set.of(set), List.of(), Set.of(), List.of(), null).status());
    }

    @Test
    void storedPassWinsAfterCatalogChanges() {
        var result = rule.derive(true, Set.of(set), List.of(), Set.of(), List.of(),
                PracticePassReason.ALL_SETS_ATTEMPTED);
        assertEquals(PracticeStatus.PASSED, result.status());
        assertEquals(PracticePassReason.ALL_SETS_ATTEMPTED, result.reason());
    }
}
