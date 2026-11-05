package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticePassReason;
import com.ieltspath.learning.domain.vo.PracticeStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record TopicLessonsResult(UUID topicId, List<LessonSummary> lessons, String testStatus,
                                 LearningSkill skill, boolean hasTopicTest, Set<LearningSkill> skills) {
    public TopicLessonsResult {
        skills = skills == null ? Set.of() : skills;
    }
    public TopicLessonsResult(UUID topicId, List<LessonSummary> lessons, String testStatus,
                              LearningSkill skill, boolean hasTopicTest) {
        this(topicId, lessons, testStatus, skill, hasTopicTest, skill == null ? Set.of() : Set.of(skill));
    }
    public TopicLessonsResult(UUID topicId, List<LessonSummary> lessons, String testStatus) {
        this(topicId, lessons, testStatus, null, true);
    }
    /**
     * {@code estimatedMinutes} is null when Content has no duration for the lesson. {@code blockingReviewId} is the
     * pending review that keeps the lesson locked, null when no review does.
     */
    public record LessonSummary(UUID lessonId, String code, String title, int sortOrder, String status,
                                PracticeStatus practiceStatus, PracticePassReason practicePassReason,
                                Integer estimatedMinutes, UUID blockingReviewId, boolean paywall,
                                boolean freePreview) {
        public LessonSummary(UUID lessonId, String code, String title, int sortOrder, String status,
                             PracticeStatus practiceStatus, PracticePassReason practicePassReason,
                             Integer estimatedMinutes, UUID blockingReviewId) {
            this(lessonId, code, title, sortOrder, status, practiceStatus, practicePassReason, estimatedMinutes,
                    blockingReviewId, false, false);
        }
        public LessonSummary(UUID lessonId, String code, String title, int sortOrder, String status,
                             PracticeStatus practiceStatus, PracticePassReason practicePassReason,
                             Integer estimatedMinutes) {
            this(lessonId, code, title, sortOrder, status, practiceStatus, practicePassReason, estimatedMinutes, null,
                    false, false);
        }
        public LessonSummary(UUID lessonId, String code, String title, int sortOrder, String status,
                             PracticeStatus practiceStatus, PracticePassReason practicePassReason) {
            this(lessonId, code, title, sortOrder, status, practiceStatus, practicePassReason, null, null, false, false);
        }
        public LessonSummary(UUID lessonId, String code, String title, int sortOrder, String status) {
            this(lessonId, code, title, sortOrder, status, null, null, null, null, false, false);
        }
    }
}
