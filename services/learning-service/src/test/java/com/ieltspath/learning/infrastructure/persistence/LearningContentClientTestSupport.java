package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.LearningContentClient;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

final class LearningContentClientTestSupport {
    private LearningContentClientTestSupport() {}

    static void stubLessonAccessSummaries(LearningContentClient content) {
        when(content.getLessonAccessSummary(any(UUID.class))).thenAnswer(invocation -> {
            var lesson = content.getLesson(invocation.getArgument(0));
            return new LearningContentClient.LessonSummary(lesson.lessonId(), lesson.topicId(), lesson.code(),
                    lesson.title(), lesson.summary(), lesson.sortOrder(), lesson.knowledgePointIds(), List.of(),
                    lesson.skills(), null, lesson.courseId(), lesson.skill(), lesson.skillPosition());
        });
        when(content.getLesson(any(UUID.class), anyBoolean()))
                .thenAnswer(invocation -> content.getLesson(invocation.getArgument(0)));
    }
}
