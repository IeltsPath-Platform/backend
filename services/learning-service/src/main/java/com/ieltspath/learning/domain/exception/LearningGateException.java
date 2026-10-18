package com.ieltspath.learning.domain.exception;

import com.ieltspath.learning.domain.vo.PendingReview;

import java.util.List;
import java.util.UUID;

public final class LearningGateException extends RuntimeException {
    private final String code;
    private final List<PendingReview> reviews;
    private final List<UUID> lessonIds;

    public LearningGateException(String code, List<PendingReview> reviews) {
        this(code, reviews, List.of());
    }

    /** {@code lessonIds} names the earlier lessons whose Practice is still open ({@code PRACTICE_REQUIRED}). */
    public LearningGateException(String code, List<PendingReview> reviews, List<UUID> lessonIds) {
        super(switch (code) {
            case "REVIEW_REQUIRED" -> "Complete the pending reviews first.";
            case "TOPIC_LOCKED" -> "This topic is locked.";
            case "LESSON_LOCKED" -> "Complete the earlier lessons first.";
            case "PRACTICE_REQUIRED" -> "Complete practice of the earlier lessons first.";
            default -> throw new IllegalArgumentException("Unknown learning gate code.");
        });
        this.code = code;
        this.reviews = List.copyOf(reviews);
        this.lessonIds = List.copyOf(lessonIds);
    }

    public String getCode() {
        return code;
    }

    public List<PendingReview> getReviews() {
        return reviews;
    }

    public List<UUID> getLessonIds() {
        return lessonIds;
    }
}
