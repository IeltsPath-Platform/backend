package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.LessonResources;
import com.ieltspath.learning.application.service.LessonAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * A lesson's summary, slides and documents behind the same gate as reading the lesson. Opening the resources does
 * not start or change the learner's lesson progress.
 */
@Service
public class GetLessonResourcesUseCase {
    private final LearnerLock lock;
    private final LessonAccess access;
    private final LearningContentClient content;

    public GetLessonResourcesUseCase(LearnerLock lock, LessonAccess access, LearningContentClient content) {
        this.lock = lock;
        this.access = access;
        this.content = content;
    }

    @Transactional
    public LessonResources execute(UUID userId, UUID lessonId) {
        lock.lock(userId);
        access.authorizeToRead(userId, lessonId);
        return content.getLessonResources(lessonId);
    }
}
