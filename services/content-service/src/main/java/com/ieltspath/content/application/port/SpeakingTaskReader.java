package com.ieltspath.content.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Reads the questions of one Speaking part of a published practice test, in the order the examiner asks them. */
public interface SpeakingTaskReader {
    Optional<SpeakingTask> find(UUID packageVersionId, UUID sectionId);

    record SpeakingTask(UUID packageId, UUID packageVersionId, UUID sectionId, String packageTitle, String title,
                        Integer partNo, Integer timeLimitSeconds, String instructions, List<Question> questions) {}

    /** {@code durationSeconds} is how long the learner may speak on the question; null when the question has none. */
    record Question(UUID questionVersionId, int sortOrder, String stem, Integer durationSeconds) {}
}
