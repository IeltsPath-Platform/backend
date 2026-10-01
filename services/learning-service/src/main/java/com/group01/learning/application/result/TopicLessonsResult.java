package com.group01.learning.application.result;

import java.util.List;
import java.util.UUID;

public record TopicLessonsResult(UUID topicId, List<LessonSummary> lessons, String testStatus) {
    public record LessonSummary(UUID lessonId, String code, String title, int sortOrder, String status) {}
}
