package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.TopicCatalog;
import com.ieltspath.learning.application.result.SuggestionResult;
import com.ieltspath.learning.application.service.KnowledgePointDirectory;
import com.ieltspath.learning.application.service.PaywallGate;
import com.ieltspath.learning.domain.aggregate.LessonProgress;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.PracticeTestAssignmentRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.SuggestionRules;
import com.ieltspath.learning.domain.service.SuggestionRules.Candidate;
import com.ieltspath.learning.domain.service.SuggestionRules.Reason;
import com.ieltspath.learning.domain.service.SuggestionRules.Target;
import com.ieltspath.learning.domain.service.SuggestionRules.Type;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.MasteryTier;
import com.ieltspath.learning.domain.vo.TopicStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Up to five optional suggestions from published content: weak points to consolidate or due for review first, then
 * lessons for points that build on what the learner already holds firmly. Nothing the learner has done and nothing above
 * the course band they are learning is offered, and a suggestion never blocks progress.
 */
@Service
public class GetSuggestionsUseCase {
    private static final int MAX_TARGETS = 20;
    private static final int TREE_PROBE = 50;

    private final MasteryProfileRepository profiles;
    private final LearnerCurriculumRepository curricula;
    private final LearnerPlacementRepository placements;
    private final LessonProgressRepository lessons;
    private final PracticeAttemptRepository attempts;
    private final ReviewItemRepository reviews;
    private final PracticeTestAssignmentRepository practiceTests;
    private final LearningContentClient content;
    private final TopicCatalog topicCatalog;
    private final KnowledgePointDirectory directory;
    private final PaywallGate paywall;
    private final SuggestionRules rules = new SuggestionRules();
    private final Clock clock = Clock.systemUTC();

    public GetSuggestionsUseCase(MasteryProfileRepository profiles, LearnerCurriculumRepository curricula,
                                 LearnerPlacementRepository placements, LessonProgressRepository lessons,
                                 PracticeAttemptRepository attempts, ReviewItemRepository reviews,
                                 PracticeTestAssignmentRepository practiceTests, LearningContentClient content,
                                 TopicCatalog topicCatalog, KnowledgePointDirectory directory, PaywallGate paywall) {
        this.profiles = profiles;
        this.curricula = curricula;
        this.placements = placements;
        this.lessons = lessons;
        this.attempts = attempts;
        this.reviews = reviews;
        this.practiceTests = practiceTests;
        this.content = content;
        this.topicCatalog = topicCatalog;
        this.directory = directory;
        this.paywall = paywall;
    }

    @Transactional(readOnly = true)
    public SuggestionResult execute(UUID userId) {
        Map<UUID, KnowledgePointDirectory.Entry> described = directory.describeForDisplay();
        Optional<BigDecimal> maxBand = currentBand(userId, described);
        if (maxBand.isEmpty()) return new SuggestionResult(List.of());

        var now = clock.instant();
        List<MasteryProfile> all = profiles.findAll(userId);
        Set<UUID> blocked = new HashSet<>();
        reviews.findPending(userId).stream().filter(review -> review.blocking())
                .forEach(review -> blocked.add(review.knowledgePointId()));
        List<Target> targets = new ArrayList<>();
        all.stream().filter(profile -> !blocked.contains(profile.knowledgePointId()))
                .filter(profile -> profile.tier() == MasteryTier.CONSOLIDATE)
                .sorted(Comparator.comparingDouble(MasteryProfile::mastery))
                .forEach(profile -> targets.add(new Target(profile.knowledgePointId(), Reason.CONSOLIDATE)));
        all.stream().filter(profile -> !blocked.contains(profile.knowledgePointId()))
                .filter(profile -> GetReviewBasketUseCase.isDue(profile, now))
                .sorted(Comparator.comparing(MasteryProfile::nextReviewAt))
                .forEach(profile -> targets.add(new Target(profile.knowledgePointId(), Reason.DUE)));
        targets.addAll(nextInTree(all, described));
        if (targets.isEmpty()) return new SuggestionResult(List.of());
        List<Target> limited = targets.stream().limit(MAX_TARGETS).toList();

        var found = content.suggestionCandidates(limited.stream().map(Target::knowledgePointId).distinct().toList(),
                maxBand.get());
        List<Candidate> candidates = candidates(userId, found);
        List<SuggestionResult.Item> items = rules.choose(limited, candidates, maxBand.get()).stream()
                .map(suggestion -> item(suggestion, described)).toList();
        return new SuggestionResult(items);
    }

    /** The highest band among courses the learner is working in; the placement band before any topic is open. */
    private Optional<BigDecimal> currentBand(UUID userId, Map<UUID, KnowledgePointDirectory.Entry> described) {
        var curriculum = curricula.find(userId);
        var statuses = curriculum.statuses();
        Set<UUID> activeTopics = curriculum.topics().stream()
                .filter(topic -> statuses.get(topic.topicId()) == TopicStatus.IN_PROGRESS)
                .map(topic -> topic.courseId()).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Optional<BigDecimal> fromCourses = topicCatalog.topics(false).stream()
                .filter(topic -> topic.course() != null && activeTopics.contains(topic.course().courseId()))
                .map(topic -> topic.course().bandLevel()).max(Comparator.naturalOrder());
        return fromCourses.or(() -> placements.find(userId).map(placement -> placement.band().value()));
    }

