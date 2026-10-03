package com.group01.learning.application.service;

import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.LessonPracticeSet;
import com.group01.learning.domain.aggregate.PracticeAttempt;
import com.group01.learning.domain.repository.LessonPracticePassRepository;
import com.group01.learning.domain.repository.LessonProgressRepository;
import com.group01.learning.domain.repository.PracticeAttemptRepository;
import com.group01.learning.domain.repository.ReviewItemRepository;
import com.group01.learning.domain.service.PracticeClearance;
import com.group01.learning.domain.service.PracticeClearance.Clearance;
import com.group01.learning.domain.vo.PracticePassReason;
import com.group01.learning.domain.vo.PracticeStatus;
import com.group01.learning.domain.vo.PracticeReviewState;
import com.group01.learning.domain.vo.PendingReview;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PracticeProgress {
    private final LearningContentClient content;
    private final LessonProgressRepository lessons;
    private final PracticeAttemptRepository attempts;
    private final ReviewItemRepository reviews;
    private final LessonPracticePassRepository passes;
    private final PracticeClearance rule = new PracticeClearance();

    public PracticeProgress(LearningContentClient content, LessonProgressRepository lessons,
                            PracticeAttemptRepository attempts, ReviewItemRepository reviews,
                            LessonPracticePassRepository passes) {
        this.content = content;
        this.lessons = lessons;
        this.attempts = attempts;
        this.reviews = reviews;
        this.passes = passes;
    }

    public record TopicState(Map<UUID, Clearance> clearances, List<PendingReview> pendingReviews) {}

    public TopicState forTopic(UUID userId, UUID topicId, Map<UUID, Boolean> completed) {
        Map<UUID, List<LessonPracticeSet>> sets = new HashMap<>();
        var catalog = content.topicPracticeSets(topicId);
        if (catalog != null) for (var lesson : catalog.lessons()) {
            sets.put(lesson.lessonId(), lesson.practiceSets());
        }
        for (UUID lessonId : completed.keySet()) sets.putIfAbsent(lessonId, List.of());
        var snapshot = reviews.findForTopic(userId, sets.keySet());
        var packageIds = sets.values().stream().flatMap(List::stream).map(LessonPracticeSet::packageId).toList();
        var topicAttempts = attempts.findForTopic(userId, sets.keySet(), packageIds);
        return new TopicState(derive(userId, completed, sets, topicAttempts.firstPasses(),
                topicAttempts.revealedPackageIds(), snapshot.practice()), snapshot.pending());
    }

    public Clearance forLesson(UUID userId, UUID lessonId, boolean completed, List<LessonPracticeSet> sets) {
        var facts = attempts.findForTopic(userId, List.of(lessonId),
                sets.stream().map(LessonPracticeSet::packageId).toList());
        return derive(userId, Map.of(lessonId, completed), Map.of(lessonId, sets),
                facts.firstPasses(), facts.revealedPackageIds(),
                reviews.findPracticeByLessons(userId, List.of(lessonId))).get(lessonId);
    }

    /** The caller holds the learner lock. Only write paths invoke this method. */
    public void refreshPassForLesson(UUID userId, UUID lessonId) {
        boolean completed = lessons.find(userId, lessonId).map(LessonAccess::completed).orElse(false);
        var sets = content.lessonPracticeSets(lessonId);
        Clearance state = forLesson(userId, lessonId, completed, sets == null ? List.of() : sets);
        persistPassed(userId, Map.of(lessonId, state));
    }

    /** The caller holds the learner lock. Stored rows are never overwritten. */
    public void persistPassed(UUID userId, Map<UUID, Clearance> states) {
        Map<UUID, PracticePassReason> newlyPassed = new HashMap<>();
        states.forEach((id, state) -> {
            if (state.status() == PracticeStatus.PASSED) newlyPassed.put(id, state.reason());
        });
        passes.insertIfAbsent(userId, newlyPassed);
    }

    private Map<UUID, Clearance> derive(UUID userId, Map<UUID, Boolean> completed,
                                        Map<UUID, List<LessonPracticeSet>> sets,
                                        List<PracticeAttemptRepository.FirstPass> firstPasses, Set<UUID> revealed,
                                        List<PracticeReviewState> allReviews) {
        Map<UUID, PracticePassReason> stored = passes.findByLessons(userId, sets.keySet());
        Map<UUID, List<PracticeClearance.AttemptFact>> attemptFacts = new HashMap<>();
        for (var pass : firstPasses) {
            attemptFacts.computeIfAbsent(pass.lessonId(), ignored -> new ArrayList<>())
                    .add(new PracticeClearance.AttemptFact(pass.packageId(), true, true));
        }
        Map<UUID, List<PracticeClearance.ReviewFact>> reviewFacts = new HashMap<>();
        for (PracticeReviewState review : allReviews) {
            reviewFacts.computeIfAbsent(review.lessonId(), ignored -> new ArrayList<>())
                    .add(new PracticeClearance.ReviewFact(review.status()));
        }
        Map<UUID, Clearance> result = new HashMap<>();
        for (var entry : sets.entrySet()) {
            UUID lessonId = entry.getKey();
            Set<UUID> packages = entry.getValue().stream().map(LessonPracticeSet::packageId)
                    .collect(Collectors.toSet());
            result.put(lessonId, rule.derive(completed.getOrDefault(lessonId, false), packages,
                    attemptFacts.getOrDefault(lessonId, List.of()), revealed,
                    reviewFacts.getOrDefault(lessonId, List.of()), stored.get(lessonId)));
        }
        return result;
    }
}
