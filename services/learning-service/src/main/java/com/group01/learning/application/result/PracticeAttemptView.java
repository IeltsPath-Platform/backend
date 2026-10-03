package com.group01.learning.application.result;

import java.util.List;
import java.util.UUID;

public record PracticeAttemptView(UUID attemptId, UUID packageId, UUID packageVersionId, String passage,
                                  Audio audio, List<LessonResult.Question> questions) {
    public record Audio(String mediaUrl, Integer durationSeconds) {}
}
