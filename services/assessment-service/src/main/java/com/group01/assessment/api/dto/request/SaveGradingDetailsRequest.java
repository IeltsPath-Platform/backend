package com.group01.assessment.api.dto.request;

import com.group01.assessment.application.command.ErrorAnalysisInput;
import com.group01.assessment.application.command.ItemResultInput;
import com.group01.assessment.application.command.KnowledgeJudgmentInput;
import com.group01.assessment.application.command.SaveGradingDetailsCommand;
import com.group01.assessment.application.command.SkillScoreInput;
import com.group01.assessment.domain.vo.GradingSource;
import com.group01.assessment.domain.vo.QualitativeJudgment;
import com.group01.assessment.domain.vo.Skill;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * A grader's details for one result version. {@code overallBand} always replaces the version's band; omitting it
 * clears the band. Same band bounds as {@link CreateAssessmentResultRequest}.
 */
public record SaveGradingDetailsRequest(
        @DecimalMin("0.0") @DecimalMax("9.0") Double overallBand,
        List<@Valid @NotNull SkillScore> skillScores,
        List<@Valid @NotNull ItemResult> itemResults,
        List<@Valid @NotNull ErrorAnalysis> errors,
        List<@Valid @NotNull KnowledgeJudgment> knowledgeJudgments) {

    public record SkillScore(@NotNull Skill skill,
                             @PositiveOrZero Double rawScore,
                             @DecimalMin("0.0") @DecimalMax("9.0") Double band,
                             @NotNull GradingSource gradingSource,
                             UUID feedbackRevisionId) {
    }

    /** {@code feedbackSnapshot} is a JSON document; an omitted snapshot is stored as {@code {}}. */
    public record ItemResult(@NotNull UUID attemptItemId,
                             @NotNull @PositiveOrZero Double score,
                             @NotNull @Positive Double maxScore,
                             Boolean correct,
                             @PositiveOrZero Long durationMilliseconds,
                             String feedbackSnapshot) {
    }

    public record ErrorAnalysis(@NotNull UUID id,
                                @NotNull UUID attemptItemId,
                                UUID knowledgePointId,
                                @NotBlank @Size(max = 50) String errorType,
                                String explanation) {
    }

    public record KnowledgeJudgment(@NotNull UUID attemptItemId,
                                    @NotNull UUID knowledgePointId,
                                    @NotNull QualitativeJudgment judgment) {
    }

    public SaveGradingDetailsCommand toCommand(UUID resultId) {
        return new SaveGradingDetailsCommand(
                resultId,
                overallBand,
                skillScores == null ? null : skillScores.stream()
                        .map(s -> new SkillScoreInput(s.skill(), s.rawScore(), s.band(), s.gradingSource(),
                                s.feedbackRevisionId()))
                        .toList(),
                itemResults == null ? null : itemResults.stream()
                        .map(i -> new ItemResultInput(i.attemptItemId(), i.score(), i.maxScore(), i.correct(),
                                i.durationMilliseconds(), i.feedbackSnapshot() == null ? "{}" : i.feedbackSnapshot()))
                        .toList(),
                errors == null ? null : errors.stream()
                        .map(e -> new ErrorAnalysisInput(e.id(), e.attemptItemId(), e.knowledgePointId(),
                                e.errorType(), e.explanation()))
                        .toList(),
                knowledgeJudgments == null ? null : knowledgeJudgments.stream()
                        .map(j -> new KnowledgeJudgmentInput(j.attemptItemId(), j.knowledgePointId(), j.judgment()))
                        .toList());
    }
}
