package com.group01.learning.application.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * An open practice attempt. {@code sections} lists every section with its passage or audio, its auto-graded
 * questions and its essays (each with the learner's newest submission). {@code passage}, {@code audio} and
 * {@code questions} repeat the first section's material and every auto-graded question, as before mixed sets.
 */
public record PracticeAttemptView(UUID attemptId, UUID packageId, UUID packageVersionId, String passage,
                                  Audio audio, List<LessonResult.Question> questions, List<Section> sections) {
    public PracticeAttemptView(UUID attemptId, UUID packageId, UUID packageVersionId, String passage, Audio audio,
                               List<LessonResult.Question> questions) {
        this(attemptId, packageId, packageVersionId, passage, audio, questions, List.of());
    }

    public record Audio(String mediaUrl, Integer durationSeconds) {}

    public record Section(UUID sectionId, String title, String skill, String passage, Audio audio,
                          List<LessonResult.Question> questions, List<Essay> essays) {}

    public record Essay(UUID questionVersionId, int sortOrder, String stem, String task, Integer minWords,
                        BigDecimal passBand, LessonResult.LatestSubmission latestSubmission) {}
}
