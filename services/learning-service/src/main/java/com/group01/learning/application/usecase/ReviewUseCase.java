package com.group01.learning.application.usecase;

import com.group01.learning.application.AnswerSheet;
import com.group01.learning.application.LessonEvidenceReference;
import com.group01.learning.application.command.SubmitReviewCommand;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.Item;
import com.group01.learning.application.port.LearningContentClient.PackageVersion;
import com.group01.learning.application.port.LearningContentClient.PracticeSet;
import com.group01.learning.application.port.LearningContentClient.Section;
import com.group01.learning.application.port.LearningProgressStore;
import com.group01.learning.application.port.LearningProgressStore.NewEvidence;
import com.group01.learning.application.port.ReviewStore;
import com.group01.learning.application.port.ReviewStore.ReviewItem;
import com.group01.learning.application.result.LessonResult;
import com.group01.learning.application.result.ReviewResult;
import com.group01.learning.application.result.ReviewSubmissionResult;
import com.group01.learning.application.result.SubmissionResult;
import com.group01.learning.domain.service.AnswerSpecGrader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A pending review gives the theory of the lesson that teaches the weak knowledge point and one practice set of new
 * questions. A failed set is followed by another package; after the third failed set the review is skipped so the
 * learner is never stuck, and later tests can insert a new review.
 */
@Service
public class ReviewUseCase {
    static final int MAX_FAILED_REVIEW_SETS = 3;
    private static final int MIN_SET_QUESTIONS = 3;
    private static final int PACKAGE_CANDIDATES = 10;

    private final LearningContentClient content;
    private final LearningProgressStore progress;
    private final ReviewStore reviews;
    private final AnswerSpecGrader grader = new AnswerSpecGrader();

    public ReviewUseCase(LearningContentClient content, LearningProgressStore progress, ReviewStore reviews) {
        this.content = content;
        this.progress = progress;
        this.reviews = reviews;
    }

    @Transactional
    public ReviewResult get(UUID userId, UUID reviewId) {
        progress.lockUser(userId);
        ReviewItem review = ownedReview(userId, reviewId);
        List<String> theory = content.getLesson(review.lessonId()).blocks().stream()
                .filter(block -> "TEXT".equals(block.blockType()) && block.textContent() != null)
                .sorted(Comparator.comparingInt(LearningContentClient.Block::sortOrder))
                .map(LearningContentClient.Block::textContent).toList();
        if (!"PENDING".equals(review.status())) {
            return new ReviewResult(reviewId, review.status(), review.lessonId(), theory, null);
        }
        Optional<ReviewStore.ReviewSet> open = reviews.findOpenSet(reviewId);
        if (open.isEmpty()) {
            Optional<PracticeSet> next = nextPackage(userId, review.knowledgePointId());
            if (next.isEmpty()) {
                // Content removed every package for this KP: let the learner continue.
                reviews.finishReview(reviewId, "SKIPPED");
                return new ReviewResult(reviewId, "SKIPPED", review.lessonId(), theory, null);
            }
            UUID setId = reviews.insertSet(userId, reviewId, next.get().packageId(), next.get().packageVersionId());
            open = Optional.of(new ReviewStore.ReviewSet(setId, reviewId, next.get().packageId(),
                    next.get().packageVersionId()));
        }
        ReviewStore.ReviewSet set = open.get();
        PackageVersion version = content.getPackageVersion(set.packageVersionId());
        Section first = orderedSections(version).stream().findFirst().orElse(null);
        List<LessonResult.Question> questions = orderedItems(version).stream()
                .map(item -> new LessonResult.Question(item.questionVersionId(), item.sortOrder(), item.stem(),
                        item.options() == null ? null : item.options().stream().map(option -> new LessonResult.Option(
                                option.optionKey(), option.content(), option.sortOrder())).toList()))
                .toList();
        ReviewResult.Audio audio = first == null || first.audio() == null ? null
                : new ReviewResult.Audio(first.audio().mediaUrl(), first.audio().durationSeconds());
        return new ReviewResult(reviewId, "PENDING", review.lessonId(), theory, new ReviewResult.ReviewSet(
                set.reviewSetId(), set.packageId(), set.packageVersionId(), first == null ? null : first.passage(),
                audio, questions));
    }

