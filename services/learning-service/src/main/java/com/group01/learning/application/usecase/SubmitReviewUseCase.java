package com.group01.learning.application.usecase;

import com.group01.learning.application.command.SubmitReviewCommand;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.Item;
import com.group01.learning.application.port.LearningContentClient.PackageVersion;
import com.group01.learning.application.port.LearningContentClient.Section;
import com.group01.learning.application.port.ReviewSubmissionLog;
import com.group01.learning.application.result.ReviewSubmissionResult;
import com.group01.learning.application.result.SubmissionResult;
import com.group01.learning.application.service.AnswerSheet;
import com.group01.learning.application.service.LessonEvidenceReference;
import com.group01.learning.application.service.ItemGrading;
import com.group01.learning.application.service.PracticeProgress;
import com.group01.learning.domain.aggregate.ReviewItem;
import com.group01.learning.domain.entity.ReviewSet;
import com.group01.learning.domain.repository.KnowledgeEvidenceRepository;
import com.group01.learning.domain.repository.ReviewItemRepository;
import com.group01.learning.domain.repository.PracticeAttemptRepository;
import com.group01.learning.domain.service.AnswerSpecGrader;
import com.group01.learning.domain.service.PassMark;
import com.group01.learning.domain.vo.EvidenceSource;
import com.group01.learning.domain.vo.KnowledgeEvidence;
import com.group01.learning.domain.vo.ReviewStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A pending review gives the theory of the lesson that teaches the weak knowledge point and one practice set of new
 * questions. The review's own rules ({@link ReviewItem}) decide DONE, another set, or SKIPPED.
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
                    passed ? grade.correctAnswer() : null, passed ? item.explanation() : null));
        }
        evidence.append(userId, setAnswers);

        ReviewStatus status = review.recordSetResult(set.id(), command.requestId(), passed);
        Section first = ItemGrading.orderedSections(version).stream().findFirst().orElse(null);
        String transcript = passed && first != null && first.audio() != null ? first.audio().transcript() : null;
        ReviewSubmissionResult response = new ReviewSubmissionResult(status.name(), passed, List.copyOf(results),
                transcript);
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
