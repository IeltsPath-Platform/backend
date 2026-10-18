package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.LessonAccessGate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PracticeAccess {
    private final LearningContentClient content;
    private final LessonProgressRepository lessons;
    private final ReviewItemRepository reviews;

    public PracticeAccess(LearningContentClient content, LessonProgressRepository lessons, ReviewItemRepository reviews) {
        this.content = content;
        this.lessons = lessons;
        this.reviews = reviews;
    }

    public LearningContentClient.Lesson require(UUID userId, UUID lessonId) {
        var lesson = content.getLesson(lessonId);
        if (!completed(userId, lessonId)) {
            throw new LearningRequestException(409, "PRACTICE_LOCKED", "Complete the lesson before practice");
        }
        var pending = LessonAccessGate.blocking(reviews.findPending(userId), lesson.skills());
        if (!pending.isEmpty()) throw new LearningGateException("REVIEW_REQUIRED", pending);
        return lesson;
    }

    public boolean completed(UUID userId, UUID lessonId) {
        return lessons.find(userId, lessonId).map(LessonAccess::completed).orElse(false);
    }
}
