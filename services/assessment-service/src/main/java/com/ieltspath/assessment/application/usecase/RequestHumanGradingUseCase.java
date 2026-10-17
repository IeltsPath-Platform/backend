package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.application.port.HumanGradingRequestStore;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.infrastructure.client.AccessHumanGradingCreditClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RequestHumanGradingUseCase {
    private final HumanGradingRequestStore requests;
    private final GateEssayJobStore jobs;
    private final AccessHumanGradingCreditClient credits;

    public RequestHumanGradingUseCase(HumanGradingRequestStore requests, GateEssayJobStore jobs,
                                      AccessHumanGradingCreditClient credits) {
        this.requests = requests;
        this.jobs = jobs;
        this.credits = credits;
    }

    @Transactional
    public RequestResult execute(UUID userId, UUID attemptId, UUID itemId) {
        UUID submissionId = requests.submittedTopicEssay(userId, attemptId, itemId)
                .orElseThrow(() -> new AssessmentNotFoundException("Submitted topic essay not found"));
        credits.consume(submissionId);
        try {
            jobs.enqueueHuman(submissionId, userId, "human-credit:" + submissionId, "HUMAN_CREDIT");
            return new RequestResult(submissionId, "QUEUED");
        } catch (RuntimeException exception) {
            credits.refund(submissionId);
            throw exception;
        }
    }

    public record RequestResult(UUID submissionId, String status) {}
}
