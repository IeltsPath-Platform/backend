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
        var attempt = attempts.findByIdAndUserId(attemptId, userId)
                .filter(candidate -> candidate.getAttemptType() == AttemptType.PLACEMENT)
                .orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài kiểm tra đầu vào."));
        var result = results.findLatestCompletedByAttemptId(attemptId)
                .orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy kết quả bài thi."));
        var skills = grading.skillBands(attemptId, result.id()).entrySet().stream()
                .map(entry -> new PlacementResult.SkillBand(entry.getKey(), entry.getValue()))
                .toList();
        return new PlacementResult(attemptId, result.overallBand(), result.completedAt(), skills,
                report.sections(attemptId, result.id()), attempt.getPlacementPool(),
                result.placementPool() == null ? null : result.placementPool().ceiling(),
                result.placementPool() != null && result.placementPool() != com.ieltspath.assessment.domain.vo.PlacementPool.BAND_5_9
                        && result.uncappedBand() != null && result.uncappedBand() >= result.placementPool().ceiling(),
                result.partial());
    }
}
