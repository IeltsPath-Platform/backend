package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient.Lesson;
import com.ieltspath.learning.application.port.LearningContentClient.LessonSummary;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.ieltspath.learning.domain.aggregate.LessonProgress;
import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.LessonAccessGate;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.MasteryHistory;
import com.ieltspath.learning.domain.vo.PracticeStatus;
import com.ieltspath.learning.domain.vo.TopicStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The one lesson gate shared by reading a lesson, exercise and essay submissions, and completion: pending review,
 * then topic, then earlier lessons, then the Practice of those lessons. Callers hold the learner's lock.
 */
@Component
public class LessonAccess {
    private final LearningContentClient content;
    private final LessonProgressRepository lessons;
    private final LearnerCurriculumRepository curricula;
    private final ReviewItemRepository reviews;
    private final KnowledgeEvidenceRepository evidence;
    private final RefreshLearningTopicsUseCase refreshTopics;
    private final PracticeProgress practice;
    private final LessonAccessGate gate = new LessonAccessGate();

    public LessonAccess(LearningContentClient content, LessonProgressRepository lessons,
                        LearnerCurriculumRepository curricula, ReviewItemRepository reviews,
                        KnowledgeEvidenceRepository evidence, RefreshLearningTopicsUseCase refreshTopics,
                        PracticeProgress practice) {
        this.content = content;
        this.lessons = lessons;
        this.curricula = curricula;
        this.reviews = reviews;
        this.evidence = evidence;
        this.refreshTopics = refreshTopics;
        this.practice = practice;
    }

    public Context authorize(UUID userId, UUID lessonId) {
        return authorize(userId, content.getLesson(lessonId));
    }

    public Context authorize(UUID userId, Lesson lesson) {
        var summaries = orderedLessons(content.getTopicLessons(lesson.topicId()));
        var progress = lessons.findByTopic(userId, lesson.topicId());
        boolean previousComplete = true;
        boolean found = false;
        List<UUID> previousIds = new ArrayList<>();
        for (var summary : summaries) {
            if (summary.lessonId().equals(lesson.lessonId())) { found = true; break; }
            previousComplete &= completed(progress.get(summary.lessonId()));
            previousIds.add(summary.lessonId());
        }
        var topic = topic(userId, lesson.topicId());
        List<UUID> needPractice = found && previousComplete
                ? lessonsNeedingPractice(userId, lesson.topicId(), previousIds, progress) : List.of();
        Set<UUID> previous = Set.copyOf(previousIds);
        gate.authorize(reviews.findPending(userId), null, skillsOf(lesson, topic), previous,
                status(userId, lesson.topicId()), previousComplete, needPractice);
        if (!found) throw new LearningRequestException(404, "NOT_FOUND", "Lesson was not found");
        return new Context(lesson, progress.get(lesson.lessonId()), previousComplete, previous, needPractice);
    }

    /**
     * The earlier lessons whose Practice is not passed, in lesson order. A lesson with no Practice counts as passed. A
     * pass found here is stored, so a lesson that opened is not locked again when Content later publishes more sets.
     * The first lesson of a topic has no earlier lesson, so Content is not asked for Practice at all.
     */
    private List<UUID> lessonsNeedingPractice(UUID userId, UUID topicId, List<UUID> previousIds,
                                              Map<UUID, LessonProgress> progress) {
        if (previousIds.isEmpty()) return List.of();
        Map<UUID, Boolean> completedByLesson = new LinkedHashMap<>();
        for (UUID id : previousIds) completedByLesson.put(id, completed(progress.get(id)));
        var states = practice.forTopic(userId, topicId, completedByLesson).clearances();
        practice.persistPassed(userId, states);
        return previousIds.stream().filter(id -> states.get(id).status() != PracticeStatus.PASSED).toList();
    }

    /** Checks the gate again after a curriculum refresh may have changed the learner's current topic. */
    public void reauthorize(UUID userId, Context context) {
        var topic = topic(userId, context.lesson().topicId());
        gate.authorize(reviews.findPending(userId), null, skillsOf(context.lesson(), topic),
                context.previousLessonIds(), status(userId, context.lesson().topicId()),
                context.previousLessonsComplete(), context.lessonsNeedingPractice());
    }

    public TopicProgress topic(UUID userId, UUID topicId) {
        var found = curricula.find(userId).topic(topicId);
        if (found.isEmpty() || found.get().skills().isEmpty()) {
            refreshTopics.execute(userId);
            found = curricula.find(userId).topic(topicId);
        }
        return found.orElse(new TopicProgress(topicId, null, null));
    }

    /** The skills the lesson teaches; a lesson Content sent without them takes its topic's skills. */
    public static Set<LearningSkill> skillsOf(Lesson lesson, TopicProgress topic) {
        return lesson.skills().isEmpty() ? topic.skills() : lesson.skills();
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

    /**
     * {@code previousLessonIds} are the lessons before this one in the topic; {@code lessonsNeedingPractice} are the
     * ones among them whose Practice was not passed when the lesson was opened.
     */
    public record Context(Lesson lesson, LessonProgress progress, boolean previousLessonsComplete,
                          Set<UUID> previousLessonIds, List<UUID> lessonsNeedingPractice) {}
}
