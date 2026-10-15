package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient.LessonPracticeSet;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.repository.LessonPracticePassRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.PracticeClearance.Clearance;
import com.ieltspath.learning.domain.service.PracticeClearance;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PendingReview;
import com.ieltspath.learning.domain.vo.PracticePassReason;
import com.ieltspath.learning.domain.vo.PracticeReviewState;
import com.ieltspath.learning.domain.vo.PracticeStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PracticeProgress {
    private final LearningContentClient content;
    private final PracticeAttemptRepository attempts;
    private final ReviewItemRepository reviews;
    private final LessonPracticePassRepository passes;
    private final PracticeClearance rule = new PracticeClearance();

    public PracticeProgress(LearningContentClient content,
                            PracticeAttemptRepository attempts, ReviewItemRepository reviews,
                            LessonPracticePassRepository passes) {
        this.content = content;
        this.attempts = attempts;
        this.reviews = reviews;
        this.passes = passes;
    }

    public record TopicState(Map<UUID, Clearance> clearances, List<PendingReview> pendingReviews) {}

    public TopicState forTopic(UUID userId, UUID topicId, Map<UUID, Boolean> open) {
        Map<UUID, List<LessonPracticeSet>> sets = new HashMap<>();
        var catalog = content.topicPracticeSets(topicId);
        if (catalog != null) for (var lesson : catalog.lessons()) {
            sets.put(lesson.lessonId(), lesson.practiceSets());
        }
        for (UUID lessonId : open.keySet()) sets.putIfAbsent(lessonId, List.of());
        var snapshot = reviews.findForTopic(userId, sets.keySet());
        var packageIds = sets.values().stream().flatMap(List::stream).map(LessonPracticeSet::packageId).toList();
        var topicAttempts = attempts.findForTopic(userId, sets.keySet(), packageIds);
        return new TopicState(derive(userId, open, sets, topicAttempts.firstPasses(),
                topicAttempts.submittedPackageIds(), snapshot.practice()), snapshot.pending());
    }

    public Clearance forLesson(UUID userId, UUID lessonId, boolean open, List<LessonPracticeSet> sets) {
        var facts = attempts.findForTopic(userId, List.of(lessonId),
                sets.stream().map(LessonPracticeSet::packageId).toList());
        return derive(userId, Map.of(lessonId, open), Map.of(lessonId, sets),
                facts.firstPasses(), facts.submittedPackageIds(),
                reviews.findPracticeByLessons(userId, List.of(lessonId))).get(lessonId);
    }

    /** The caller holds the learner lock and has opened the lesson. Only write paths invoke this method. */
    public Clearance refreshPassForLesson(UUID userId, UUID lessonId) {
        var sets = content.lessonPracticeSets(lessonId);
        Clearance state = forLesson(userId, lessonId, true, sets == null ? List.of() : sets);
        persistPassed(userId, Map.of(lessonId, state));
        return state;
    }

    /** The caller holds the learner lock. Stored rows are never overwritten. */
    public void persistPassed(UUID userId, Map<UUID, Clearance> states) {
        Map<UUID, PracticePassReason> newlyPassed = new HashMap<>();
        states.forEach((id, state) -> {
            if (state.status() == PracticeStatus.PASSED) newlyPassed.put(id, state.reason());
        });
        passes.insertIfAbsent(userId, newlyPassed);
    }

    private Map<UUID, Clearance> derive(UUID userId, Map<UUID, Boolean> open,
                                        Map<UUID, List<LessonPracticeSet>> sets,
                                        List<PracticeAttemptRepository.FirstPass> firstPasses, Set<UUID> submitted,
                                        List<PracticeReviewState> allReviews) {
        Map<UUID, PracticePassReason> stored = passes.findByLessons(userId, sets.keySet());
        Map<UUID, List<PracticeClearance.AttemptFact>> attemptFacts = new HashMap<>();
        for (var pass : firstPasses) {
            attemptFacts.computeIfAbsent(pass.lessonId(), ignored -> new ArrayList<>())
                    .add(new PracticeClearance.AttemptFact(pass.packageId(), pass.passed(), true, pass.passedSkills()));
        }
        Map<UUID, List<PracticeClearance.ReviewFact>> reviewFacts = new HashMap<>();
        for (PracticeReviewState review : allReviews) {
            reviewFacts.computeIfAbsent(review.lessonId(), ignored -> new ArrayList<>())
                    .add(new PracticeClearance.ReviewFact(review.status(), review.skill()));
        }
        Map<UUID, Clearance> result = new HashMap<>();
        for (var entry : sets.entrySet()) {
            UUID lessonId = entry.getKey();
            Map<UUID, Set<LearningSkill>> packages = new HashMap<>();
            // A set that needs a paid feature is never required, or a learner without the plan could not finish.
            entry.getValue().stream().filter(set -> set.requiredFeatureKey() == null)
                    .forEach(set -> packages.put(set.packageId(), set.skills()));
            result.put(lessonId, rule.derive(open.getOrDefault(lessonId, false), packages,
                    attemptFacts.getOrDefault(lessonId, List.of()), submitted,
                    reviewFacts.getOrDefault(lessonId, List.of()), stored.get(lessonId)));
        }
        return result;
    }
}
