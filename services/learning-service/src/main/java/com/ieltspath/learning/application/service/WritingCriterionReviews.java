package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.WritingCriterionReviewRule;
import com.ieltspath.learning.domain.vo.FailedWritingCriterion;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Optional reviews for the criteria of an essay or a recording that fell below the pass band. Called under the learner
 * lock, in the same transaction that records the evidence.
 */
@Service
public class WritingCriterionReviews {
    private static final int MIN_SET_QUESTIONS = 3;
    private static final String WRITING_TRIGGER = "WRITING_CRITERION";
    private static final String SPEAKING_TRIGGER = "SPEAKING_CRITERION";
    private final ReviewItemRepository reviews;
    private final PracticeAttemptRepository attempts;
    private final LearningContentClient content;
    private final int limit;
    private final int speakingLimit;
    private final WritingCriterionReviewRule rule = new WritingCriterionReviewRule();

    public WritingCriterionReviews(ReviewItemRepository reviews, PracticeAttemptRepository attempts,
                                   LearningContentClient content,
                                   @Value("${learning.writing.criterion-review-limit:2}") int limit,
                                   @Value("${learning.speaking.criterion-review-limit:2}") int speakingLimit) {
        if (limit < 0) throw new IllegalArgumentException("criterion-review-limit must not be negative");
        if (speakingLimit < 0) throw new IllegalArgumentException("speaking criterion-review-limit must not be negative");
        this.reviews = reviews;
        this.attempts = attempts;
        this.content = content;
        this.limit = limit;
        this.speakingLimit = speakingLimit;
    }

    public void create(UUID userId, UUID lessonId, Collection<FailedWritingCriterion> failedCriteria) {
        create(userId, lessonId, failedCriteria, limit);
    }

    /** As {@link #create(UUID, UUID, Collection)} with at most {@code maxReviews} new reviews, for callers that share a cap. */
    public void create(UUID userId, UUID lessonId, Collection<FailedWritingCriterion> failedCriteria, int maxReviews) {
        create(userId, lessonId, failedCriteria, maxReviews, LearningSkill.WRITING, WRITING_TRIGGER);
    }

    /** The criteria of a recorded answer, reviewed as Speaking points under their own trigger. */
    public void createForSpeaking(UUID userId, UUID lessonId, Collection<FailedWritingCriterion> failedCriteria) {
        create(userId, lessonId, failedCriteria, speakingLimit, LearningSkill.SPEAKING, SPEAKING_TRIGGER);
    }

    private void create(UUID userId, UUID lessonId, Collection<FailedWritingCriterion> failedCriteria, int maxReviews,
                        LearningSkill skill, String triggerKind) {
        if (lessonId == null || maxReviews <= 0 || failedCriteria.isEmpty()) return;
        Set<UUID> pending = reviews.findPending(userId).stream().map(review -> review.knowledgePointId())
                .collect(Collectors.toSet());
        List<UUID> pointIds = failedCriteria.stream().map(FailedWritingCriterion::knowledgePointId)
                .filter(pointId -> !pending.contains(pointId)).distinct()
                .sorted(Comparator.comparing(UUID::toString)).toList();
        if (pointIds.isEmpty()) return;
        Set<UUID> excluded = new HashSet<>(attempts.revealedPackageIds(userId));
        excluded.addAll(reviews.assignedPackageIds(userId));
        Map<UUID, Integer> available = content.practiceSetAvailability(pointIds,
                excluded.stream().sorted(Comparator.comparing(UUID::toString)).toList(), MIN_SET_QUESTIONS);
        List<FailedWritingCriterion> selected = rule.select(failedCriteria, pending,
                available == null ? Map.of() : available, maxReviews);
        reviews.insertCriterionPending(userId, lessonId, skill, triggerKind, selected);
    }
}
