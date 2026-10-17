package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.StaffInsightRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Closes a learner's open INACTIVE flag when they answer again. It runs before the submission commits, so the flag and
 * the answer change together; a submission that rolls back leaves the flag open.
 */
@Component
public class InactivityFlagCloser {
    private final StaffInsightRepository insight;

    public InactivityFlagCloser(StaffInsightRepository insight) {
        this.insight = insight;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onAnswers(QuestionEventsAppended appended) {
        insight.dismissInactivityFlags(appended.userId());
    }
}
