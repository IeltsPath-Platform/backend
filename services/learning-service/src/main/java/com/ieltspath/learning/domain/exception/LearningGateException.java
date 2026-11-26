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
            case "REVIEW_REQUIRED" -> "Vui lòng hoàn thành các bài ôn tập đang chờ trước khi tiếp tục.";
            case "TOPIC_LOCKED" -> "Chủ đề này hiện đang bị khóa. Vui lòng hoàn thành chủ đề trước đó.";
            case "LESSON_LOCKED" -> "Vui lòng hoàn thành các bài học trước đó để mở khóa bài này.";
            case "PRACTICE_REQUIRED" -> "Vui lòng hoàn thành phần luyện tập của các bài học trước.";
            case "PAYWALL" -> "Vui lòng mở khóa kỹ năng này để truy cập bài học.";
            default -> throw new IllegalArgumentException("Mã điều kiện mở bài học không hợp lệ.");
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
