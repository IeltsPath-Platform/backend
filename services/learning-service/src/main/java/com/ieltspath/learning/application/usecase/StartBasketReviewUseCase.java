package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.ReviewResult;
import com.ieltspath.learning.application.service.CompletedLessonLocator;
import com.ieltspath.learning.application.service.ErrorTrapCatalog;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.MasteryTier;
import com.ieltspath.learning.domain.vo.PendingReview;
import com.ieltspath.learning.domain.service.MainErrorRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Starts an optional review of a knowledge point from the review basket and gives its first set. A point that already
 * has a pending review returns that review, blocking or not. A point at the priority tier is not in the basket.
 */
@Service
public class StartBasketReviewUseCase {
    private final LearnerLock lock;
    private final MasteryProfileRepository profiles;
    private final ReviewItemRepository reviews;
    private final PracticeAttemptRepository attempts;
    private final CompletedLessonLocator lessonLocator;
    private final KnowledgeEvidenceRepository evidence;
    private final LearningContentClient content;
    private final GetReviewUseCase getReview;
    private final ErrorTrapCatalog errorTraps;
    private final TransactionTemplate transaction;
    private final Clock clock = Clock.systemUTC();

    @Autowired
    public StartBasketReviewUseCase(LearnerLock lock, MasteryProfileRepository profiles, ReviewItemRepository reviews,
                                    PracticeAttemptRepository attempts, CompletedLessonLocator lessonLocator,
                                    KnowledgeEvidenceRepository evidence,
                                    LearningContentClient content, GetReviewUseCase getReview,
                                    ErrorTrapCatalog errorTraps, PlatformTransactionManager transactionManager) {
        this.lock = lock;
        this.profiles = profiles;
        this.reviews = reviews;
        this.attempts = attempts;
        this.lessonLocator = lessonLocator;
        this.evidence = evidence;
        this.content = content;
        this.getReview = getReview;
        this.errorTraps = errorTraps;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public ReviewResult execute(UUID userId, UUID knowledgePointId) {
        ErrorTrapCatalog.ErrorTraps traps = errorTraps == null ? null : errorTraps.load();
        if (traps != null && traps.parentByTrapKp().containsKey(knowledgePointId)) {
            throw new LearningRequestException(409, "NOT_IN_REVIEW_BASKET",
                    "This knowledge point is not in the review basket");
        }
        Optional<PendingReview> existing = reviews.findPending(userId).stream()
                .filter(review -> review.knowledgePointId().equals(knowledgePointId)).findFirst();
        if (existing.isPresent()) return getReview.execute(userId, existing.get().reviewId());

        String trapErrorCode = traps == null ? null : MainErrorRule.mainErrorCode(knowledgePointId,
                profiles.findErrorTrapProfiles(userId), traps).orElse(null);
        for (int preparation = 0; preparation < 2; preparation++) {
            MasteryProfile profile = eligibleProfile(userId, knowledgePointId);
            UUID lessonId = lessonLocator.teaching(userId, knowledgePointId).orElseThrow(() ->
                    new LearningRequestException(409, "NO_LESSON_FOR_KNOWLEDGE_POINT",
                            "No completed lesson teaches this knowledge point"));
            Set<UUID> excluded = new HashSet<>(attempts.revealedPackageIds(userId));
            excluded.addAll(reviews.assignedPackageIds(userId));
            List<LearningContentClient.PracticeSet> candidates = trapErrorCode == null
                    ? content.searchPracticeSets(knowledgePointId, excluded.stream().toList(),
                            GetReviewBasketUseCase.MIN_SET_QUESTIONS, 1, lessonId)
                    : content.searchPracticeSets(knowledgePointId, excluded.stream().toList(),
                            GetReviewBasketUseCase.MIN_SET_QUESTIONS, 1, lessonId, trapErrorCode);
            if (candidates.isEmpty()) throw noUnrevealedSet();
            LearningContentClient.PracticeSet selected = candidates.getFirst();

            StartOutcome outcome = transaction.execute(status -> {
                lock.lock(userId);
                Optional<PendingReview> concurrent = pending(userId, knowledgePointId);
                if (concurrent.isPresent()) return new StartOutcome(concurrent.get().reviewId(), false);
                MasteryProfile current = eligibleProfile(userId, knowledgePointId);
                if (current.tier() != profile.tier()) {
                    throw new LearningRequestException(409, "NOT_IN_REVIEW_BASKET",
                            "This knowledge point is not in the review basket");
                }
                Set<UUID> assigned = new HashSet<>(attempts.revealedPackageIds(userId));
                assigned.addAll(reviews.assignedPackageIds(userId));
                if (assigned.contains(selected.packageId())) return new StartOutcome(null, true);
                LearningSkill skill = evidence.findMasteryHistories(userId, List.of(knowledgePointId)).stream()
                        .map(history -> history.skill()).filter(java.util.Objects::nonNull).findFirst().orElse(null);
                UUID id = UUID.randomUUID();
                if (reviews.insertOptional(userId, id, knowledgePointId, lessonId, skill, current.tier().name())) {
                    return new StartOutcome(id, false);
                }
                return pending(userId, knowledgePointId).map(review -> new StartOutcome(review.reviewId(), false))
                        .orElseThrow(() -> noUnrevealedSet());
            });
            if (!outcome.candidateBecameUnavailable()) return getReview.execute(userId, outcome.reviewId());
            if (preparation == 1) throw noUnrevealedSet();
        }
        throw noUnrevealedSet();
    }

    private MasteryProfile eligibleProfile(UUID userId, UUID knowledgePointId) {
        MasteryProfile profile = profiles.find(userId, List.of(knowledgePointId)).get(knowledgePointId);
        if (profile == null || profile.tier() == MasteryTier.PRIORITY || profile.tier() == MasteryTier.INSUFFICIENT
                || (profile.tier() == MasteryTier.MAINTAIN && !GetReviewBasketUseCase.isDue(profile, clock.instant()))) {
            throw new LearningRequestException(409, "NOT_IN_REVIEW_BASKET",
                    "This knowledge point is not in the review basket");
        }
        return profile;
    }

    private Optional<PendingReview> pending(UUID userId, UUID knowledgePointId) {
        return reviews.findPending(userId).stream()
                .filter(review -> review.knowledgePointId().equals(knowledgePointId)).findFirst();
    }

    private static LearningRequestException noUnrevealedSet() {
        return new LearningRequestException(409, "NO_UNREVEALED_SET",
                "No practice set the learner has not seen is left for this knowledge point");
    }

    private record StartOutcome(UUID reviewId, boolean candidateBecameUnavailable) {}
}
