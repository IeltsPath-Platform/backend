package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.aggregate.LessonProgress;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.vo.PracticePassReason;
import com.ieltspath.learning.domain.vo.PracticeStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * The one place a lesson becomes completed. A lesson with required Practice completes when its Practice passes; a
 * lesson without one completes when the learner confirms it. Callers hold the learner's lock.
 */
@Component
public class LessonCompletion {
    private final LearningContentClient content;
    private final LessonProgressRepository lessons;
    private final PracticeProgress practice;
    private final TopicCompletion topicCompletion;
    private final Clock clock = Clock.systemUTC();

    public LessonCompletion(LearningContentClient content, LessonProgressRepository lessons,
                            PracticeProgress practice, TopicCompletion topicCompletion) {
        this.content = content;
        this.lessons = lessons;
        this.practice = practice;
        this.topicCompletion = topicCompletion;
    }

    /** Re-derives the lesson's Practice after an attempt or review changed it, completing the lesson once it passes. */
    public void afterPracticeChange(UUID userId, UUID lessonId) {
        var clearance = practice.refreshPassForLesson(userId, lessonId);
        if (clearance.status() != PracticeStatus.PASSED || clearance.reason() == PracticePassReason.NO_PRACTICE) return;
        if (lessons.find(userId, lessonId).map(LessonProgress::isCompleted).orElse(false)) return;
        complete(userId, content.getLesson(lessonId));
    }

    /** Records the lesson as completed; idempotent. */
    public void complete(UUID userId, LearningContentClient.Lesson lesson) {
        LessonProgress progress = lessons.find(userId, lesson.lessonId()).orElseGet(() -> LessonProgress.start(userId,
                lesson.lessonId(), lesson.topicId(), lesson.sortOrder(), lesson.knowledgePointIds()));
        if (!progress.complete(clock.instant())) return;
        lessons.save(progress);
        topicCompletion.onLessonCompleted(userId, lesson.topicId());
    }
}
