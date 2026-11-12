package com.ieltspath.notification.infrastructure;

import com.ieltspath.notification.application.LearningMailUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Sends the examiners' hourly digest of new grading jobs. */
@Component
@ConditionalOnExpression(LearningMailGate.EXPRESSION)
public class LearningMailScheduler {
    private final LearningMailUseCase mail;

    public LearningMailScheduler(LearningMailUseCase mail) {
        this.mail = mail;
    }

    @Scheduled(fixedDelayString = "${notification.mail.examiner-digest-interval:PT1H}",
            initialDelayString = "${notification.mail.examiner-digest-initial-delay:PT5M}")
    public void flushExaminerDigests() {
        mail.flushExaminerDigests(Instant.now());
    }
}
