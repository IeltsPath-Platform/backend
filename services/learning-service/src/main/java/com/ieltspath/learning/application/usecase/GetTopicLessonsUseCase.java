package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.TopicLessonsResult;
import com.ieltspath.learning.application.service.LessonAccess;
import com.ieltspath.learning.application.service.PracticeProgress;
import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.service.LessonAccessGate;
import com.ieltspath.learning.domain.vo.PendingReview;
import com.ieltspath.learning.domain.vo.PracticeStatus;
import com.ieltspath.learning.domain.vo.TopicStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A topic's lessons in order with their status, and whether its final test can be taken. */
@Service
public class GetTopicLessonsUseCase {
    private final LearningContentClient content;
    private final LessonAccess access;
    private final LessonProgressRepository lessons;
    private final PracticeProgress practice;

    public GetTopicLessonsUseCase(LearningContentClient content, LessonAccess access,
                                  LessonProgressRepository lessons,
                                  PracticeProgress practice) {
        this.content = content;
        this.access = access;
        this.lessons = lessons;
        this.practice = practice;
    }

    @Transactional
    public TopicLessonsResult execute(UUID userId, UUID topicId) {
        var topic = access.topic(userId, topicId);
        TopicStatus topicStatus = access.status(userId, topicId);
        if (topicStatus == TopicStatus.LOCKED) throw new LearningGateException("TOPIC_LOCKED", List.of());
        var ordered = LessonAccess.orderedLessons(content.getTopicLessons(topicId));
        var progress = lessons.findByTopic(userId, topicId);
        Map<UUID, Boolean> openByLesson = new HashMap<>();
        boolean earlierComplete = true;
        for (var lesson : ordered) {
            openByLesson.put(lesson.lessonId(), earlierComplete);
            earlierComplete &= LessonAccess.completed(progress.get(lesson.lessonId()));
        }
        var topicPractice = practice.forTopic(userId, topicId, openByLesson);
        var practiceStates = topicPractice.clearances();
        boolean previousComplete = true;
        List<TopicLessonsResult.LessonSummary> summaries = new ArrayList<>();
        for (var lesson : ordered) {
            boolean completed = LessonAccess.completed(progress.get(lesson.lessonId()));
            // The same rule as the lesson gate: a pending review blocks the other lessons teaching its skill, not
            // the lesson that created it, which stays open to read.
            var blocking = completed ? List.<PendingReview>of() : LessonAccessGate.blocking(
                    topicPractice.pendingReviews().stream()
                            .filter(review -> !review.lessonId().equals(lesson.lessonId())).toList(),
                    lesson.skills().isEmpty() ? topic.skills() : lesson.skills());
            UUID blockingReviewId = blocking.isEmpty() ? null : blocking.getFirst().reviewId();
            boolean open = previousComplete && blockingReviewId == null;
            summaries.add(new TopicLessonsResult.LessonSummary(lesson.lessonId(), lesson.code(), lesson.title(),
                    lesson.sortOrder(), completed ? "COMPLETED" : open ? "AVAILABLE" : "LOCKED",
                    practiceStates.get(lesson.lessonId()).status(),
                    practiceStates.get(lesson.lessonId()).reason(), lesson.estimatedMinutes(), blockingReviewId));
            previousComplete &= completed;
        }
        boolean hasPendingReview = !LessonAccessGate.blocking(topicPractice.pendingReviews(), topic.skills()).isEmpty();
        boolean practicePassed = practiceStates.values().stream()
                .allMatch(state -> state.status() == PracticeStatus.PASSED);
        String testStatus = !topic.hasTopicTest() ? "NONE" : topicStatus == TopicStatus.PASSED ? "PASSED"
                : previousComplete && practicePassed && !hasPendingReview ? "AVAILABLE" : "LOCKED";
        return new TopicLessonsResult(topicId, List.copyOf(summaries), testStatus,
                topic.skill(), topic.hasTopicTest(), topic.skills());
    }
}
