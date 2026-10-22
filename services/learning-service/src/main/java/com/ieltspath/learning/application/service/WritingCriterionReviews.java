package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.ReviewRule.ReviewCandidate;
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

/** Called under the learner lock, in the same transaction that records the essay evidence. */
@Service
public class WritingCriterionReviews {
    private static final int MIN_SET_QUESTIONS = 3;
    private final ReviewItemRepository reviews;
    private final PracticeAttemptRepository attempts;
    private final LearningContentClient content;
    private final int limit;
    private final WritingCriterionReviewRule rule = new WritingCriterionReviewRule();

    public WritingCriterionReviews(ReviewItemRepository reviews, PracticeAttemptRepository attempts,
                                   LearningContentClient content,
                                   @Value("${learning.writing.criterion-review-limit:2}") int limit) {
        if (limit < 0) throw new IllegalArgumentException("criterion-review-limit must not be negative");
        this.reviews = reviews;
        this.attempts = attempts;
        this.content = content;
        this.limit = limit;
    }

    public void create(UUID userId, UUID lessonId, Collection<FailedWritingCriterion> failedCriteria) {
        if (lessonId == null || limit == 0 || failedCriteria.isEmpty()) return;
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
        List<ReviewCandidate> selected = rule.select(failedCriteria, pending,
                        available == null ? Map.of() : available, limit).stream()
                .map(criterion -> new ReviewCandidate(criterion.knowledgePointId(), lessonId, LearningSkill.WRITING))
                .toList();
        reviews.insertPending(userId, selected, false, "WRITING_CRITERION");
    }
}
