package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.domain.aggregate.Hearts;
import com.ieltspath.learning.domain.exception.HeartsDepletedException;
import com.ieltspath.learning.domain.repository.HeartsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Hearts of a learner. Callers hold the learner lock. The maximum follows the plan: PREMIUM has more hearts, and when
 * Access cannot say which plan a learner is on, the maximum already stored is kept.
 */
@Component
public class HeartsService {
    public record Snapshot(int hearts, int maxHearts, Instant nextHeartAt, int refillCost) {}

    private final HeartsRepository repository;
    private final AccessClient access;
    private final int standardMax;
    private final int premiumMax;
    private final int refillCost;
    private final Clock clock = Clock.systemUTC();

    public HeartsService(HeartsRepository repository, AccessClient access,
                         @Value("${learning.gamification.hearts.max:5}") int standardMax,
                         @Value("${learning.gamification.hearts.premium-max:7}") int premiumMax,
                         @Value("${learning.gamification.hearts.refill-cost:5}") int refillCost) {
        this.repository = repository;
        this.access = access;
        this.standardMax = standardMax;
        this.premiumMax = premiumMax;
        this.refillCost = refillCost;
    }

    public Snapshot snapshot(UUID userId) {
        return view(load(userId));
    }

    /** Refuses a heart-costing start or submission while the learner has no hearts. */
    public void requireHearts(UUID userId) {
        Hearts hearts = load(userId);
        if (hearts.hearts() == 0) throw new HeartsDepletedException(hearts.nextHeartAt().orElse(null), refillCost);
    }

    /** Takes the hearts of the wrong objective answers of one new submission; the learner may end at zero. */
    public Snapshot lose(UUID userId, int wrongAnswers) {
        Hearts hearts = load(userId);
        hearts.lose(wrongAnswers, clock.instant());
        repository.save(hearts);
        return view(hearts);
    }

    /**
     * Buys a full refill for {@code refillCost} points. The points are taken first, under {@code requestId}, which Access
     * treats as idempotent: a repeat of the same request costs nothing more, so a failed save can be retried safely.
     */
    public Snapshot refill(UUID userId, UUID requestId) {
        Hearts hearts = load(userId);
        if (hearts.full()) throw new LearningRequestException(409, "HEARTS_FULL", "Hearts are already full");
        access.debit(userId, refillCost, "HEART_REFILL", requestId, requestId.toString(), "Hồi tim");
        hearts.refillFull(clock.instant());
        repository.save(hearts);
        return view(hearts);
    }

    /** A level-up refills the hearts for free. */
    public void refillOnLevelUp(UUID userId) {
        Hearts hearts = load(userId);
        hearts.refillFull(clock.instant());
        repository.save(hearts);
    }

    /** The learner's hearts now: the plan's maximum applied, the time regained, and saved. A new learner starts full. */
    private Hearts load(UUID userId) {
        Instant now = clock.instant();
        int max = maxFor(userId);
        Hearts hearts = repository.find(userId).orElseGet(() -> Hearts.start(userId, max, now));
        hearts.setMax(max, now);
        hearts.regen(now);
        repository.save(hearts);
        return hearts;
    }

    private int maxFor(UUID userId) {
        try {
            return access.premium() ? premiumMax : standardMax;
        } catch (AccessUnavailableException unavailable) {
            return repository.find(userId).map(Hearts::max).orElse(standardMax);
        }
    }

    private Snapshot view(Hearts hearts) {
        return new Snapshot(hearts.hearts(), hearts.max(), hearts.nextHeartAt().orElse(null), refillCost);
    }
}
