package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Lesson;
import com.ieltspath.learning.application.service.LessonAccess;
import com.ieltspath.learning.application.service.LessonCompletion;
import com.ieltspath.learning.application.service.PracticeProgress;
import com.ieltspath.learning.domain.vo.PracticeStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Completes a lesson the learner confirms; idempotent. A lesson with required Practice completes only through its
 * Practice, so confirming it before the Practice passes is refused.
 */
@Service
public class CompleteLessonUseCase {
    private final LearnerLock lock;
    private final LessonAccess access;
    private final LearningContentClient content;
    private final PracticeProgress practice;
    private final LessonCompletion completion;

    public CompleteLessonUseCase(LearnerLock lock, LessonAccess access, LearningContentClient content,
                                 PracticeProgress practice, LessonCompletion completion) {
        this.lock = lock;
        this.access = access;
        this.content = content;
        this.practice = practice;
        this.completion = completion;
    }

    @Transactional
    public UUID execute(UUID userId, UUID lessonId) {
        lock.lock(userId);
        LessonAccess.Context context = access.authorize(userId, lessonId);
        Lesson lesson = context.lesson();
        var sets = content.lessonPracticeSets(lessonId);
        var clearance = practice.forLesson(userId, lessonId, true, sets == null ? List.of() : sets);
        if (clearance.status() != PracticeStatus.PASSED) {
            throw new LearningRequestException(409, "PRACTICE_REQUIRED", "Pass this lesson's practice to complete it",
                    null, List.of(lessonId));
        }
        practice.persistPassed(userId, Map.of(lessonId, clearance));
        access.refresh(userId, lesson, context.progress());
        completion.complete(userId, lesson);
        return lessonId;
    }
}
