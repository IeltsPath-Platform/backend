package com.group01.learning.application.usecase;

import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.port.LearningContentClient.Block;
import com.group01.learning.application.port.LearningContentClient.Lesson;
import com.group01.learning.application.service.LessonAccess;
import com.group01.learning.application.service.PracticeProgress;
import com.group01.learning.application.service.TopicCompletion;
import com.group01.learning.domain.aggregate.LessonProgress;
import com.group01.learning.domain.repository.LessonProgressRepository;
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
