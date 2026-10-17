package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PendingReview;
import com.ieltspath.learning.domain.vo.TopicStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Whether a learner may open a lesson. The checks run in this order, the first failure wins: a blocking pending review
 * ({@code REVIEW_REQUIRED}), the topic ({@code TOPIC_LOCKED}), an earlier lesson that is not completed
 * ({@code LESSON_LOCKED}), then an earlier lesson whose Practice is not passed ({@code PRACTICE_REQUIRED}).
 */
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
        authorize(pendingReviews, currentReviewId, lessonSkills, Set.of(), topicStatus, previousLessonsComplete,
                List.of());
    }

    /**
     * The lesson gate. On top of the skill rule, a pending review that belongs to one of {@code previousLessonIds}
     * (the lessons before this one in the topic) blocks whatever skill it has, so the learner is led into that review.
     * {@code lessonsNeedingPractice} are the earlier lessons whose Practice is not passed; they only matter once every
     * earlier lesson is completed and no review is pending.
     */
    public void authorize(List<PendingReview> pendingReviews, UUID currentReviewId, Set<LearningSkill> lessonSkills,
                          Set<UUID> previousLessonIds, TopicStatus topicStatus, boolean previousLessonsComplete,
                          List<UUID> lessonsNeedingPractice) {
        List<PendingReview> blockingReviews = blocking(pendingReviews, lessonSkills, previousLessonIds).stream()
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
        if (!lessonsNeedingPractice.isEmpty()) {
            throw new LearningGateException("PRACTICE_REQUIRED", List.of(), lessonsNeedingPractice);
        }
    }

    /** The pending reviews that block content teaching {@code skills}. */
    public static List<PendingReview> blocking(List<PendingReview> pendingReviews, Set<LearningSkill> skills) {
        return blocking(pendingReviews, skills, Set.of());
    }

    /**
     * As above, plus every pending review that belongs to one of {@code lessonIds} whatever its skill. An optional
     * review ({@link PendingReview#blocking()} false) never blocks.
     */
    public static List<PendingReview> blocking(List<PendingReview> pendingReviews, Set<LearningSkill> skills,
                                               Set<UUID> lessonIds) {
        return pendingReviews.stream().filter(PendingReview::blocking)
                .filter(review -> review.skill() == null || skills.contains(review.skill())
                        || lessonIds.contains(review.lessonId())).toList();
    }
}
