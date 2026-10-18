package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PendingReview;
import com.ieltspath.learning.domain.vo.TopicStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LessonAccessGateTest {
    private final LessonAccessGate gate = new LessonAccessGate();
    private final PendingReview review = new PendingReview(new UUID(0, 1), new UUID(0, 2), new UUID(0, 3));

    @Test
    void pendingReviewTakesPriorityOverTopicAndEarlierLesson() {
        LearningGateException error = assertThrows(LearningGateException.class,
                () -> gate.authorize(List.of(review), null, TopicStatus.LOCKED, false));

        assertThat(error.getCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(error.getReviews()).containsExactly(review);
        assertThat(error.getMessage()).isEqualTo("Complete the pending reviews first.");
        assertThatThrownBy(() -> error.getReviews().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aReviewBlocksOnlyLessonsThatTeachItsSkill() {
        var reading = new PendingReview(new UUID(0, 4), new UUID(0, 5), new UUID(0, 6), LearningSkill.READING);

        assertThat(assertThrows(LearningGateException.class, () -> gate.authorize(List.of(reading), null,
                Set.of(LearningSkill.READING, LearningSkill.LISTENING), TopicStatus.IN_PROGRESS, true)).getCode())
                .isEqualTo("REVIEW_REQUIRED");
        assertThatCode(() -> gate.authorize(List.of(reading), null, Set.of(LearningSkill.LISTENING),
                TopicStatus.IN_PROGRESS, true)).doesNotThrowAnyException();
    }

    @Test
    void aReviewWithoutASkillBlocksEveryLessonWhateverItTeaches() {
        assertThat(assertThrows(LearningGateException.class, () -> gate.authorize(List.of(review), null,
                Set.of(LearningSkill.LISTENING), TopicStatus.IN_PROGRESS, true)).getCode())
                .isEqualTo("REVIEW_REQUIRED");
        assertThat(assertThrows(LearningGateException.class, () -> gate.authorize(List.of(review), null,
                Set.<LearningSkill>of(), TopicStatus.IN_PROGRESS, true)).getCode()).isEqualTo("REVIEW_REQUIRED");
    }

    @Test
    void lockedTopicTakesPriorityOverEarlierLesson() {
        LearningGateException error = assertThrows(LearningGateException.class,
                () -> gate.authorize(List.of(), null, TopicStatus.LOCKED, false));

        assertThat(error.getCode()).isEqualTo("TOPIC_LOCKED");
        assertThat(error.getReviews()).isEmpty();
    }

    @Test
    void missingTopicIsLockedAndIncompleteEarlierLessonBlocksOpenTopic() {
        assertThat(assertThrows(LearningGateException.class,
                () -> gate.authorize(List.of(), null, null, true)).getCode()).isEqualTo("TOPIC_LOCKED");
        assertThat(assertThrows(LearningGateException.class,
                () -> gate.authorize(List.of(), null, TopicStatus.IN_PROGRESS, false)).getCode())
                .isEqualTo("LESSON_LOCKED");
    }

    @Test
    void permitsCurrentOrPassedTopicAfterEarlierLessonsAreComplete() {
        assertThatCode(() -> gate.authorize(List.of(), null, TopicStatus.IN_PROGRESS, true)).doesNotThrowAnyException();
        assertThatCode(() -> gate.authorize(List.of(), null, TopicStatus.PASSED, true)).doesNotThrowAnyException();
    }

    @Test
    void currentReviewDoesNotBlockItsOwnAccessButOtherReviewsDo() {
        assertThatCode(() -> gate.authorize(List.of(review), review.reviewId(), TopicStatus.PASSED, true))
                .doesNotThrowAnyException();
        PendingReview other = new PendingReview(new UUID(0, 4), new UUID(0, 5), new UUID(0, 6));
        LearningGateException error = assertThrows(LearningGateException.class,
                () -> gate.authorize(List.of(review, other), review.reviewId(), TopicStatus.PASSED, true));

        assertThat(error.getCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(error.getReviews()).containsExactly(other);
    }

    @Test
    void currentReviewExceptionDoesNotBypassOtherGates() {
        assertThat(assertThrows(LearningGateException.class,
                () -> gate.authorize(List.of(review), review.reviewId(), TopicStatus.LOCKED, true)).getCode())
                .isEqualTo("TOPIC_LOCKED");
        assertThat(assertThrows(LearningGateException.class,
                () -> gate.authorize(List.of(review), review.reviewId(), TopicStatus.IN_PROGRESS, false)).getCode())
                .isEqualTo("LESSON_LOCKED");
    }

    private static final UUID FIRST = new UUID(1, 1);
    private static final UUID SECOND = new UUID(1, 2);

    private LearningGateException lessonGate(List<PendingReview> pending, Set<LearningSkill> skills,
                                             Set<UUID> previousLessons, TopicStatus topic, boolean previousComplete,
                                             List<UUID> needPractice) {
        return assertThrows(LearningGateException.class, () -> gate.authorize(pending, null, skills, previousLessons,
                topic, previousComplete, needPractice));
    }

    @Test
    void anEarlierLessonWhosePracticeIsNotPassedRequiresPracticeAndNamesTheLessons() {
        var error = lessonGate(List.of(), Set.of(LearningSkill.READING), Set.of(FIRST, SECOND),
                TopicStatus.IN_PROGRESS, true, List.of(FIRST, SECOND));

        assertThat(error.getCode()).isEqualTo("PRACTICE_REQUIRED");
        assertThat(error.getLessonIds()).containsExactly(FIRST, SECOND);
        assertThat(error.getReviews()).isEmpty();
        assertThat(error.getMessage()).isEqualTo("Complete practice of the earlier lessons first.");
        assertThatThrownBy(() -> error.getLessonIds().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aPendingReviewOfAnEarlierLessonBlocksEvenWhenItsSkillIsNotTaughtHere() {
        var listeningReviewOfFirst = new PendingReview(new UUID(2, 1), FIRST, new UUID(2, 2), LearningSkill.LISTENING);

        var error = lessonGate(List.of(listeningReviewOfFirst), Set.of(LearningSkill.READING), Set.of(FIRST),
                TopicStatus.IN_PROGRESS, true, List.of(FIRST));

        assertThat(error.getCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(error.getReviews()).containsExactly(listeningReviewOfFirst);
    }

    @Test
    void aPendingReviewOfAnotherTopicAndAnotherSkillDoesNotBlock() {
        var elsewhere = new PendingReview(new UUID(2, 3), new UUID(9, 9), new UUID(2, 4), LearningSkill.LISTENING);

        assertThatCode(() -> gate.authorize(List.of(elsewhere), null, Set.of(LearningSkill.READING), Set.of(FIRST),
                TopicStatus.IN_PROGRESS, true, List.of())).doesNotThrowAnyException();
    }

    @Test
    void reviewRequiredThenTopicThenLessonLockedAllComeBeforePracticeRequired() {
        var pending = new PendingReview(new UUID(2, 5), FIRST, new UUID(2, 6), LearningSkill.READING);

        assertThat(lessonGate(List.of(pending), Set.of(LearningSkill.READING), Set.of(FIRST), TopicStatus.LOCKED,
                false, List.of(FIRST)).getCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(lessonGate(List.of(), Set.of(LearningSkill.READING), Set.of(FIRST), TopicStatus.LOCKED, true,
                List.of(FIRST)).getCode()).isEqualTo("TOPIC_LOCKED");
        assertThat(lessonGate(List.of(), Set.of(LearningSkill.READING), Set.of(FIRST), TopicStatus.IN_PROGRESS,
                false, List.of(FIRST)).getCode()).isEqualTo("LESSON_LOCKED");
    }

    @Test
    void noPracticeDebtLetsALessonThrough() {
        assertThatCode(() -> gate.authorize(List.of(), null, Set.of(LearningSkill.READING), Set.of(FIRST, SECOND),
                TopicStatus.IN_PROGRESS, true, List.of())).doesNotThrowAnyException();
        assertThatCode(() -> gate.authorize(List.of(), null, Set.of(LearningSkill.READING), Set.of(),
                TopicStatus.IN_PROGRESS, true, List.of())).doesNotThrowAnyException();
    }

    @Test
    void theCurrentReviewDoesNotBlockItsOwnLessonAccessEvenWhenItBelongsToAnEarlierLesson() {
        var current = new PendingReview(new UUID(2, 7), FIRST, new UUID(2, 8), LearningSkill.LISTENING);

        assertThatCode(() -> gate.authorize(List.of(current), current.reviewId(), Set.of(LearningSkill.READING),
                Set.of(FIRST), TopicStatus.IN_PROGRESS, true, List.of())).doesNotThrowAnyException();
    }

    @Test
    void anOptionalReviewNeverLocksALesson() {
        var optionalOfThisSkill = new PendingReview(new UUID(3, 1), FIRST, new UUID(3, 2), LearningSkill.READING, false);
        var optionalWithoutSkill = new PendingReview(new UUID(3, 3), FIRST, new UUID(3, 4), null, false);

        assertThatCode(() -> gate.authorize(List.of(optionalOfThisSkill, optionalWithoutSkill), null,
                Set.of(LearningSkill.READING), Set.of(FIRST), TopicStatus.IN_PROGRESS, true, List.of()))
                .doesNotThrowAnyException();
        assertThat(LessonAccessGate.blocking(List.of(optionalOfThisSkill, optionalWithoutSkill),
                Set.of(LearningSkill.READING))).isEmpty();
    }

    @Test
    void aBlockingReviewStillBlocksNextToAnOptionalOne() {
        var optional = new PendingReview(new UUID(3, 5), FIRST, new UUID(3, 6), LearningSkill.READING, false);
        var blocking = new PendingReview(new UUID(3, 7), FIRST, new UUID(3, 8), LearningSkill.READING, true);

        var error = lessonGate(List.of(optional, blocking), Set.of(LearningSkill.READING), Set.of(FIRST),
                TopicStatus.IN_PROGRESS, true, List.of());

        assertThat(error.getCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(error.getReviews()).containsExactly(blocking);
    }

    @Test
    void reviewsBlockOnlyTheirSkillWhileUnspecifiedReviewsBlockEverySkill() {
        var reading = new PendingReview(new UUID(0, 10), new UUID(0, 11), new UUID(0, 12),
                LearningSkill.READING);
        assertThatCode(() -> gate.authorize(List.of(reading), null, LearningSkill.LISTENING,
                TopicStatus.IN_PROGRESS, true)).doesNotThrowAnyException();
        var error = assertThrows(LearningGateException.class, () -> gate.authorize(List.of(reading), null,
                LearningSkill.READING, TopicStatus.IN_PROGRESS, true));
        assertThat(error.getReviews()).containsExactly(reading);
        assertThat(assertThrows(LearningGateException.class, () -> gate.authorize(List.of(review), null,
                LearningSkill.LISTENING, TopicStatus.IN_PROGRESS, true)).getCode())
                .isEqualTo("REVIEW_REQUIRED");
        assertThatCode(() -> gate.authorize(List.of(reading), reading.reviewId(), LearningSkill.READING,
                TopicStatus.IN_PROGRESS, true)).doesNotThrowAnyException();
    }
}
