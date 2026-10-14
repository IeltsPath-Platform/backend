package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Practice uses the lesson's gate and opens with the lesson. Unlike reading the lesson, it waits for every pending
 * review of the lesson's skills, including the ones the lesson's own Practice created. Callers hold the learner's lock.
 */
@Component
public class PracticeAccess {
    private final LessonAccess access;

    public PracticeAccess(LessonAccess access) {
        this.access = access;
    }

    public LearningContentClient.Lesson require(UUID userId, UUID lessonId) {
        return access.authorizePractice(userId, lessonId).lesson();
    }
}
