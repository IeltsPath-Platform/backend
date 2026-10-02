package com.group01.learning.application.service;

import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.Lesson;
import com.group01.learning.application.port.LearningContentClient.LessonSummary;
import com.group01.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.group01.learning.domain.aggregate.LessonProgress;
import com.group01.learning.domain.repository.KnowledgeEvidenceRepository;
import com.group01.learning.domain.repository.LearnerCurriculumRepository;
import com.group01.learning.domain.repository.LessonProgressRepository;
import com.group01.learning.domain.repository.ReviewItemRepository;
import com.group01.learning.domain.service.LessonAccessGate;
import com.group01.learning.domain.vo.MasteryHistory;
import com.group01.learning.domain.vo.TopicStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The one lesson gate shared by reading a lesson, exercise and essay submissions, and completion: pending review,
 * then topic, then earlier lessons. Callers hold the learner's lock.
 */
@Component
public class LessonAccess {
    private final LearningContentClient content;
    private final LessonProgressRepository lessons;
    private final LearnerCurriculumRepository curricula;
    private final ReviewItemRepository reviews;
    private final KnowledgeEvidenceRepository evidence;
    private final RefreshLearningTopicsUseCase refreshTopics;
    private final LessonAccessGate gate = new LessonAccessGate();

    public LessonAccess(LearningContentClient content, LessonProgressRepository lessons,
                        LearnerCurriculumRepository curricula, ReviewItemRepository reviews,
                        KnowledgeEvidenceRepository evidence, RefreshLearningTopicsUseCase refreshTopics) {
        this.content = content;
        this.lessons = lessons;
        this.curricula = curricula;
        this.reviews = reviews;
        this.evidence = evidence;
        this.refreshTopics = refreshTopics;
    }

    public Context authorize(UUID userId, UUID lessonId) {
        return authorize(userId, content.getLesson(lessonId));
    }

    public Context authorize(UUID userId, Lesson lesson) {
        var summaries = orderedLessons(content.getTopicLessons(lesson.topicId()));
        var progress = lessons.findByTopic(userId, lesson.topicId());
        boolean previousComplete = true;
        boolean found = false;
        for (var summary : summaries) {
            if (summary.lessonId().equals(lesson.lessonId())) { found = true; break; }
            previousComplete &= completed(progress.get(summary.lessonId()));
        }
        gate.authorize(reviews.findPending(userId), null, status(userId, lesson.topicId()), previousComplete);
        if (!found) throw new LearningRequestException(404, "NOT_FOUND", "Lesson was not found");
        return new Context(lesson, progress.get(lesson.lessonId()), previousComplete);
    }

    /** Checks the gate again after a curriculum refresh may have changed the learner's current topic. */
    public void reauthorize(UUID userId, Context context) {
        gate.authorize(reviews.findPending(userId), null, status(userId, context.lesson().topicId()),
                context.previousLessonsComplete());
    }

    public TopicStatus status(UUID userId, UUID topicId) {
        return curricula.find(userId).status(topicId);
    }

    /**
     * The learner's progress in the lesson, placed where Content currently has it and saved; created on first use.
     */
    public LessonProgress refresh(UUID userId, Lesson lesson, LessonProgress current) {
        LessonProgress progress = current != null ? current
                : lessons.find(userId, lesson.lessonId()).orElseGet(() -> LessonProgress.start(userId,
                lesson.lessonId(), lesson.topicId(), lesson.sortOrder(), lesson.knowledgePointIds()));
        progress.place(lesson.topicId(), lesson.sortOrder(), lesson.knowledgePointIds());
        lessons.save(progress);
        return progress;
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
        return evidence.findMasteryHistories(userId).stream().map(MasteryHistory::knowledgePointId)
                .collect(Collectors.toSet());
    }

    public static boolean completed(LessonProgress progress) {
        return progress != null && progress.isCompleted();
    }

    public static List<LessonSummary> orderedLessons(List<LessonSummary> lessons) {
        return lessons.stream().sorted(Comparator.comparingInt(LessonSummary::sortOrder)
                .thenComparing(lesson -> lesson.lessonId().toString())).toList();
    }

    public record Context(Lesson lesson, LessonProgress progress, boolean previousLessonsComplete) {}
}
