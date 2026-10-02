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
import com.group01.learning.domain.aggregate.ReviewItem;
import com.group01.learning.domain.entity.ReviewSet;
import com.group01.learning.domain.repository.KnowledgeEvidenceRepository;
import com.group01.learning.domain.repository.ReviewItemRepository;
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
    private final ReviewSubmissionLog submissions;
    private final AnswerSpecGrader grader = new AnswerSpecGrader();

    public SubmitReviewUseCase(LearningContentClient content, LearnerLock lock, KnowledgeEvidenceRepository evidence,
                               ReviewItemRepository reviews, ReviewSubmissionLog submissions) {
        this.content = content;
        this.lock = lock;
        this.evidence = evidence;
        this.reviews = reviews;
        this.submissions = submissions;
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
        PackageVersion version = content.getPackageVersion(set.packageVersionId());
        List<Item> items = orderedItems(version);
        Map<UUID, Object> answers = AnswerSheet.require(
                items.stream().map(Item::questionVersionId).toList(), command.answers());
        List<AnswerSpecGrader.Grade> grades = items.stream()
                .map(item -> grader.grade(item.answerSpec(), answers.get(item.questionVersionId()))).toList();
        if (grades.stream().anyMatch(grade -> !grade.gradable())) {
            throw new LearningRequestException(422, "UNGRADABLE_EXERCISE", "Review set contains unsupported questions");
        }
        boolean passed = PassMark.passes(grades.stream().filter(AnswerSpecGrader.Grade::correct).count(),
                items.size());

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
        Section first = orderedSections(version).stream().findFirst().orElse(null);
        String transcript = passed && first != null && first.audio() != null ? first.audio().transcript() : null;
        ReviewSubmissionResult response = new ReviewSubmissionResult(status.name(), passed, List.copyOf(results),
                transcript);
        reviews.save(review);
        submissions.save(set.id(), response);
        return response;
    }

    private ReviewItem ownedReview(UUID userId, UUID reviewId) {
        return reviews.findOwned(userId, reviewId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));
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