    /** Points that build only on points the learner holds firmly and has no evidence on yet. */
    private List<Target> nextInTree(List<MasteryProfile> all, Map<UUID, KnowledgePointDirectory.Entry> described) {
        Set<UUID> firm = all.stream().filter(profile -> profile.tier() == MasteryTier.MAINTAIN)
                .map(MasteryProfile::knowledgePointId).collect(Collectors.toSet());
        if (firm.isEmpty()) return List.of();
        // A point without enough evidence to judge is still new to the learner.
        Set<UUID> known = all.stream().filter(profile -> profile.tier() != MasteryTier.INSUFFICIENT)
                .map(MasteryProfile::knowledgePointId).collect(Collectors.toSet());
        List<UUID> unseen = described.keySet().stream().filter(kp -> !known.contains(kp))
                .sorted(Comparator.comparing(UUID::toString)).limit(TREE_PROBE).toList();
        if (unseen.isEmpty()) return List.of();
        Map<UUID, List<UUID>> prerequisites = content.prerequisites(unseen);
        if (prerequisites == null) return List.of();
        return unseen.stream().filter(kp -> {
            List<UUID> needs = prerequisites.getOrDefault(kp, List.of());
            return !needs.isEmpty() && firm.containsAll(needs);
        }).map(kp -> new Target(kp, Reason.NEXT_IN_TREE)).toList();
    }

    private List<Candidate> candidates(UUID userId, LearningContentClient.SuggestionCandidates found) {
        Set<UUID> doneSets = new HashSet<>(attempts.revealedPackageIds(userId));
        doneSets.addAll(reviews.assignedPackageIds(userId));
        Set<UUID> doneLessons = lessons.findCompleted(userId).stream().map(LessonProgress::lessonId)
                .collect(Collectors.toSet());
        List<Candidate> candidates = new ArrayList<>();
        for (var pkg : found.packages()) {
            Type type = "PRACTICE_TEST".equals(pkg.type()) ? Type.PRACTICE_TEST : Type.PRACTICE_SET;
            boolean seen = doneSets.contains(pkg.packageId())
                    || (type == Type.PRACTICE_TEST && practiceTests.find(userId, pkg.packageId()).isPresent());
            candidates.add(new Candidate(type, pkg.packageId(), pkg.packageVersionId(), pkg.title(),
                    pkg.knowledgePointIds(), pkg.band(), pkg.skill(), seen));
        }
        // A lesson the learner cannot open is not offered; one already completed stays, marked as done.
        PaywallGate.Request access = paywall.forLearner(userId);
        for (var lesson : found.lessons()) {
            boolean done = doneLessons.contains(lesson.lessonId());
            if (!done && !openForSuggestion(access, lesson)) continue;
            candidates.add(new Candidate(Type.LESSON, lesson.lessonId(), null, lesson.title(),
                    lesson.knowledgePointIds(), lesson.band(), lesson.skill(), done));
        }
        return candidates;
    }

    /**
     * Whether the lesson is open to the learner under the same rule that opens it for reading. Suggestions are optional, so
     * when Access cannot say whether the learner has Premium, a closed lesson is left out instead of failing the request.
     */
    private static boolean openForSuggestion(PaywallGate.Request access, LearningContentClient.LessonCandidate lesson) {
        // The gate's skills, the same ones the reader uses; a null set (older Content) grants no skill, only the open rules.
        Set<LearningSkill> skills = lesson.skills() == null ? Set.of() : lesson.skills();
        try {
            // Not completed here, so the completion-time grandfathering of a lesson does not apply.
            return access.lessonOpen(lesson.courseId(), lesson.skillPosition(), skills, false);
        } catch (AccessUnavailableException unavailable) {
            return false;
        }
    }

    private static SuggestionResult.Item item(SuggestionRules.Suggestion suggestion,
                                              Map<UUID, KnowledgePointDirectory.Entry> described) {
        Candidate candidate = suggestion.candidate();
        var meta = described.getOrDefault(suggestion.knowledgePointId(), KnowledgePointDirectory.Entry.UNKNOWN);
        String path = switch (suggestion.type()) {
            case LESSON -> "/api/learning/lessons/" + candidate.id();
            case PRACTICE_SET -> suggestion.reason() == Reason.NEXT_IN_TREE ? null
                    : "/api/learning/review-basket/" + suggestion.knowledgePointId() + "/start";
            case PRACTICE_TEST -> null;
        };
        return new SuggestionResult.Item(suggestion.type().name(), candidate.id(), candidate.title(),
                suggestion.knowledgePointId(), meta.code(), suggestion.reason().name(), candidate.band(),
                candidate.skill(), path);
    }
}
