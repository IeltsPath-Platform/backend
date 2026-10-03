package com.group01.learning.application.result;

import java.util.List;
import java.util.UUID;
import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.PracticeStatus;
import com.group01.learning.domain.vo.PracticePassReason;

public record TopicLessonsResult(UUID topicId, List<LessonSummary> lessons, String testStatus,
                                 LearningSkill skill, boolean hasTopicTest) {
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
