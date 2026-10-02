package com.group01.learning.application.usecase;

import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.result.TopicLessonsResult;
import com.group01.learning.application.service.LessonAccess;
import com.group01.learning.domain.exception.LearningGateException;
import com.group01.learning.domain.repository.LessonProgressRepository;
import com.group01.learning.domain.repository.ReviewItemRepository;
import com.group01.learning.domain.vo.TopicStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A topic's lessons in order with their status, and whether its final test can be taken. */
@Service
public class GetTopicLessonsUseCase {
    private final LearningContentClient content;
    private final LessonAccess access;
    private final LessonProgressRepository lessons;
    private final ReviewItemRepository reviews;

    public GetTopicLessonsUseCase(LearningContentClient content, LessonAccess access,
                                  LessonProgressRepository lessons, ReviewItemRepository reviews) {
        this.content = content;
        this.access = access;
        this.lessons = lessons;
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public TopicLessonsResult execute(UUID userId, UUID topicId) {
        TopicStatus topicStatus = access.status(userId, topicId);
        if (topicStatus == TopicStatus.LOCKED) throw new LearningGateException("TOPIC_LOCKED", List.of());
        var ordered = LessonAccess.orderedLessons(content.getTopicLessons(topicId));
        var progress = lessons.findByTopic(userId, topicId);
        boolean previousComplete = true;
        List<TopicLessonsResult.LessonSummary> summaries = new ArrayList<>();
        for (var lesson : ordered) {
            boolean completed = LessonAccess.completed(progress.get(lesson.lessonId()));
            summaries.add(new TopicLessonsResult.LessonSummary(lesson.lessonId(), lesson.code(), lesson.title(),
                    lesson.sortOrder(), completed ? "COMPLETED" : previousComplete ? "AVAILABLE" : "LOCKED"));
            previousComplete &= completed;
        }
        boolean hasPendingReview = !reviews.findPending(userId).isEmpty();
        String testStatus = topicStatus == TopicStatus.PASSED ? "PASSED"
                : previousComplete && !hasPendingReview ? "AVAILABLE" : "LOCKED";
        return new TopicLessonsResult(topicId, List.copyOf(summaries), testStatus);
    }
}
