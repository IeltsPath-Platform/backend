package com.group01.learning.domain.service;

import com.group01.learning.domain.exception.LearningGateException;
import com.group01.learning.domain.vo.PendingReview;
import com.group01.learning.domain.vo.TopicStatus;
import com.group01.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;

import java.util.List;
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
