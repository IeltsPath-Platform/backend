package com.group01.assessment.infrastructure.scheduler;

import com.group01.assessment.application.usecase.GradeGateEssayJobUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls the essay queue; one failing batch does not stop later scheduled work. */
@Component
@ConditionalOnProperty(name = "assessment.llm-grading.enabled", havingValue = "true", matchIfMissing = true)
public class GateEssayGradingScheduler {
    private static final Logger log = LoggerFactory.getLogger(GateEssayGradingScheduler.class);
    private final GradeGateEssayJobUseCase grading;

    public GateEssayGradingScheduler(GradeGateEssayJobUseCase grading) { this.grading = grading; }

    @Scheduled(fixedDelayString = "${assessment.llm-grading.poll-interval:PT5S}")
    public void gradeQueuedEssays() {
        try {
            grading.gradeBatch();
        } catch (RuntimeException exception) {
            log.warn("Gate essay grading batch failed: {}", exception.getClass().getSimpleName());
        }
    }
}
