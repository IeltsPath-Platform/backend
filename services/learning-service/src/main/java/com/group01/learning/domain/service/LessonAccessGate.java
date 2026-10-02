package com.group01.learning.domain.service;

import com.group01.learning.domain.exception.LearningGateException;
import com.group01.learning.domain.vo.PendingReview;
import com.group01.learning.domain.vo.TopicStatus;

import java.util.List;
import java.util.UUID;

public final class LessonAccessGate {
    public void authorize(List<PendingReview> pendingReviews, UUID currentReviewId,
                          TopicStatus topicStatus, boolean previousLessonsComplete) {
        List<PendingReview> blockingReviews = pendingReviews.stream()
                .filter(review -> !review.reviewId().equals(currentReviewId))
                .toList();
        if (!blockingReviews.isEmpty()) {
            throw new LearningGateException("REVIEW_REQUIRED", blockingReviews);
        }
        if (topicStatus != TopicStatus.IN_PROGRESS && topicStatus != TopicStatus.PASSED) {
            throw new LearningGateException("TOPIC_LOCKED", List.of());
        }
        if (!previousLessonsComplete) {
            throw new LearningGateException("LESSON_LOCKED", List.of());
        }
    }
}
