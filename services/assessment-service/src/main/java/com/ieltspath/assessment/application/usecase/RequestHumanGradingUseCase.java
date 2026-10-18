package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.application.port.HumanGradingCreditPort;
import com.ieltspath.assessment.application.port.HumanGradingRequestStore;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.util.UUID;

/**
 * Paid human grading of a topic essay. The credit is spent in Access (a call that commits on its own) and the job is
 * queued here, so the two cannot be one transaction. No database transaction is held while Access is called; the job is
 * written in its own short transaction afterwards.
 *
 * <p>The credit is idempotent by submission and the job by its key, so asking again is safe at every point: a request
 * whose job already exists neither spends nor refunds anything, and one that lost the job (a failed write, or a refund
 * that did not go through) finds its credit already spent and only queues the job. A credit is refunded only when this
 * call spent it and the job is known not to exist.
 */
@Service
public class RequestHumanGradingUseCase {
    private static final Logger log = LoggerFactory.getLogger(RequestHumanGradingUseCase.class);

    private final HumanGradingRequestStore requests;
    private final GateEssayJobStore jobs;
    private final HumanGradingCreditPort credits;
    private final TransactionOperations transaction;

    public RequestHumanGradingUseCase(HumanGradingRequestStore requests, GateEssayJobStore jobs,
                                      HumanGradingCreditPort credits, TransactionOperations transaction) {
        this.requests = requests;
        this.jobs = jobs;
        this.credits = credits;
        this.transaction = transaction;
    }

    public RequestResult execute(UUID userId, UUID attemptId, UUID itemId) {
        UUID submissionId = requests.submittedTopicEssay(userId, attemptId, itemId)
                .orElseThrow(() -> new AssessmentNotFoundException("Submitted topic essay not found"));
        String key = "human-credit:" + submissionId;
        if (jobs.jobExists(key)) return new RequestResult(submissionId, "QUEUED");

        boolean charged = credits.consume(submissionId);
        try {
            transaction.executeWithoutResult(status -> jobs.enqueueHuman(submissionId, userId, key, "HUMAN_CREDIT"));
        } catch (RuntimeException failure) {
            if (charged && !jobMayExist(key)) refund(submissionId);
            throw failure;
        }
        return new RequestResult(submissionId, "QUEUED");
    }

    /** A job that may exist keeps its credit: refunding then would give a queued essay away for free. */
    private boolean jobMayExist(String key) {
        try {
            return jobs.jobExists(key);
        } catch (RuntimeException unknown) {
            return true;
        }
    }

    private void refund(UUID submissionId) {
        try {
            credits.refund(submissionId);
        } catch (RuntimeException failure) {
            // The credit stays with the essay; asking again queues the job without spending a second one.
            log.warn("Human grading credit not refunded: submissionId={}, errorType={}", submissionId,
                    failure.getClass().getSimpleName());
        }
    }

    public record RequestResult(UUID submissionId, String status) {}
}
