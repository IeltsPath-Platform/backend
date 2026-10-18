package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.ReviewBasketResult;
import com.ieltspath.learning.application.result.ReviewBasketResult.OptionalItem;
import com.ieltspath.learning.application.result.ReviewBasketResult.RequiredItem;
import com.ieltspath.learning.application.service.KnowledgePointDirectory;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.MasteryTier;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The learner's review basket: what is required, what is worth consolidating, and what is due to maintain. */
@Service
public class GetReviewBasketUseCase {
    /** The smallest set Content may hand out as a review. */
    static final int MIN_SET_QUESTIONS = 3;

    private final MasteryProfileRepository profiles;
    private final ReviewItemRepository reviews;
    private final PracticeAttemptRepository attempts;
    private final LearningContentClient content;
    private final KnowledgePointDirectory directory;
    private final Clock clock = Clock.systemUTC();

    public GetReviewBasketUseCase(MasteryProfileRepository profiles, ReviewItemRepository reviews,
                                  PracticeAttemptRepository attempts, LearningContentClient content,
                                  KnowledgePointDirectory directory) {
        this.profiles = profiles;
        this.reviews = reviews;
        this.attempts = attempts;
        this.content = content;
        this.directory = directory;
    }

    @Transactional(readOnly = true)
    public ReviewBasketResult execute(UUID userId) {
        Instant now = clock.instant();
        List<PendingReview> pending = reviews.findPending(userId);
        Map<UUID, UUID> optionalReviewByPoint = new HashMap<>();
        List<PendingReview> blocking = new ArrayList<>();
        for (PendingReview review : pending) {
            if (review.blocking()) blocking.add(review);
            else optionalReviewByPoint.put(review.knowledgePointId(), review.reviewId());
        }

        // A point with a blocking review is listed as required, not offered again as an optional one.
        Set<UUID> blockedPoints = new HashSet<>();
        blocking.forEach(review -> blockedPoints.add(review.knowledgePointId()));
        List<MasteryProfile> candidates = profiles.findAll(userId).stream()
                .filter(profile -> !blockedPoints.contains(profile.knowledgePointId()))
                .filter(profile -> profile.tier() == MasteryTier.CONSOLIDATE || isDue(profile, now)).toList();
        if (blocking.isEmpty() && candidates.isEmpty()) return new ReviewBasketResult(List.of(), List.of(), List.of());

        Map<UUID, KnowledgePointDirectory.Entry> described = directory.describe();
        Map<UUID, Integer> availability = availability(userId, candidates);

        List<RequiredItem> required = blocking.stream().map(review -> {
            var meta = described.getOrDefault(review.knowledgePointId(), KnowledgePointDirectory.Entry.UNKNOWN);
            return new RequiredItem(review.reviewId(), review.knowledgePointId(), meta.code(), meta.name(),
                    review.lessonId(), review.skill());
        }).toList();
        List<OptionalItem> consolidate = new ArrayList<>();
        List<OptionalItem> due = new ArrayList<>();
        for (MasteryProfile profile : candidates) {
            var meta = described.getOrDefault(profile.knowledgePointId(), KnowledgePointDirectory.Entry.UNKNOWN);
            boolean isDue = profile.tier() == MasteryTier.MAINTAIN;
            var item = new OptionalItem(profile.knowledgePointId(), meta.code(), meta.name(), meta.skill(),
                    profile.tier().name(), profile.mastery(), profile.nextReviewAt(), isDue ? "DUE" : "CONSOLIDATE",
                    availability.getOrDefault(profile.knowledgePointId(), 0) > 0
                            || optionalReviewByPoint.containsKey(profile.knowledgePointId()),
                    optionalReviewByPoint.get(profile.knowledgePointId()));
            (isDue ? due : consolidate).add(item);
        }
        Comparator<OptionalItem> weakestFirst = Comparator.comparingDouble(OptionalItem::mastery)
                .thenComparing(item -> item.code() == null ? "" : item.code());
        consolidate.sort(weakestFirst);
        due.sort(Comparator.comparing(OptionalItem::dueAt));
        return new ReviewBasketResult(required, List.copyOf(consolidate), List.copyOf(due));
    }

    static boolean isDue(MasteryProfile profile, Instant now) {
        return profile.tier() == MasteryTier.MAINTAIN && profile.nextReviewAt() != null
                && !profile.nextReviewAt().isAfter(now);
    }

    /** How many sets Content still has per point that the learner has not seen. */
    private Map<UUID, Integer> availability(UUID userId, List<MasteryProfile> candidates) {
        if (candidates.isEmpty()) return Map.of();
        Set<UUID> excluded = new HashSet<>(attempts.revealedPackageIds(userId));
        excluded.addAll(reviews.assignedPackageIds(userId));
        Map<UUID, Integer> counts = content.practiceSetAvailability(
                candidates.stream().map(MasteryProfile::knowledgePointId)
                        .sorted(Comparator.comparing(UUID::toString)).toList(),
                excluded.stream().sorted(Comparator.comparing(UUID::toString)).toList(), MIN_SET_QUESTIONS);
        return counts == null ? Map.of() : counts;
    }
}
