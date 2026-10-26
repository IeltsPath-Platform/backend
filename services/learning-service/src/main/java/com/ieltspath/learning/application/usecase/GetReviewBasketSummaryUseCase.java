package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.result.ReviewBasketSummaryResult;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The counts behind the review basket's badge. Unlike the basket it never asks Content whether a set is left, so it is
 * cheap enough to read on every page: the required count is the learner's blocking reviews, and a due point counts
 * unless a blocking review already holds it, as in the basket.
 */
@Service
public class GetReviewBasketSummaryUseCase {
    private final MasteryProfileRepository profiles;
    private final ReviewItemRepository reviews;
    private final Clock clock = Clock.systemUTC();

    public GetReviewBasketSummaryUseCase(MasteryProfileRepository profiles, ReviewItemRepository reviews) {
        this.profiles = profiles;
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public ReviewBasketSummaryResult execute(UUID userId) {
        int required = 0;
        Set<UUID> blocked = new HashSet<>();
        for (PendingReview review : reviews.findPending(userId)) {
            if (!review.blocking()) continue;
            required++;
            blocked.add(review.knowledgePointId());
        }
        return new ReviewBasketSummaryResult(required, profiles.countDue(userId, clock.instant(), blocked));
    }
}
