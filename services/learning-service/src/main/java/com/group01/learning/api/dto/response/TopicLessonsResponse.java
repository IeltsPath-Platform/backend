package com.group01.learning.api.dto.response;

import com.group01.learning.application.result.TopicLessonsResult;
import java.util.List;
import java.util.UUID;
import com.group01.learning.domain.vo.LearningSkill;

public record TopicLessonsResponse(UUID topicId, List<Lesson> lessons, String testStatus,
                                   LearningSkill skill, boolean hasTopicTest) {
    public record Lesson(UUID lessonId, String code, String title, int sortOrder, String status) {}

    public static TopicLessonsResponse from(TopicLessonsResult result) {
        return new TopicLessonsResponse(result.topicId(), result.lessons().stream().map(lesson -> new Lesson(
                lesson.lessonId(), lesson.code(), lesson.title(), lesson.sortOrder(), lesson.status())).toList(),
                result.testStatus(), result.skill(), result.hasTopicTest());
    }
}
