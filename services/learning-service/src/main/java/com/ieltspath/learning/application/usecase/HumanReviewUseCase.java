package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.HumanReviewClient;
import com.ieltspath.learning.application.port.HumanReviewClient.CreateReview;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.HumanReviewRequestRepository;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.WritingSubmissionStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * A learner with a human-grading credit asks a Mentor to review a graded lesson or Practice essay. The review is feedback
 * only: it writes no evidence, makes no review and changes no gate. The credit is spent once per essay (Access is
 * idempotent by essay id); if the review cannot be created the credit is given back. No call holds a database
 * transaction, and repeating the request returns the review already asked for.
 */
@Service
public class HumanReviewUseCase {
    private final WritingSubmissionRepository essays;
    private final HumanReviewRequestRepository requests;
    private final HumanReviewClient client;
    private final LearningContentClient content;

    public HumanReviewUseCase(WritingSubmissionRepository essays, HumanReviewRequestRepository requests,
                              HumanReviewClient client, LearningContentClient content) {
        this.essays = essays;
        this.requests = requests;
        this.client = client;
        this.content = content;
    }

    /** Asks for the review; returns the id of the review in Assessment. */
    public UUID request(UUID userId, UUID submissionId) {
        WritingSubmission essay = owned(userId, submissionId);
        var existing = requests.findReviewId(submissionId);
        if (existing.isPresent()) return existing.get();
        if (essay.status() != WritingSubmissionStatus.GRADED) {
            throw new LearningRequestException(409, "NOT_REVIEWABLE", "Only a graded essay can be sent to a Mentor");
        }
        var rubric = content.getActiveRubric(essay.prompt().task());

        client.consumeCredit(submissionId);
        UUID reviewId;
        try {
            reviewId = client.createReview(new CreateReview(requestId(submissionId), userId, submissionId,
                    essay.blockId() != null ? "LESSON" : "PRACTICE", essay.prompt().task(), essay.prompt().stem(),
                    essay.essayText(), rubric.code(), rubric.version()));
        } catch (RuntimeException failure) {
            refund(submissionId);
            throw failure;
        }
        if (!requests.insert(submissionId, userId, reviewId)) {
            // A concurrent request stored first; both made the same review, so return the stored one.
            return requests.findReviewId(submissionId).orElse(reviewId);
        }
        return reviewId;
    }

    /** The Mentor's review of the essay, or PENDING while no Examiner has finished it. */
    public HumanReviewClient.Review review(UUID userId, UUID submissionId) {
        owned(userId, submissionId);
        UUID reviewId = requests.findReviewId(submissionId).orElseThrow(() -> new LearningRequestException(404,
                "NO_HUMAN_REVIEW", "No Mentor review was asked for this essay"));
        return client.fetchReview(reviewId, userId);
    }

    private WritingSubmission owned(UUID userId, UUID submissionId) {
        return essays.findOwned(submissionId, userId).orElseThrow(
                () -> new LearningRequestException(404, "NOT_FOUND", "Submission was not found"));
    }

    private void refund(UUID submissionId) {
        try {
            client.refundCredit(submissionId);
        } catch (RuntimeException ignored) {
            // The credit stays with the essay: asking again reuses it, so nothing is spent twice.
        }
    }

    /** The same essay always makes the same request, so Assessment creates its review once. */
    private static UUID requestId(UUID submissionId) {
        return UUID.nameUUIDFromBytes(("human-review:" + submissionId).getBytes(StandardCharsets.UTF_8));
    }
}
