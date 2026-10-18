package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.service.MasteryPolicy;
import com.ieltspath.learning.domain.vo.MasteryTier;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One learner's standing on one knowledge point: the score, its tier and when it is due for review. {@code confidence}
 * is the decayed weight of the evidence behind an MP-1.0 score, null under the other formula.
 */
public record MasteryProfile(UUID userId, UUID knowledgePointId, double mastery, MasteryTier tier, int evidenceCount,
                             int successStreak, Instant lastEvidenceAt, Instant nextReviewAt, Long medianDurationMs,
                             List<String> topErrorCodes, Double confidence) {
    public MasteryProfile {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(knowledgePointId, "knowledgePointId");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(lastEvidenceAt, "lastEvidenceAt");
        topErrorCodes = List.copyOf(topErrorCodes);
    }

    public MasteryProfile(UUID userId, UUID knowledgePointId, double mastery, MasteryTier tier, int evidenceCount,
                          int successStreak, Instant lastEvidenceAt, Instant nextReviewAt, Long medianDurationMs,
                          List<String> topErrorCodes) {
        this(userId, knowledgePointId, mastery, tier, evidenceCount, successStreak, lastEvidenceAt, nextReviewAt,
                medianDurationMs, topErrorCodes, null);
    }

    /** The same standing under other rules: only the tier and the review date can change. */
    public MasteryProfile under(MasteryPolicy policy) {
        MasteryTier newTier = policy.tier(mastery);
        return new MasteryProfile(userId, knowledgePointId, mastery, newTier, evidenceCount, successStreak,
                lastEvidenceAt, policy.nextReviewAt(newTier, lastEvidenceAt, successStreak), medianDurationMs,
                topErrorCodes, confidence);
    }
}
