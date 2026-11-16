package com.ieltspath.library.infrastructure.scheduler;

import com.ieltspath.library.application.usecase.QueueVocabularyReviewDueUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Queues the daily review reminders. Safe on several instances: each learner's event id is fixed for the day, so whichever
 * instance runs second queues nothing.
 */
@Component
@ConditionalOnProperty(name = "library.review-due.enabled", havingValue = "true", matchIfMissing = true)
public class VocabularyReviewDueScheduler {
    private static final Logger log = LoggerFactory.getLogger(VocabularyReviewDueScheduler.class);

    private final QueueVocabularyReviewDueUseCase useCase;
    private final Clock clock;

    public VocabularyReviewDueScheduler(QueueVocabularyReviewDueUseCase useCase, Clock libraryClock) {
        this.useCase = useCase;
        this.clock = libraryClock;
    }

    @Scheduled(cron = "${library.review-due.cron:0 0 8 * * *}", zone = "${library.review-due.zone:Asia/Ho_Chi_Minh}")
    public void queueReminders() {
        try {
            int queued = useCase.execute(clock.instant());
            log.info("Vocabulary review reminders queued count={}", queued);
        } catch (RuntimeException failure) {
            // The next day's run starts fresh; a broken database must not stop the scheduler thread.
            log.warn("Vocabulary review reminders failed: errorType={}", failure.getClass().getSimpleName());
        }
    }
}
