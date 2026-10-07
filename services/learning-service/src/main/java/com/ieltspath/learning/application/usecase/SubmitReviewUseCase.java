package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.SubmitReviewCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.application.port.ReviewSubmissionLog;
import com.ieltspath.learning.application.result.ReviewSubmissionResult;
import com.ieltspath.learning.application.result.SubmissionResult;
import com.ieltspath.learning.application.service.AnswerSheet;
import com.ieltspath.learning.application.service.LessonEvidenceReference;
import com.ieltspath.learning.application.service.ItemGrading;
import com.ieltspath.learning.application.service.PracticeProgress;
import com.ieltspath.learning.domain.aggregate.ReviewItem;
import com.ieltspath.learning.domain.entity.ReviewSet;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.service.AnswerSpecGrader;
import com.ieltspath.learning.domain.service.PassMark;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.ReviewStage;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Grades the open set of a review at the practice stage. Every answered set shows its solutions and transcript; the
 * review's own rules ({@link ReviewItem}) decide DONE, the theory step, or SKIPPED.
 */
@Service
public class SubmitReviewUseCase {

    private final LearningContentClient content;
    private final LearnerLock lock;
    private final KnowledgeEvidenceRepository evidence;
    private final ReviewItemRepository reviews;
    private final PracticeAttemptRepository attempts;
    private final ReviewSubmissionLog submissions;
    private final PracticeProgress practice;
    private final ItemGrading grading = new ItemGrading();

    public SubmitReviewUseCase(LearningContentClient content, LearnerLock lock, KnowledgeEvidenceRepository evidence,
                               ReviewItemRepository reviews, PracticeAttemptRepository attempts,
                               ReviewSubmissionLog submissions,
                               PracticeProgress practice) {
        this.content = content;
        this.lock = lock;
        this.evidence = evidence;
        this.reviews = reviews;
        this.attempts = attempts;
        this.submissions = submissions;
        this.practice = practice;
    }

    @Transactional
    public ReviewSubmissionResult execute(UUID userId, UUID reviewId, SubmitReviewCommand command) {
        lock.lock(userId);
        var saved = submissions.find(command.requestId());
        if (saved.isPresent()) {
            var submission = saved.get();
            if (!submission.userId().equals(userId) || !submission.reviewId().equals(reviewId)
                    || !submission.reviewSetId().equals(command.reviewSetId())) {
                throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
            }
            return submission.response();
        }
        ReviewItem review = ownedReview(userId, reviewId);
        if (review.status() == ReviewStatus.PENDING && review.stage() == ReviewStage.THEORY) {
            throw new LearningRequestException(409, "THEORY_REQUIRED", "Read the theory before the next set");
        }
        if (!review.acceptsAnswersFor(command.reviewSetId())) {
            throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Review set is closed");
        }
        ReviewSet set = review.openSet().orElseThrow();
        if (attempts.revealedPackageIds(userId).contains(set.packageId())) {
            throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Review set is closed");
        }
        PackageVersion version = content.getPackageVersion(set.packageVersionId());
        var graded = grading.grade(version, command.answers(), "Review set");
        List<Item> items = graded.items();
        var grades = graded.grades();
        boolean passed = graded.passed();

        // A set is answered once, so every set contributes its first answers as evidence.
        List<KnowledgeEvidence> setAnswers = new ArrayList<>();
        List<SubmissionResult.AnswerResult> results = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            Item item = items.get(index);
            AnswerSpecGrader.Grade grade = grades.get(index);
            for (UUID kpId : new LinkedHashSet<>(item.knowledgePointMappings().stream()
                    .map(LearningContentClient.KnowledgePointMapping::knowledgePointId).toList())) {
                setAnswers.add(KnowledgeEvidence.of(kpId, grade.correct(), EvidenceSource.REVIEW_SET,
                        LessonEvidenceReference.forReviewSet(command.requestId(), item.questionVersionId(), kpId)));
            }
            results.add(new SubmissionResult.AnswerResult(item.questionVersionId(), grade.correct(),
                    grade.correctAnswer(), item.explanation()));
        }
        evidence.append(userId, setAnswers);

        ReviewStatus status = review.recordSetResult(set.id(), command.requestId(), graded.correct(), graded.total());
        Section first = ItemGrading.orderedSections(version).stream().findFirst().orElse(null);
        String transcript = first != null && first.audio() != null ? first.audio().transcript() : null;
        ReviewSubmissionResult response = new ReviewSubmissionResult(status.name(), passed, List.copyOf(results),
                transcript, review.stage().name(), review.failedSets());
        reviews.save(review);
        submissions.save(set.id(), response);
        if (status != ReviewStatus.PENDING) practice.refreshPassForLesson(userId, review.lessonId());
        return response;
    }

    private ReviewItem ownedReview(UUID userId, UUID reviewId) {
        return reviews.findOwned(userId, reviewId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));
    }

}
