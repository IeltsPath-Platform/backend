package com.group01.learning.application.service;

import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.domain.exception.LearningGateException;
import com.group01.learning.domain.repository.LessonProgressRepository;
import com.group01.learning.domain.repository.ReviewItemRepository;
import com.group01.learning.domain.vo.LearningSkill;
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
        LearningSkill skill = lesson.skill();
        var pending = reviews.findPending(userId).stream()
                .filter(review -> review.skill() == null || review.skill() == skill).toList();
        if (!pending.isEmpty()) throw new LearningGateException("REVIEW_REQUIRED", pending);
        return lesson;
    }

    public boolean completed(UUID userId, UUID lessonId) {
        return lessons.find(userId, lessonId).map(LessonAccess::completed).orElse(false);
    }
}
