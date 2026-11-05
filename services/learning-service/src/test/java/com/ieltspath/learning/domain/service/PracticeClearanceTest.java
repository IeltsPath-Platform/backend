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

    private PracticeClearance.Clearance mixed(List<PracticeClearance.AttemptFact> attempts, Set<UUID> revealed,
                                              List<PracticeClearance.ReviewFact> reviews) {
        return rule.derive(true, Map.of(readingSet, R, listeningSet, L, mixedSet, RL, writingSet, W), attempts,
                revealed, reviews, null);
    }

    private PracticeClearance.AttemptFact passed(UUID set, Set<LearningSkill> skills) {
        return new PracticeClearance.AttemptFact(set, false, true, skills);
    }

    private PracticeClearance.Clearance reading(List<PracticeClearance.AttemptFact> attempts,
                                                List<PracticeClearance.ReviewFact> reviews) {
        // No set is revealed here so ALL_SETS_ATTEMPTED cannot mask the failed-attempt rule under test.
        return rule.derive(true, Map.of(readingSet, R), attempts, Set.of(), reviews, null);
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

    @Test
    void aMixedSetPassingBothSkillsClearsThePractice() {
        var result = mixed(List.of(passed(mixedSet, RL)), Set.of(mixedSet), List.of());
        assertEquals(PracticeStatus.PASSED, result.status());
        assertEquals(PracticePassReason.FIRST_SUBMISSION, result.reason());
    }

    @Test
    void readingAloneLeavesListeningRequiredUntilAListeningPartPasses() {
        assertEquals(PracticeStatus.REQUIRED, mixed(List.of(passed(readingSet, R)), Set.of(readingSet), List.of()).status());
        // Listening was attempted in the mixed set and not passed: it is soft-cleared unless a blocking review waits.
        assertEquals(PracticePassReason.ATTEMPT_NOT_BLOCKED,
                mixed(List.of(passed(mixedSet, R)), Set.of(mixedSet), List.of()).reason());
        assertEquals(PracticeStatus.REQUIRED, mixed(List.of(passed(mixedSet, R)), Set.of(mixedSet), List.of(
                new PracticeClearance.ReviewFact(ReviewStatus.PENDING, LearningSkill.LISTENING, true))).status());
        assertEquals(PracticeStatus.PASSED, mixed(List.of(passed(readingSet, R), passed(listeningSet, L)),
                Set.of(readingSet, listeningSet), List.of()).status());
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
    void aSkillClearedByAFinishedReviewKeepsTheWeakerReason() {
        var result = mixed(List.of(passed(readingSet, R)), Set.of(readingSet),
                List.of(new PracticeClearance.ReviewFact(ReviewStatus.SKIPPED, LearningSkill.LISTENING)));
        assertEquals(PracticeStatus.PASSED, result.status());
        assertEquals(PracticePassReason.REVIEW_FINISHED, result.reason());
        var otherSkill = mixed(List.of(passed(readingSet, R)), Set.of(readingSet),
                List.of(new PracticeClearance.ReviewFact(ReviewStatus.DONE, LearningSkill.READING)));
        assertEquals(PracticeStatus.REQUIRED, otherSkill.status());
    }

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
    void aSkillWaitsForEveryOneOfItsReviewsNotJustTheFirstFinishedOne() {
        var oneDoneOnePending = List.of(new PracticeClearance.ReviewFact(ReviewStatus.DONE, LearningSkill.READING),
                new PracticeClearance.ReviewFact(ReviewStatus.PENDING, LearningSkill.READING));
        assertEquals(PracticeStatus.REQUIRED, rule.derive(true, Map.of(readingSet, R), List.of(), Set.of(),
                oneDoneOnePending, null).status());

        var bothDone = List.of(new PracticeClearance.ReviewFact(ReviewStatus.DONE, LearningSkill.READING),
                new PracticeClearance.ReviewFact(ReviewStatus.DONE, LearningSkill.READING));
        var cleared = rule.derive(true, Map.of(readingSet, R), List.of(), Set.of(), bothDone, null);
        assertEquals(PracticeStatus.PASSED, cleared.status());
        assertEquals(PracticePassReason.REVIEW_FINISHED, cleared.reason());

        var doneAndSkipped = List.of(new PracticeClearance.ReviewFact(ReviewStatus.DONE, LearningSkill.READING),
                new PracticeClearance.ReviewFact(ReviewStatus.SKIPPED, LearningSkill.READING));
        assertEquals(PracticeStatus.PASSED, rule.derive(true, Map.of(readingSet, R), List.of(), Set.of(),
                doneAndSkipped, null).status());
    }

    @Test
    void aPendingReviewOfAnotherSkillDoesNotHoldBackAClearedSkill() {
        var reviews = List.of(new PracticeClearance.ReviewFact(ReviewStatus.DONE, LearningSkill.READING),
                new PracticeClearance.ReviewFact(ReviewStatus.PENDING, LearningSkill.LISTENING));
        assertEquals(PracticeStatus.PASSED, rule.derive(true, Map.of(readingSet, R), List.of(), Set.of(), reviews,
                null).status());
    }

    @Test
    void withoutSkillsAPendingReviewKeepsThePracticeRequiredEvenWhenAnotherReviewIsDone() {
        var mixedStates = List.of(new PracticeClearance.ReviewFact(ReviewStatus.DONE),
                new PracticeClearance.ReviewFact(ReviewStatus.PENDING));
        assertEquals(PracticeStatus.REQUIRED,
                rule.derive(true, Set.of(set), List.of(), Set.of(), mixedStates, null).status());

        var allFinished = List.of(new PracticeClearance.ReviewFact(ReviewStatus.DONE),
                new PracticeClearance.ReviewFact(ReviewStatus.SKIPPED));
        assertEquals(PracticePassReason.REVIEW_FINISHED,
                rule.derive(true, Set.of(set), List.of(), Set.of(), allFinished, null).reason());
    }

    @Test
    void aFirstSubmissionPassStillWinsWhileOtherSkillsReviewsAreOpen() {
        var result = rule.derive(true, Map.of(readingSet, R), List.of(passed(readingSet, R)), Set.of(readingSet),
                List.of(new PracticeClearance.ReviewFact(ReviewStatus.PENDING, LearningSkill.READING)), null);
        assertEquals(PracticePassReason.FIRST_SUBMISSION, result.reason());
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
