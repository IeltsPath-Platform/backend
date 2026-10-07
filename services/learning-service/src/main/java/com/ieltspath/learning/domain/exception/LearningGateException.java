package com.ieltspath.learning.domain.exception;

import com.ieltspath.learning.domain.vo.PendingReview;

import java.util.List;

public final class LearningGateException extends RuntimeException {
    private final String code;
    private final List<PendingReview> reviews;

    public LearningGateException(String code, List<PendingReview> reviews) {
        super(switch (code) {
            case "REVIEW_REQUIRED" -> "Complete the pending reviews first.";
            case "TOPIC_LOCKED" -> "This topic is locked.";
            case "LESSON_LOCKED" -> "Complete the earlier lessons first.";
            default -> throw new IllegalArgumentException("Unknown learning gate code.");
        });
        this.code = code;
        this.reviews = List.copyOf(reviews);
    }

    public String getCode() {
        return code;
    }

    public List<PendingReview> getReviews() {
        return reviews;
    }
}
