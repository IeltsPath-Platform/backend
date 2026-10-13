package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.LearningSkill;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** What a learner sees of a lesson itself: where it sits and its status. Its content is read from the resources. */
public record LessonResult(UUID lessonId, UUID topicId, String code, String title, String summary,
                           int sortOrder, String status, LearningSkill skill, Set<LearningSkill> skills) {
    public LessonResult {
        skills = skills == null ? (skill == null ? Set.of() : Set.of(skill)) : skills;
    }
    public LessonResult(UUID lessonId, UUID topicId, String code, String title, String summary,
                        int sortOrder, String status, LearningSkill skill) {
        this(lessonId, topicId, code, title, summary, sortOrder, status, skill, null);
    }
    public LessonResult(UUID lessonId, UUID topicId, String code, String title, String summary,
                        int sortOrder, String status) {
        this(lessonId, topicId, code, title, summary, sortOrder, status, null);
    }

    public record Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options, String hint) {
        public Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options) {
            this(questionVersionId, sortOrder, stem, options, null);
        }
    }
    public record Option(String optionKey, String content, int sortOrder) {}

    /** {@code overallBand} and {@code passed} are null unless the submission is GRADED (points debited). */
    public record LatestSubmission(UUID id, String status, BigDecimal overallBand, Boolean passed) {}
}
