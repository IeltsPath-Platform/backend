package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.PracticeSet;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.application.result.LessonResult;
import com.ieltspath.learning.application.result.ReviewResult;
import com.ieltspath.learning.application.service.FirstAttemptMistakes;
import com.ieltspath.learning.application.service.ItemGrading;
import com.ieltspath.learning.application.service.PracticeProgress;
import com.ieltspath.learning.application.service.TheoryFocus;
import com.ieltspath.learning.domain.aggregate.ReviewItem;
import com.ieltspath.learning.domain.entity.ReviewSet;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.ReviewRule;
import com.ieltspath.learning.domain.vo.ReviewStage;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import com.ieltspath.learning.domain.vo.TheoryReason;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A pending review shows the theory of its knowledge point and, at the practice stage, one set of a package whose
 * answers the learner has never seen; at the theory stage it shows a quick check instead of a set. The review's own
 * rules ({@link ReviewItem}) decide DONE, the theory step, or SKIPPED.
 */
@Service
public class GetReviewUseCase {
    private static final int MIN_SET_QUESTIONS = 3;

    private final LearningContentClient content;
    private final LearnerLock lock;
    private final ReviewItemRepository reviews;
    private final PracticeAttemptRepository attempts;
    private final PracticeProgress practice;
    private final FirstAttemptMistakes mistakes;
    private final TheoryFocus theoryFocus = new TheoryFocus();

    public GetReviewUseCase(LearningContentClient content, LearnerLock lock, ReviewItemRepository reviews,
                            PracticeAttemptRepository attempts, PracticeProgress practice,
                            FirstAttemptMistakes mistakes) {
        this.content = content;
        this.lock = lock;
        this.reviews = reviews;
        this.attempts = attempts;
        this.practice = practice;
        this.mistakes = mistakes;
    }

    @Transactional
    public ReviewResult execute(UUID userId, UUID reviewId) {
        lock.lock(userId);
        ReviewItem review = ownedReview(userId, reviewId);
        var lesson = content.getLesson(review.lessonId());
        var focus = theoryFocus.of(lesson, review.knowledgePointId());
        if (review.status() != ReviewStatus.PENDING) return result(review, focus, null, List.of());
        // Practice reviews choose their stage when created; reviews from assessment results are created by the
        // event consumer, which cannot read lessons, so the lesson check happens on first open.
        if (review.isFresh() && !"PRACTICE".equals(review.triggerKind())
                && mistakes.of(userId, lesson).contains(review.knowledgePointId())) {
            review.startWithTheory(TheoryReason.WRONG_IN_LESSON);
            reviews.save(review);
        }
        if (review.stage() == ReviewStage.THEORY) {
            return result(review, focus, null, focus.quickCheck().stream()
                    .map(question -> new LessonResult.Question(question.questionVersionId(), question.sortOrder(),
                            question.stem(), options(question.options()), question.hint())).toList());
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
                return result(review, focus, null, List.of());
            }
            review.assignSet(UUID.randomUUID(), next.get().packageId(), next.get().packageVersionId());
            reviews.save(review);
        }
        ReviewSet set = review.openSet().orElseThrow();
        PackageVersion version = content.getPackageVersion(set.packageVersionId());
        Section first = ItemGrading.orderedSections(version).stream().findFirst().orElse(null);
        List<LessonResult.Question> questions = ItemGrading.orderedItems(version).stream()
                .map(item -> new LessonResult.Question(item.questionVersionId(), item.sortOrder(), item.stem(),
                        options(item.options()), item.hint()))
                .toList();
        ReviewResult.Audio audio = first == null || first.audio() == null ? null
                : new ReviewResult.Audio(first.audio().mediaUrl(), first.audio().durationSeconds());
        return result(review, focus, new ReviewResult.ReviewSet(set.id(), set.packageId(), set.packageVersionId(),
                first == null ? null : first.passage(), audio, questions), List.of());
    }

    private static ReviewResult result(ReviewItem review, TheoryFocus.Focus focus, ReviewResult.ReviewSet set,
                                       List<LessonResult.Question> quickCheck) {
        return new ReviewResult(review.id(), review.status().name(), review.lessonId(), focus.theory(), set,
                review.knowledgePointId(), review.skill(), review.stage().name(),
                review.theoryReason() == null ? null : review.theoryReason().name(), focus.scope(),
                review.failedSets(), ReviewRule.MAX_FAILED_REVIEW_SETS, quickCheck);
    }

    private static List<LessonResult.Option> options(List<LearningContentClient.Option> options) {
        return options == null ? null : options.stream()
                .map(option -> new LessonResult.Option(option.optionKey(), option.content(), option.sortOrder()))
                .toList();
    }

    private ReviewItem ownedReview(UUID userId, UUID reviewId) {
        return reviews.findOwned(userId, reviewId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));
    }

    /** Only a package whose answers the learner has not seen and that no earlier set gave can be assigned. */
    private Optional<PracticeSet> nextPackage(UUID userId, UUID knowledgePointId, UUID lessonId, Set<UUID> revealed) {
        Set<UUID> excluded = new HashSet<>(revealed);
        excluded.addAll(reviews.assignedPackageIds(userId));
        return content.searchPracticeSets(knowledgePointId, excluded.stream().toList(), MIN_SET_QUESTIONS, 1, lessonId)
                .stream().findFirst();
    }
}
