package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.result.PlacementResult;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AssessmentResultRepository;
import com.ieltspath.assessment.domain.vo.AttemptType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** The learner's own completed placement with its skill bands and report detail. Not found until grading has completed. */
@Service
public class GetPlacementResultUseCase {
    private final AssessmentAttemptRepository attempts;
    private final AssessmentResultRepository results;
    private final PlacementGradingService grading;
    private final PlacementReportAssembler report;

    public GetPlacementResultUseCase(AssessmentAttemptRepository attempts, AssessmentResultRepository results,
                                     PlacementGradingService grading, PlacementReportAssembler report) {
        this.attempts = attempts;
        this.results = results;
        this.grading = grading;
        this.report = report;
    }

    @Transactional(readOnly = true)
    public PlacementResult execute(UUID userId, UUID attemptId) {
        attempts.findByIdAndUserId(attemptId, userId)
                .filter(attempt -> attempt.getAttemptType() == AttemptType.PLACEMENT)
                .orElseThrow(() -> new AssessmentNotFoundException("Placement attempt not found"));
        var result = results.findLatestCompletedByAttemptId(attemptId)
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment result not found"));
        var skills = grading.skillBands(attemptId, result.id()).entrySet().stream()
                .map(entry -> new PlacementResult.SkillBand(entry.getKey(), entry.getValue()))
                .toList();
        return new PlacementResult(attemptId, result.overallBand(), result.completedAt(), skills,
                report.sections(attemptId, result.id()));
    }
}
