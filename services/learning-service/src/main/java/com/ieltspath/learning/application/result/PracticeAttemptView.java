package com.ieltspath.learning.application.result;

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

    /** {@code recorded} are the items answered by a recording; empty for a set without any. */
    public record Section(UUID sectionId, String title, String skill, String passage, Audio audio,
                          List<LessonResult.Question> questions, List<Essay> essays, List<Recorded> recorded) {
        public Section(UUID sectionId, String title, String skill, String passage, Audio audio,
                       List<LessonResult.Question> questions, List<Essay> essays) {
            this(sectionId, title, skill, passage, audio, questions, essays, List.of());
        }
    }

    /**
     * An item answered by speaking. {@code latestSubmission} is the newest recorded answer of this attempt; its band
     * appears only once GRADED. The answer spec is never part of the view.
     */
    public record Recorded(UUID questionVersionId, int sortOrder, String stem, Integer durationSeconds,
                           BigDecimal passBand, LessonResult.LatestSubmission latestSubmission) {}

    /**
     * {@code sampleAnswer} is the model answer the learner may open while writing; null when Content has none.
     * {@code answerSpec} and {@code chartFacts} are never part of the view.
     */
    public record Essay(UUID questionVersionId, int sortOrder, String stem, String task, Integer minWords,
                        BigDecimal passBand, LessonResult.LatestSubmission latestSubmission,
                        String guide, List<LessonResult.Image> images, String sampleAnswer) {
        public Essay(UUID questionVersionId, int sortOrder, String stem, String task, Integer minWords,
                     BigDecimal passBand, LessonResult.LatestSubmission latestSubmission) {
            this(questionVersionId, sortOrder, stem, task, minWords, passBand, latestSubmission, null, List.of(), null);
        }
    }
}
