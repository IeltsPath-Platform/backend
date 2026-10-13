package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient.Lesson;
import com.ieltspath.learning.application.result.LessonResult;
import com.ieltspath.learning.application.service.LessonAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Opens a lesson behind the lesson gate; its summary, slides and documents come from the resources. */
@Service
public class GetLessonUseCase {
    private final LearnerLock lock;
    private final LessonAccess access;

    public GetLessonUseCase(LearnerLock lock, LessonAccess access) {
        this.lock = lock;
        this.access = access;
    }

    @Transactional
    public LessonResult execute(UUID userId, UUID lessonId) {
        lock.lock(userId);
        LessonAccess.Context context = access.authorize(userId, lessonId);
        access.refresh(userId, context.lesson(), context.progress());
        Lesson lesson = context.lesson();
        return new LessonResult(lesson.lessonId(), lesson.topicId(), lesson.code(), lesson.title(), lesson.summary(),
                lesson.sortOrder(), LessonAccess.completed(context.progress()) ? "COMPLETED" : "AVAILABLE",
                lesson.skill(), lesson.skills());
    }
}
