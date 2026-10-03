package com.group01.learning.application.service;

import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.domain.repository.LearnerCurriculumRepository;
import com.group01.learning.domain.repository.LessonProgressRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Component
public class TopicCompletion {
    private final LearningContentClient content;
    private final LearnerCurriculumRepository curricula;
    private final LessonProgressRepository lessons;
    private final Clock clock = Clock.systemUTC();

    public TopicCompletion(LearningContentClient content, LearnerCurriculumRepository curricula,
                           LessonProgressRepository lessons) {
        this.content = content;
        this.curricula = curricula;
        this.lessons = lessons;
    }

    public void onLessonCompleted(UUID userId, UUID topicId) {
        var curriculum = curricula.find(userId);
        var topic = curriculum.topic(topicId);
        if (topic.isEmpty() || topic.get().hasTopicTest() || topic.get().passedAt() != null) return;
        var published = content.getTopicLessons(topicId);
        if (published.isEmpty()) return;
        var progress = lessons.findByTopic(userId, topicId);
        if (published.stream().allMatch(lesson -> LessonAccess.completed(progress.get(lesson.lessonId())))) {
            curriculum.pass(topicId, clock.instant());
            curricula.save(curriculum);
        }
    }
}
