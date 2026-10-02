package com.group01.learning.application.result;

import java.util.List;
import java.util.UUID;

/** {@code set} is null once the review is no longer pending. */
public record ReviewResult(UUID reviewId, String reviewStatus, UUID lessonId, List<String> theory, ReviewSet set) {
    public record ReviewSet(UUID reviewSetId, UUID packageId, UUID packageVersionId, String passage, Audio audio,
                            List<LessonResult.Question> questions) {}

    /** Review audio never carries the transcript before the set is passed. */
    public record Audio(String mediaUrl, Integer durationSeconds) {}
}
