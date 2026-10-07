package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PendingReview;
import com.ieltspath.learning.domain.vo.TopicStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class LessonAccessGate {
    public void authorize(List<PendingReview> pendingReviews, UUID currentReviewId,
                          TopicStatus topicStatus, boolean previousLessonsComplete) {
        authorize(pendingReviews, currentReviewId, (LearningSkill) null, topicStatus, previousLessonsComplete);
    }

    public void authorize(List<PendingReview> pendingReviews, UUID currentReviewId, LearningSkill lessonSkill,
                          TopicStatus topicStatus, boolean previousLessonsComplete) {
        authorize(pendingReviews, currentReviewId, lessonSkill == null ? Set.of() : Set.of(lessonSkill), topicStatus,
                previousLessonsComplete);
    }

    /**
     * A pending review blocks a lesson that teaches the review's skill; a review without a skill blocks every lesson.
     */
    public void authorize(List<PendingReview> pendingReviews, UUID currentReviewId, Set<LearningSkill> lessonSkills,
                          TopicStatus topicStatus, boolean previousLessonsComplete) {
        List<PendingReview> blockingReviews = blocking(pendingReviews, lessonSkills).stream()
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

    /** The pending reviews that block content teaching {@code skills}. */
    public static List<PendingReview> blocking(List<PendingReview> pendingReviews, Set<LearningSkill> skills) {
        return pendingReviews.stream().filter(review -> review.skill() == null || skills.contains(review.skill()))
                .toList();
    }
}
