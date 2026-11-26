package com.ieltspath.assessment.infrastructure.scheduler;

import com.ieltspath.assessment.application.port.PlacementSpeakingGradeStore;
import com.ieltspath.assessment.application.usecase.PlacementGradingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Marks placement Speaking requests past their ten-minute deadline as failed and lets normal grading finalize. */
@Component
public class PlacementSpeakingTimeoutScheduler {
    private final PlacementSpeakingGradeStore grades;
    private final PlacementGradingService grading;

    public PlacementSpeakingTimeoutScheduler(PlacementSpeakingGradeStore grades, PlacementGradingService grading) {
        this.grades = grades; this.grading = grading;
    }

    @Scheduled(fixedDelayString = "${assessment.placement.speaking-grading.timeout-poll-ms:30000}")
    @Transactional
    public void expire() {
        for (var attemptId : grades.failExpired(Instant.now(), 50)) grading.completeIfGraded(attemptId);
    }
}
