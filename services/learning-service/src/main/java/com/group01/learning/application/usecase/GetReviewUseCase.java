package com.group01.learning.application.usecase;

import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.PackageVersion;
import com.group01.learning.application.port.LearningContentClient.PracticeSet;
import com.group01.learning.application.port.LearningContentClient.Section;
import com.group01.learning.application.result.LessonResult;
import com.group01.learning.application.result.ReviewResult;
import com.group01.learning.domain.aggregate.ReviewItem;
import com.group01.learning.domain.entity.ReviewSet;
import com.group01.learning.domain.repository.ReviewItemRepository;
import com.group01.learning.domain.repository.PracticeAttemptRepository;
import com.group01.learning.application.service.PracticeProgress;
import com.group01.learning.application.service.ItemGrading;
import com.group01.learning.domain.vo.ReviewStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A pending review gives the theory of the lesson that teaches the weak knowledge point and one practice set of new
 * questions. The review's own rules ({@link ReviewItem}) decide DONE, another set, or SKIPPED.
 */
@Service
public class GetReviewUseCase {
    private static final int MIN_SET_QUESTIONS = 3;

    private final LearningContentClient content;
    private final LearnerLock lock;
    private final ReviewItemRepository reviews;
    private final PracticeAttemptRepository attempts;
    private final PracticeProgress practice;

    public GetReviewUseCase(LearningContentClient content, LearnerLock lock, ReviewItemRepository reviews,
                            PracticeAttemptRepository attempts, PracticeProgress practice) {
        this.content = content;
        this.lock = lock;
        this.reviews = reviews;
        this.attempts = attempts;
        this.practice = practice;
    }

    @Transactional
    public ReviewResult execute(UUID userId, UUID reviewId) {
        lock.lock(userId);
        ReviewItem review = ownedReview(userId, reviewId);
        List<String> theory = content.getLesson(review.lessonId()).blocks().stream()
                .filter(block -> "TEXT".equals(block.blockType()) && block.textContent() != null)
                .sorted(Comparator.comparingInt(LearningContentClient.Block::sortOrder))
                .map(LearningContentClient.Block::textContent).toList();
        if (review.status() != ReviewStatus.PENDING) {
            return new ReviewResult(reviewId, review.status().name(), review.lessonId(), theory, null);
        }
        Set<UUID> revealed = attempts.revealedPackageIds(userId);
        if (review.openSet().isPresent() && revealed.contains(review.openSet().orElseThrow().packageId())) {
            ReviewSet stale = review.discardOpenSet();
            reviews.deleteOpenSet(userId, reviewId, stale.id());
        }
        if (review.openSet().isEmpty()) {
            Optional<PracticeSet> next = nextPackage(userId, review.knowledgePointId(), review.lessonId(), revealed);
            if (next.isEmpty()) {
                // No unrevealed eligible package remains, so the learner can continue.
                review.skip();
                reviews.save(review);
                practice.refreshPassForLesson(userId, review.lessonId());
                return new ReviewResult(reviewId, review.status().name(), review.lessonId(), theory, null);
            }
            review.assignSet(UUID.randomUUID(), next.get().packageId(), next.get().packageVersionId());
            reviews.save(review);
        }
        ReviewSet set = review.openSet().orElseThrow();
        PackageVersion version = content.getPackageVersion(set.packageVersionId());
        Section first = ItemGrading.orderedSections(version).stream().findFirst().orElse(null);
        List<LessonResult.Question> questions = ItemGrading.orderedItems(version).stream()
                .map(item -> new LessonResult.Question(item.questionVersionId(), item.sortOrder(), item.stem(),
                        item.options() == null ? null : item.options().stream().map(option -> new LessonResult.Option(
                                option.optionKey(), option.content(), option.sortOrder())).toList()))
                .toList();
        ReviewResult.Audio audio = first == null || first.audio() == null ? null
                : new ReviewResult.Audio(first.audio().mediaUrl(), first.audio().durationSeconds());
        return new ReviewResult(reviewId, ReviewStatus.PENDING.name(), review.lessonId(), theory,
                new ReviewResult.ReviewSet(set.id(), set.packageId(), set.packageVersionId(),
                        first == null ? null : first.passage(), audio, questions));
    }

    private ReviewItem ownedReview(UUID userId, UUID reviewId) {
        return reviews.findOwned(userId, reviewId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));
    }

    /** Only a package whose answers the learner has not seen can be assigned. */
    private Optional<PracticeSet> nextPackage(UUID userId, UUID knowledgePointId, UUID lessonId, Set<UUID> revealed) {
        Set<UUID> excluded = new HashSet<>(revealed);
        excluded.addAll(reviews.assignedPackageIds(userId));
        return content.searchPracticeSets(knowledgePointId, excluded.stream().toList(), MIN_SET_QUESTIONS, 1, lessonId)
                .stream().findFirst();
    }
}
