package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.aggregate.LessonProgress;
import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Finds the lesson a review of a knowledge point points at: one the learner has completed and that teaches it. */
@Component
public class CompletedLessonLocator {
    private final LessonProgressRepository lessons;
    private final LearnerCurriculumRepository curricula;

    public CompletedLessonLocator(LessonProgressRepository lessons, LearnerCurriculumRepository curricula) {
        this.lessons = lessons;
        this.curricula = curricula;
    }

    /** The first completed lesson, in topic then lesson order, that teaches the point. */
    public Optional<UUID> teaching(UUID userId, UUID knowledgePointId) {
        Map<UUID, Integer> topicOrder = new HashMap<>();
        for (TopicProgress topic : curricula.find(userId).topics()) {
            if (topic.sequenceOrder() != null) topicOrder.put(topic.topicId(), topic.sequenceOrder());
        }
        return lessons.findCompleted(userId).stream()
                .filter(lesson -> lesson.knowledgePointIds().contains(knowledgePointId))
                .min(Comparator.<LessonProgress, Integer>comparing(lesson ->
                                topicOrder.getOrDefault(lesson.topicId(), Integer.MAX_VALUE))
                        .thenComparingInt(LessonProgress::sortOrder)
                        .thenComparing(lesson -> lesson.lessonId().toString()))
                .map(LessonProgress::lessonId);
    }
}
