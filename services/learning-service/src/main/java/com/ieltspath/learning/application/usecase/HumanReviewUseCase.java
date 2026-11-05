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
 * idempotent by essay id). It is given back only when this call spent it and Assessment refused the review outright; a
 * timeout or server error may have left the review behind, so the request is repeated (Assessment creates a review once
 * per request id) and, if it still fails, the credit stays with the essay and asking again reuses it. No call holds a
 * database transaction, and repeating the request returns the review already asked for.
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

    /**
     * Asks the chosen examiner for the review; returns the id of the review in Assessment. The learner picks the
     * examiner: nobody else receives the essay.
     */
    public UUID request(UUID userId, UUID submissionId, UUID examinerId) {
        if (examinerId == null) {
            throw new LearningRequestException(400, "EXAMINER_REQUIRED", "Choose the examiner who will grade the essay");
        }
        WritingSubmission essay = owned(userId, submissionId);
        var existing = requests.findReviewId(submissionId);
        if (existing.isPresent()) {
            requireNotCancelled(userId, existing.get());
            return existing.get();
        }
        if (essay.status() != WritingSubmissionStatus.GRADED && essay.status() != WritingSubmissionStatus.AWAITING_EXAMINER) {
            throw new LearningRequestException(409, "NOT_REVIEWABLE",
                    "Only a graded essay, or one sent to an examiner, can be sent to a Mentor");
        }
        var rubric = content.getActiveRubric(essay.prompt().task());

        boolean charged = client.consumeCredit(submissionId).charged();
        UUID reviewId;
        try {
            reviewId = createReview(new CreateReview(requestId(submissionId), userId, submissionId,
                    essay.blockId() != null ? "LESSON" : "PRACTICE", essay.prompt().task(), essay.prompt().stem(),
                    essay.prompt().images().stream().map(image -> new HumanReviewClient.PromptImage(image.mediaUrl(), image.altText())).toList(),
                    essay.essayText(), rubric.code(), rubric.version(), examinerId));
        } catch (LearningRequestException failure) {
            if (charged && refused(failure)) refund(submissionId);
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

    /**
     * The learner gives up a late review whose examiner can no longer be changed: Assessment closes the job, then the credit
     * goes back to the learner. Both steps are idempotent, so after a failure the learner asks again and the step that
     * already happened is not repeated.
     */
    public HumanReviewClient.Review cancel(UUID userId, UUID reviewSubmissionId) {
        UUID submissionId = requests.findSubmissionId(reviewSubmissionId, userId).orElseThrow(() -> new LearningRequestException(404,
                "NO_HUMAN_REVIEW", "No Mentor review was asked for this essay"));
        HumanReviewClient.Review cancelled = client.cancelReview(reviewSubmissionId);
        client.refundCredit(submissionId);
        return cancelled;
    }

    /** A cancelled request is not asked again: the credit went back, and the learner sends the essay as a new one. */
    private void requireNotCancelled(UUID userId, UUID reviewId) {
        HumanReviewClient.Review review;
        try {
            review = client.fetchReview(reviewId, userId);
        } catch (LearningRequestException unavailable) {
            return; // A repeated request must not fail only because Assessment cannot be reached right now.
        }
        if (review != null && "CANCELLED".equals(review.status())) {
            throw new LearningRequestException(409, "REVIEW_CANCELLED",
                    "This request was cancelled and its credit given back; send the essay again as a new one");
        }
    }

    /** Asks once more after a failure that may have left the review behind; the request id makes the repeat safe. */
    private UUID createReview(CreateReview request) {
        try {
            return client.createReview(request);
        } catch (LearningRequestException failure) {
            if (refused(failure) || failure.getStatus() == 401) throw failure;
            return client.createReview(request);
        }
    }

    /** Assessment answered that it did not create the review. */
    private static boolean refused(LearningRequestException failure) {
        return "HUMAN_GRADING_CONFLICT".equals(failure.getCode()) || "HUMAN_REVIEW_REJECTED".equals(failure.getCode())
                || "EXAMINER_UNAVAILABLE".equals(failure.getCode());
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
