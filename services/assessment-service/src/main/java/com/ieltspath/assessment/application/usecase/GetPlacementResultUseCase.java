package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.result.PlacementResult;
import com.ieltspath.assessment.application.port.PlacementSpeakingGradeStore;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AssessmentResultRepository;
import com.ieltspath.assessment.domain.vo.AttemptType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

/** The learner's own completed placement with its skill bands and report detail. Not found until grading has completed. */
@Service
public class GetPlacementResultUseCase {
    private final AssessmentAttemptRepository attempts;
    private final AssessmentResultRepository results;
    private final PlacementGradingService grading;
    private final PlacementReportAssembler report;
    private final PlacementSpeakingGradeStore speakingGrades;
    private final ObjectMapper json;

    @Autowired
    public GetPlacementResultUseCase(AssessmentAttemptRepository attempts, AssessmentResultRepository results,
                                     PlacementGradingService grading, PlacementReportAssembler report,
                                     PlacementSpeakingGradeStore speakingGrades, ObjectMapper json) {
        this.attempts = attempts;
        this.results = results;
        this.grading = grading;
        this.report = report;
        this.speakingGrades = speakingGrades;
        this.json = json;
    }

    public GetPlacementResultUseCase(AssessmentAttemptRepository attempts, AssessmentResultRepository results,
                                     PlacementGradingService grading, PlacementReportAssembler report) {
        this(attempts, results, grading, report, null, new ObjectMapper());
    }

    @Transactional(readOnly = true)
    public PlacementResult execute(UUID userId, UUID attemptId) {
        var attempt = attempts.findByIdAndUserId(attemptId, userId)
                .filter(candidate -> candidate.getAttemptType() == AttemptType.PLACEMENT)
                .orElseThrow(() -> new AssessmentNotFoundException("Placement attempt not found"));
        var result = results.findLatestCompletedByAttemptId(attemptId)
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment result not found"));
        var skills = grading.skillBands(attemptId, result.id()).entrySet().stream()
                .map(entry -> new PlacementResult.SkillBand(entry.getKey(), entry.getValue()))
                .toList();
        return new PlacementResult(attemptId, result.overallBand(), result.completedAt(), skills,
                report.sections(attemptId, result.id()), attempt.getPlacementPool(),
                result.placementPool() == null ? null : result.placementPool().ceiling(),
                result.placementPool() != null && result.placementPool() != com.ieltspath.assessment.domain.vo.PlacementPool.BAND_5_9
                        && result.uncappedBand() != null && result.uncappedBand() >= result.placementPool().ceiling(),
                result.partial(), speakingFeedback(attemptId));
    }

    private PlacementResult.SpeakingFeedback speakingFeedback(UUID attemptId) {
        if (speakingGrades == null) return null;
        var grade = speakingGrades.find(attemptId).orElse(null);
        if (grade == null || !"GRADED".equals(grade.status()) || grade.feedback() == null) return null;
        try {
            JsonNode data = json.readTree(grade.feedback());
            var criteria = new java.util.ArrayList<PlacementResult.Criterion>();
            for (JsonNode criterion : data.path("criteria")) {
                criteria.add(new PlacementResult.Criterion(criterion.path("code").asText(),
                        criterion.path("band").isNumber() ? criterion.path("band").doubleValue() : null,
                        criterion.path("comment").isTextual() ? criterion.path("comment").asText() : null));
            }
            return new PlacementResult.SpeakingFeedback(data.path("summary").isTextual()
                    ? data.path("summary").asText() : null, criteria);
        } catch (Exception ignored) {
            return null;
        }
    }
}
