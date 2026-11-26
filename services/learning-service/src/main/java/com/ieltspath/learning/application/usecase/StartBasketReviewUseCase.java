package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.ReviewResult;
import com.ieltspath.learning.application.service.CompletedLessonLocator;
import com.ieltspath.learning.application.service.ErrorTrapCatalog;
import com.ieltspath.learning.application.service.PaywallGate;
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
import org.springframework.beans.factory.annotation.Value;
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
 * has a pending review returns that review, blocking or not.
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
    private final PaywallGate paywall;
    private final int reserve;
    private final TransactionTemplate transaction;
    private final Clock clock = Clock.systemUTC();

    @Autowired
    public StartBasketReviewUseCase(LearnerLock lock, MasteryProfileRepository profiles, ReviewItemRepository reviews,
                                    PracticeAttemptRepository attempts, CompletedLessonLocator lessonLocator,
                                    KnowledgeEvidenceRepository evidence,
                                    LearningContentClient content, GetReviewUseCase getReview,
                                    ErrorTrapCatalog errorTraps, PaywallGate paywall,
                                    @Value("${learning.free-practice.reserve:2}") int reserve,
                                    PlatformTransactionManager transactionManager) {
        this.lock = lock;
        this.profiles = profiles;
        this.reviews = reviews;
        this.attempts = attempts;
        this.lessonLocator = lessonLocator;
        this.evidence = evidence;
        this.content = content;
        this.getReview = getReview;
        this.errorTraps = errorTraps;
        this.paywall = paywall;
        this.reserve = reserve;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public ReviewResult execute(UUID userId, UUID knowledgePointId) {
        ErrorTrapCatalog.ErrorTraps traps = errorTraps == null ? null : errorTraps.load();
        if (traps != null && traps.parentByTrapKp().containsKey(knowledgePointId)) {
            throw new LearningRequestException(409, "NOT_IN_REVIEW_BASKET",
                    "Điểm kiến thức này không nằm trong giỏ ôn tập.");
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
                            "Chưa có bài học nào đã hoàn thành giảng dạy điểm kiến thức này."));
            Set<UUID> excluded = new HashSet<>(attempts.revealedPackageIds(userId));
            excluded.addAll(reviews.assignedPackageIds(userId));
            List<UUID> excludedList = excluded.stream().sorted(java.util.Comparator.comparing(UUID::toString)).toList();
            boolean priority = profile.tier() == MasteryTier.PRIORITY;
            boolean decayed = GetReviewBasketUseCase.isDecayed(profile);
            PaywallGate.Request paywallRequest = null;
            if (priority || decayed) {
                int unseen = content.practiceSetAvailability(List.of(knowledgePointId), excludedList,
                        GetReviewBasketUseCase.MIN_SET_QUESTIONS).getOrDefault(knowledgePointId, 0);
                if (unseen <= reserve) throw noUnrevealedSet();
                paywallRequest = requireKnowledgePointOpen(userId, knowledgePointId);
            }
            int candidateLimit = priority || decayed ? 10 : 1;
            List<LearningContentClient.PracticeSet> candidates = trapErrorCode == null
                    ? content.searchPracticeSets(knowledgePointId, excludedList,
                            GetReviewBasketUseCase.MIN_SET_QUESTIONS, candidateLimit, lessonId)
                    : content.searchPracticeSets(knowledgePointId, excludedList,
                            GetReviewBasketUseCase.MIN_SET_QUESTIONS, candidateLimit, lessonId, trapErrorCode);
            boolean premium = false;
            if ((priority || decayed) && candidates.stream().anyMatch(candidate -> candidate.requiredFeatureKey() != null)) {
                try {
                    premium = paywallRequest.premiumActive();
                } catch (AccessUnavailableException unavailable) {
                    premium = false;
                }
            }
            boolean premiumAllowed = premium;
            List<LearningContentClient.PracticeSet> eligibleCandidates = candidates.stream()
                    .filter(candidate -> !(priority || decayed) || candidate.requiredFeatureKey() == null
                            || premiumAllowed).toList();
            LearningContentClient.PracticeSet selected = eligibleCandidates.stream().findFirst()
                    .orElseThrow(StartBasketReviewUseCase::noUnrevealedSet);
            List<LearningContentClient.PracticeSet> candidatesForGet = priority || decayed
                    ? eligibleCandidates : null;

            StartOutcome outcome = transaction.execute(status -> {
                lock.lock(userId);
                Optional<PendingReview> concurrent = pending(userId, knowledgePointId);
                if (concurrent.isPresent()) return new StartOutcome(concurrent.get().reviewId(), false, null);
                MasteryProfile current = eligibleProfile(userId, knowledgePointId);
                if (current.tier() != profile.tier()) {
                    throw new LearningRequestException(409, "NOT_IN_REVIEW_BASKET",
                            "Điểm kiến thức này không nằm trong giỏ ôn tập.");
                }
                Set<UUID> assigned = new HashSet<>(attempts.revealedPackageIds(userId));
                assigned.addAll(reviews.assignedPackageIds(userId));
                if (assigned.contains(selected.packageId())) return new StartOutcome(null, true, null);
                LearningSkill skill = evidence.findMasteryHistories(userId, List.of(knowledgePointId)).stream()
                        .map(history -> history.skill()).filter(java.util.Objects::nonNull).findFirst().orElse(null);
                UUID id = UUID.randomUUID();
                String triggerKind = current.tier() == MasteryTier.PRIORITY ? "PRIORITY"
                        : GetReviewBasketUseCase.isDecayed(current) ? "DECAYED" : current.tier().name();
                if (reviews.insertOptional(userId, id, knowledgePointId, lessonId, skill, triggerKind)) {
                    return new StartOutcome(id, false, candidatesForGet);
                }
                return pending(userId, knowledgePointId).map(review -> new StartOutcome(review.reviewId(), false, null))
                        .orElseThrow(() -> noUnrevealedSet());
            });
            if (!outcome.candidateBecameUnavailable()) {
                return outcome.preparedCandidates() == null ? getReview.execute(userId, outcome.reviewId())
                        : getReview.execute(userId, outcome.reviewId(), outcome.preparedCandidates());
            }
            if (preparation == 1) throw noUnrevealedSet();
        }
        throw noUnrevealedSet();
    }

    private MasteryProfile eligibleProfile(UUID userId, UUID knowledgePointId) {
        MasteryProfile profile = profiles.find(userId, List.of(knowledgePointId)).get(knowledgePointId);
        if (profile == null) throw notInBasket();
        if (profile.tier() == MasteryTier.PRIORITY || GetReviewBasketUseCase.isDecayed(profile)) return profile;
        if (profile.tier() == MasteryTier.INSUFFICIENT
                || (profile.tier() == MasteryTier.MAINTAIN
                && !GetReviewBasketUseCase.isDue(profile, clock.instant()))) {
            throw notInBasket();
        }
        return profile;
    }

    private static LearningRequestException notInBasket() {
        return new LearningRequestException(409, "NOT_IN_REVIEW_BASKET",
                "Điểm kiến thức này không nằm trong giỏ ôn tập.");
    }

    private PaywallGate.Request requireKnowledgePointOpen(UUID userId, UUID knowledgePointId) {
        var topic = content.getTopicSequence().stream()
                .filter(candidate -> candidate.knowledgePoints().stream()
                        .anyMatch(point -> point.id().equals(knowledgePointId)))
                .findFirst().orElseThrow(() -> new LearningRequestException(409,
                        "NO_LESSON_FOR_KNOWLEDGE_POINT", "Chưa có bài học nào đã hoàn thành giảng dạy điểm kiến thức này."));
        var point = topic.knowledgePoints().stream().filter(candidate -> candidate.id().equals(knowledgePointId))
                .findFirst().orElseThrow();
        PaywallGate.Request request = paywall.forLearner(userId);
        request.requireKnowledgePointOpen(topic, point);
        return request;
    }

    private Optional<PendingReview> pending(UUID userId, UUID knowledgePointId) {
        return reviews.findPending(userId).stream()
                .filter(review -> review.knowledgePointId().equals(knowledgePointId)).findFirst();
    }

    private static LearningRequestException noUnrevealedSet() {
        return new LearningRequestException(409, "NO_UNREVEALED_SET",
                "Hiện không còn bộ bài tập mới nào cho điểm kiến thức này.");
    }

    private record StartOutcome(UUID reviewId, boolean candidateBecameUnavailable,
                                List<LearningContentClient.PracticeSet> preparedCandidates) {}
}