    @Transactional
    public ReviewSubmissionResult submit(UUID userId, UUID reviewId, SubmitReviewCommand command) {
        progress.lockUser(userId);
        var saved = reviews.findSubmission(command.requestId());
        if (saved.isPresent()) {
            var submission = saved.get();
            if (!submission.userId().equals(userId) || !submission.reviewId().equals(reviewId)
                    || !submission.reviewSetId().equals(command.reviewSetId())) {
                throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
            }
            return submission.response();
        }
        ownedReview(userId, reviewId);
        ReviewStore.ReviewSet set = reviews.lockOpenSet(userId, reviewId, command.reviewSetId())
                .orElseThrow(() -> new LearningRequestException(409, "REVIEW_SET_CLOSED", "Review set is closed"));
        PackageVersion version = content.getPackageVersion(set.packageVersionId());
        List<Item> items = orderedItems(version);
        Map<UUID, Object> answers = AnswerSheet.require(
                items.stream().map(Item::questionVersionId).toList(), command.answers());
        List<AnswerSpecGrader.Grade> grades = items.stream()
                .map(item -> grader.grade(item.answerSpec(), answers.get(item.questionVersionId()))).toList();
        if (grades.stream().anyMatch(grade -> !grade.gradable())) {
            throw new LearningRequestException(422, "UNGRADABLE_EXERCISE", "Review set contains unsupported questions");
        }
        boolean passed = AnswerSheet.passes(grades.stream().filter(AnswerSpecGrader.Grade::correct).count(),
                items.size());

        // A set is answered once, so every set contributes its first answers as evidence.
        List<NewEvidence> evidence = new ArrayList<>();
        List<SubmissionResult.AnswerResult> results = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            Item item = items.get(index);
            AnswerSpecGrader.Grade grade = grades.get(index);
            for (UUID kpId : new LinkedHashSet<>(item.knowledgePointMappings().stream()
                    .map(LearningContentClient.KnowledgePointMapping::knowledgePointId).toList())) {
                evidence.add(new NewEvidence(kpId, grade.correct(), "review_set",
                        LessonEvidenceReference.forReviewSet(command.requestId(), item.questionVersionId(), kpId)));
            }
            results.add(new SubmissionResult.AnswerResult(item.questionVersionId(), grade.correct(),
                    passed ? grade.correctAnswer() : null, passed ? item.explanation() : null));
        }
        progress.appendEvidence(userId, evidence);

        String status = "PENDING";
        if (passed) {
            status = "DONE";
        } else if (reviews.failedSetCount(reviewId) + 1 >= MAX_FAILED_REVIEW_SETS) {
            status = "SKIPPED";
        }
        Section first = orderedSections(version).stream().findFirst().orElse(null);
        String transcript = passed && first != null && first.audio() != null ? first.audio().transcript() : null;
        ReviewSubmissionResult response = new ReviewSubmissionResult(status, passed, List.copyOf(results), transcript);
        reviews.closeSet(set.reviewSetId(), command.requestId(), passed, response);
        if (!"PENDING".equals(status)) reviews.finishReview(reviewId, status);
        return response;
    }

    private ReviewItem ownedReview(UUID userId, UUID reviewId) {
        return reviews.findReview(userId, reviewId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));
    }

    /** A package the learner has never been given, otherwise the one given longest ago. */
    private Optional<PracticeSet> nextPackage(UUID userId, UUID knowledgePointId) {
        List<PracticeSet> unused = content.searchPracticeSets(knowledgePointId, reviews.assignedPackageIds(userId),
                MIN_SET_QUESTIONS, 1);
        if (!unused.isEmpty()) return Optional.of(unused.getFirst());
        List<PracticeSet> all = content.searchPracticeSets(knowledgePointId, List.of(), MIN_SET_QUESTIONS,
                PACKAGE_CANDIDATES);
        Map<UUID, Instant> lastAssigned = reviews.lastAssignedAt(userId, all.stream().map(PracticeSet::packageId).toList());
        return all.stream().min(Comparator.comparing((PracticeSet set) -> lastAssigned.getOrDefault(set.packageId(),
                Instant.MIN)).thenComparing(set -> set.packageId().toString()));
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
