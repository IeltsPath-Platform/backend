package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.result.WritingSubmissionResult;
import com.ieltspath.learning.application.service.EssayPrompts;
import com.ieltspath.learning.application.service.EssaySubmissionFlow;
import com.ieltspath.learning.application.service.FreeWritingPrompts;
import com.ieltspath.learning.application.service.WritingSubmissionViewAssembler;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.WritingSubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Sends a free-writing essay to the examiner the learner chose, in place of the AI grade. The essay is stored first as
 * AWAITING_EXAMINER, then sent for review: Access spends one Premium grading credit (402 when the learner has no Premium
 * or no credit left) and Assessment queues it for that examiner (409 when the examiner cannot take it). Nothing here
 * grades or charges points. The credit is idempotent by essay, and Mentor requests are idempotent by essay too, so a
 * resend with the same {@code requestId} continues the same essay: after a refusal the learner sends it again, possibly
 * to another examiner, without storing a second essay. A different {@code requestId} is a new essay: it is refused with
 * 409 {@code EXAMINER_REVIEW_PENDING} while the previous essay for the same prompt still waits for its examiner.
 */
@Service
public class SubmitFreeWritingToExaminerUseCase {
    private final FreeWritingPrompts prompts;
    private final LearnerLock lock;
    private final WritingSubmissionRepository essays;
    private final EssaySubmissionFlow flow;
    private final HumanReviewUseCase reviews;
    private final WritingSubmissionViewAssembler view;
    private final TransactionTemplate transaction;
    private final Clock clock = Clock.systemUTC();

    public SubmitFreeWritingToExaminerUseCase(FreeWritingPrompts prompts, LearnerLock lock,
                                              WritingSubmissionRepository essays, EssaySubmissionFlow flow,
                                              HumanReviewUseCase reviews, WritingSubmissionViewAssembler view,
                                              PlatformTransactionManager transactionManager) {
        this.prompts = prompts;
        this.lock = lock;
        this.essays = essays;
        this.flow = flow;
        this.reviews = reviews;
        this.view = view;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public WritingSubmissionResult execute(UUID userId, UUID packageVersionId, UUID questionVersionId, UUID requestId,
                                           String essayText, UUID examinerId) {
        int words = flow.validate(essayText);
        Optional<WritingSubmission> existing = essays.findByRequestId(requestId);
        WritingSubmission submission;
        if (existing.isPresent()) {
            submission = existing.get();
            if (!sameEssay(submission, userId, questionVersionId)) throw EssaySubmissionFlow.requestConflict();
        } else {
            // A new essay for a prompt whose last essay still waits for its examiner would spend another credit and take
            // another place of the examiner's day; the learner can send again once the examiner has graded.
            pending(userId, questionVersionId).ifPresent(waiting -> { throw reviewPending(waiting); });
            Item item = prompts.find(packageVersionId, questionVersionId);
            submission = transaction.execute(status -> store(userId, item, requestId, essayText, words));
            if (!sameEssay(submission, userId, questionVersionId)) throw EssaySubmissionFlow.requestConflict();
        }
        // No transaction is open: the credit is spent in Access and the review is made in Assessment.
        reviews.request(userId, submission.id(), examinerId);
        return view.assemble(submission);
    }

    /**
     * The id of the learner's essay for this prompt that was sent to an examiner and has not been graded yet. Only the newest
     * essay can be waiting: a new one is not accepted until the last is graded or cancelled. Assessment knows which, so
     * that is asked here; no transaction is open.
     */
    public Optional<UUID> pending(UUID userId, UUID questionVersionId) {
        return essays.findLatestSentToExaminer(userId, questionVersionId)
                .filter(sent -> !Set.of("DONE", "CANCELLED").contains(reviews.review(userId, sent.id()).status()))
                .map(WritingSubmission::id);
    }

    private static LearningRequestException reviewPending(UUID submissionId) {
        return new LearningRequestException(409, "EXAMINER_REVIEW_PENDING",
                "This essay was already sent to an examiner who has not graded it yet", submissionId);
    }

    private static boolean sameEssay(WritingSubmission submission, UUID userId, UUID questionVersionId) {
        return submission.userId().equals(userId) && submission.isFree()
                && submission.status() == WritingSubmissionStatus.AWAITING_EXAMINER
                && questionVersionId.equals(submission.prompt().questionVersionId());
    }

    /** Stores the essay unless a concurrent request with the same id stored it first; that one is returned then. */
    private WritingSubmission store(UUID userId, Item item, UUID requestId, String essayText, int words) {
        lock.lock(userId);
        Optional<WritingSubmission> existing = essays.findByRequestId(requestId);
        if (existing.isPresent()) return existing.get();
        WritingSubmission submission = WritingSubmission.startFreeForExaminer(UUID.randomUUID(), userId, requestId,
                essayText, words, EssayPrompts.of(item), clock.instant());
        if (!essays.save(submission)) {
            throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
        }
        return submission;
    }
}
