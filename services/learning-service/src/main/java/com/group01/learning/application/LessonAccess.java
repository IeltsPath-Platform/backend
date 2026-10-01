package com.group01.learning.application;

import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.Lesson;
import com.group01.learning.application.port.LearningContentClient.LessonSummary;
import com.group01.learning.application.port.LearningProgressStore;
import com.group01.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.group01.learning.domain.service.LessonAccessGate;
import com.group01.learning.domain.service.TopicStatusDeriver;
import com.group01.learning.domain.vo.LessonProgress;
import com.group01.learning.domain.vo.TopicStatus;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The one lesson gate shared by reading a lesson, exercise and essay submissions, and completion: pending review,
 * then topic, then earlier lessons. Callers hold the learner's advisory lock.
 */
@Component
public class LessonAccess {
    private final LearningContentClient content;
    private final LearningProgressStore store;
    private final RefreshLearningTopicsUseCase refreshTopics;
    private final LessonAccessGate gate = new LessonAccessGate();
    private final TopicStatusDeriver topicStatuses = new TopicStatusDeriver();

    public LessonAccess(LearningContentClient content, LearningProgressStore store,
                        RefreshLearningTopicsUseCase refreshTopics) {
        this.content = content;
        this.store = store;
        this.refreshTopics = refreshTopics;
    }

    public Context authorize(UUID userId, UUID lessonId) {
        return authorize(userId, content.getLesson(lessonId));
    }

    public Context authorize(UUID userId, Lesson lesson) {
        var summaries = orderedLessons(content.getTopicLessons(lesson.topicId()));
        var progress = store.findLessons(userId, lesson.topicId());
        boolean previousComplete = true;
        boolean found = false;
        for (var summary : summaries) {
            if (summary.lessonId().equals(lesson.lessonId())) { found = true; break; }
            previousComplete &= completed(progress.get(summary.lessonId()));
        }
        gate.authorize(store.findPendingReviews(userId), null, status(userId, lesson.topicId()), previousComplete);
        if (!found) throw new LearningRequestException(404, "NOT_FOUND", "Lesson was not found");
        return new Context(lesson, progress.get(lesson.lessonId()), previousComplete);
    }

    /** Checks the gate again after a curriculum refresh may have changed the learner's current topic. */
    public void reauthorize(UUID userId, Context context) {
        gate.authorize(store.findPendingReviews(userId), null, status(userId, context.lesson().topicId()),
                context.previousLessonsComplete());
    }

    public TopicStatus status(UUID userId, UUID topicId) {
        return topicStatuses.derive(store.findTopics(userId)).getOrDefault(topicId, TopicStatus.LOCKED);
    }

    /** Stores the lesson's topic, order and knowledge points as Content currently has them. */
    public void refresh(UUID userId, Lesson lesson) {
        store.refreshLesson(userId, lesson.lessonId(), lesson.topicId(), lesson.sortOrder(), lesson.knowledgePointIds());
    }

    /** Knowledge points in the learner's catalog, refreshing the curriculum once when {@code needed} are missing. */
    public Set<UUID> knownKnowledgePoints(UUID userId, Collection<UUID> needed) {
        Set<UUID> known = catalog(userId);
        if (!known.containsAll(needed)) {
            refreshTopics.execute(userId);
            known = catalog(userId);
        }
        return known;
    }

    private Set<UUID> catalog(UUID userId) {
        return store.findMastery(userId).stream().map(LearningProgressStore.MasteryHistory::knowledgePointId)
                .collect(Collectors.toSet());
    }

    public static boolean completed(LessonProgress progress) {
        return progress != null && progress.completedAt() != null;
    }

    public static List<LessonSummary> orderedLessons(List<LessonSummary> lessons) {
        return lessons.stream().sorted(Comparator.comparingInt(LessonSummary::sortOrder)
                .thenComparing(lesson -> lesson.lessonId().toString())).toList();
    }

    public record Context(Lesson lesson, LessonProgress progress, boolean previousLessonsComplete) {}
}
