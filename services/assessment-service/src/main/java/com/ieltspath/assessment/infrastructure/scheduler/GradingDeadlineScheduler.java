package com.ieltspath.assessment.infrastructure.scheduler;

import com.ieltspath.assessment.application.usecase.AnnounceDueSoonGradingUseCase;
import com.ieltspath.assessment.application.usecase.AnnounceOverdueGradingUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Watches the examiners' deadlines on a timer: reminds the examiner when a job is 12 hours from being late (36 hours
 * after the choice), and tells the learner when it is late (48 hours). It needs the outbox relay to deliver what it
 * records, so it follows its switch.
 */
@Component
@ConditionalOnProperty(name = "assessment.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class GradingDeadlineScheduler {
    private static final Logger log = LoggerFactory.getLogger(GradingDeadlineScheduler.class);

    private final AnnounceDueSoonGradingUseCase remind;
    private final AnnounceOverdueGradingUseCase announce;

    public GradingDeadlineScheduler(AnnounceDueSoonGradingUseCase remind, AnnounceOverdueGradingUseCase announce) {
        this.remind = remind;
        this.announce = announce;
    }

    @Scheduled(fixedDelayString = "${assessment.examiner.overdue-notice.poll-interval:PT5M}",
            initialDelayString = "${assessment.examiner.overdue-notice.initial-delay:PT1M}")
    public void checkGradingDeadlines() {
        Instant now = Instant.now();
        // Each check stands alone: one failing must not stop the other, and the next tick tries again.
        try {
            int reminders = remind.execute(now);
            if (reminders > 0) log.info("Reminded the examiners of {} grading job(s) that will be late soon", reminders);
        } catch (RuntimeException failure) {
            log.warn("Due-soon grading check failed: {}", failure.getMessage());
        }
        try {
            int notices = announce.execute(now);
            if (notices > 0) log.info("Announced {} late grading job(s) to the learners", notices);
        } catch (RuntimeException failure) {
            log.warn("Late grading check failed: {}", failure.getMessage());
        }
    }
}
