package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient.Block;
import com.ieltspath.learning.application.port.LearningContentClient.Lesson;
import com.ieltspath.learning.application.service.LessonAccess;
import com.ieltspath.learning.application.service.PracticeProgress;
import com.ieltspath.learning.application.service.TopicCompletion;
import com.ieltspath.learning.domain.aggregate.LessonProgress;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Completes a lesson that has no exercise block; idempotent. */
@Service
public class CompleteLessonUseCase {
    private final LearnerLock lock;
    private final LessonAccess access;
    private final LessonProgressRepository lessons;
    private final PracticeProgress practice;
    private final TopicCompletion topicCompletion;
    private final Clock clock = Clock.systemUTC();

    public CompleteLessonUseCase(LearnerLock lock, LessonAccess access, LessonProgressRepository lessons,
                                 PracticeProgress practice, TopicCompletion topicCompletion) {
        this.lock = lock;
        this.access = access;
        this.lessons = lessons;
        this.practice = practice;
        this.topicCompletion = topicCompletion;
    }

    @Transactional
    public UUID execute(UUID userId, UUID lessonId) {
        lock.lock(userId);
        LessonAccess.Context context = access.authorize(userId, lessonId);
        Lesson lesson = context.lesson();
        if (lesson.blocks().stream().anyMatch(Block::isExercise)) {
            throw new LearningRequestException(409, "LESSON_HAS_EXERCISES", "Lesson contains exercise blocks");
        }
        LessonProgress progress = access.refresh(userId, lesson, context.progress());
        if (progress.complete(clock.instant())) {
            lessons.save(progress);
            practice.refreshPassForLesson(userId, lesson.lessonId());
            topicCompletion.onLessonCompleted(userId, lesson.topicId());
        }
        return lessonId;
    }
}
