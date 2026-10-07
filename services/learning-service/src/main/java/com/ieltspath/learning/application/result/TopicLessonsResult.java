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
    public record LessonSummary(UUID lessonId, String code, String title, int sortOrder, String status,
                                PracticeStatus practiceStatus, PracticePassReason practicePassReason) {
        public LessonSummary(UUID lessonId, String code, String title, int sortOrder, String status) {
            this(lessonId, code, title, sortOrder, status, null, null);
        }
    }
}
