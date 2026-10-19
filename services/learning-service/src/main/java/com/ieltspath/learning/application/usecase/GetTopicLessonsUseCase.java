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
import java.util.Set;
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
        Map<UUID, Boolean> completedByLesson = new HashMap<>();
        for (var lesson : ordered) completedByLesson.put(lesson.lessonId(),
                LessonAccess.completed(progress.get(lesson.lessonId())));
        var topicPractice = practice.forTopic(userId, topicId, completedByLesson);
        var practiceStates = topicPractice.clearances();
        // A lesson opens once every earlier lesson is completed and its Practice passed. The topic test only needs
        // the lessons completed here; its own Practice and review checks follow below.
        boolean previousComplete = true;
        boolean previousOpen = true;
        List<UUID> previousIds = new ArrayList<>();
        List<TopicLessonsResult.LessonSummary> summaries = new ArrayList<>();
        for (var lesson : ordered) {
            boolean completed = LessonAccess.completed(progress.get(lesson.lessonId()));
            var clearance = practiceStates.get(lesson.lessonId());
            // A pending review that blocks this lesson keeps it locked in the list as well, so the list never offers
            // a lesson that opening would refuse. A completed lesson stays open for reading.
            var blockedBy = completed ? List.<PendingReview>of()
                    : LessonAccessGate.blocking(topicPractice.pendingReviews(), topic.skills(), Set.copyOf(previousIds));
            UUID blockingReviewId = blockedBy.isEmpty() ? null : blockedBy.get(0).reviewId();
            String status = completed ? "COMPLETED" : previousOpen && blockedBy.isEmpty() ? "AVAILABLE" : "LOCKED";
            summaries.add(new TopicLessonsResult.LessonSummary(lesson.lessonId(), lesson.code(), lesson.title(),
                    lesson.sortOrder(), status, clearance.status(), clearance.reason(), lesson.estimatedMinutes(),
                    blockingReviewId));
            previousIds.add(lesson.lessonId());
            previousComplete &= completed;
            previousOpen &= completed && clearance.status() == PracticeStatus.PASSED;
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
