package com.group01.learning.application.usecase;

import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.Item;
import com.group01.learning.application.port.LearningContentClient.PackageVersion;
import com.group01.learning.application.port.LearningContentClient.PracticeSet;
import com.group01.learning.application.port.LearningContentClient.Section;
import com.group01.learning.application.result.LessonResult;
import com.group01.learning.application.result.ReviewResult;
import com.group01.learning.domain.aggregate.ReviewItem;
import com.group01.learning.domain.entity.ReviewSet;
import com.group01.learning.domain.repository.ReviewItemRepository;
import com.group01.learning.domain.service.PackageRotation;
import com.group01.learning.domain.vo.ReviewStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A pending review gives the theory of the lesson that teaches the weak knowledge point and one practice set of new
 * questions. The review's own rules ({@link ReviewItem}) decide DONE, another set, or SKIPPED.
 */
@Service
public class GetReviewUseCase {
    private static final int MIN_SET_QUESTIONS = 3;
    private static final int PACKAGE_CANDIDATES = 10;

    private final LearningContentClient content;
    private final LearnerLock lock;
    private final ReviewItemRepository reviews;

    public GetReviewUseCase(LearningContentClient content, LearnerLock lock, ReviewItemRepository reviews) {
        this.content = content;
        this.lock = lock;
        this.reviews = reviews;
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
        if (review.openSet().isEmpty()) {
            Optional<PracticeSet> next = nextPackage(userId, review.knowledgePointId());
            if (next.isEmpty()) {
                // Content removed every package for this KP: let the learner continue.
                review.skip();
                reviews.save(review);
                return new ReviewResult(reviewId, review.status().name(), review.lessonId(), theory, null);
            }
            review.assignSet(UUID.randomUUID(), next.get().packageId(), next.get().packageVersionId());
            reviews.save(review);
        }
        ReviewSet set = review.openSet().orElseThrow();
        PackageVersion version = content.getPackageVersion(set.packageVersionId());
        Section first = orderedSections(version).stream().findFirst().orElse(null);
        List<LessonResult.Question> questions = orderedItems(version).stream()
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

    /** A package the learner has never been given, otherwise the one given longest ago. */
    private Optional<PracticeSet> nextPackage(UUID userId, UUID knowledgePointId) {
        List<PracticeSet> unused = content.searchPracticeSets(knowledgePointId, reviews.assignedPackageIds(userId),
                MIN_SET_QUESTIONS, 1);
        if (!unused.isEmpty()) return Optional.of(unused.getFirst());
        List<PracticeSet> all = content.searchPracticeSets(knowledgePointId, List.of(), MIN_SET_QUESTIONS,
                PACKAGE_CANDIDATES);
        return PackageRotation.leastRecentlyUsed(
                        all.stream().map(set -> new PackageRotation.Candidate(set.packageId(), null)).toList(),
                        reviews.lastAssignedAt(userId, all.stream().map(PracticeSet::packageId).toList()))
                .flatMap(id -> all.stream().filter(set -> set.packageId().equals(id)).findFirst());
    }

    private static List<Section> orderedSections(PackageVersion version) {
        return version.sections().stream().sorted(Comparator.comparingInt(Section::sortOrder)
                .thenComparing(section -> section.sectionId().toString())).toList();
    }

    private static List<Item> orderedItems(PackageVersion version) {
        return orderedSections(version).stream().flatMap(section -> section.items().stream()
                .sorted(Comparator.comparingInt(Item::sortOrder)
                        .thenComparing(item -> item.questionVersionId().toString()))).toList();
    }
}
