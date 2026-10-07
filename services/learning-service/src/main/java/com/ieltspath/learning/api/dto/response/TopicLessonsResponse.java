package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.TopicLessonsResult;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticePassReason;
import com.ieltspath.learning.domain.vo.PracticeStatus;

import java.util.List;
import java.util.UUID;

public record TopicLessonsResponse(UUID topicId, List<Lesson> lessons, String testStatus,
                                   LearningSkill skill, boolean hasTopicTest, List<LearningSkill> skills) {
    public record Lesson(UUID lessonId, String code, String title, int sortOrder, String status,
                         PracticeStatus practiceStatus, PracticePassReason practicePassReason) {}

    public static TopicLessonsResponse from(TopicLessonsResult result) {
        return new TopicLessonsResponse(result.topicId(), result.lessons().stream().map(lesson -> new Lesson(
                lesson.lessonId(), lesson.code(), lesson.title(), lesson.sortOrder(), lesson.status(),
                lesson.practiceStatus(), lesson.practicePassReason())).toList(),
                result.testStatus(), result.skill(), result.hasTopicTest(),
                result.skills().stream().sorted().toList());
    }
}
