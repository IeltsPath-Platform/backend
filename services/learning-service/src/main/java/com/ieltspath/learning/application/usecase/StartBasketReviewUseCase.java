package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.ReviewResult;
import com.ieltspath.learning.application.service.CompletedLessonLocator;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.MasteryTier;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final Clock clock = Clock.systemUTC();

    public StartBasketReviewUseCase(LearnerLock lock, MasteryProfileRepository profiles, ReviewItemRepository reviews,
                                    PracticeAttemptRepository attempts, CompletedLessonLocator lessonLocator,
                                    KnowledgeEvidenceRepository evidence,
                                    LearningContentClient content, GetReviewUseCase getReview) {
        this.lock = lock;
        this.profiles = profiles;
        this.reviews = reviews;
        this.attempts = attempts;
        this.lessonLocator = lessonLocator;
        this.evidence = evidence;
        this.content = content;
        this.getReview = getReview;
    }

    @Transactional
    public ReviewResult execute(UUID userId, UUID knowledgePointId) {
        lock.lock(userId);
        Optional<PendingReview> existing = reviews.findPending(userId).stream()
                .filter(review -> review.knowledgePointId().equals(knowledgePointId)).findFirst();
        if (existing.isPresent()) return getReview.execute(userId, existing.get().reviewId());

        MasteryProfile profile = profiles.find(userId, List.of(knowledgePointId)).get(knowledgePointId);
        if (profile == null || profile.tier() == MasteryTier.PRIORITY || profile.tier() == MasteryTier.INSUFFICIENT
                || (profile.tier() == MasteryTier.MAINTAIN && !GetReviewBasketUseCase.isDue(profile, clock.instant()))) {
            throw new LearningRequestException(409, "NOT_IN_REVIEW_BASKET",
                    "This knowledge point is not in the review basket");
        }
        UUID lessonId = lessonLocator.teaching(userId, knowledgePointId).orElseThrow(() -> new LearningRequestException(409,
                "NO_LESSON_FOR_KNOWLEDGE_POINT", "No completed lesson teaches this knowledge point"));
        Set<UUID> excluded = new HashSet<>(attempts.revealedPackageIds(userId));
        excluded.addAll(reviews.assignedPackageIds(userId));
        if (content.searchPracticeSets(knowledgePointId, excluded.stream().toList(),
                GetReviewBasketUseCase.MIN_SET_QUESTIONS, 1, lessonId).isEmpty()) {
            throw new LearningRequestException(409, "NO_UNREVEALED_SET",
                    "No practice set the learner has not seen is left for this knowledge point");
        }
        LearningSkill skill = evidence.findMasteryHistories(userId, List.of(knowledgePointId)).stream()
                .map(history -> history.skill()).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        UUID reviewId = UUID.randomUUID();
        if (!reviews.insertOptional(userId, reviewId, knowledgePointId, lessonId, skill, profile.tier().name())) {
            // A pending review appeared since the check above; open that one.
            return getReview.execute(userId, reviews.findPending(userId).stream()
                    .filter(review -> review.knowledgePointId().equals(knowledgePointId)).findFirst().orElseThrow()
                    .reviewId());
        }
        return getReview.execute(userId, reviewId);
    }
}
