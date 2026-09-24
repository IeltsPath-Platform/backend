package com.group01.assessment.application.command;

import java.util.List;
import java.util.UUID;

public record SaveAssessmentResultDetailsCommand(
        UUID userId,
        UUID attemptId,
        List<SkillScoreInput> skillScores,
        List<ItemResultInput> itemResults,
        List<ErrorAnalysisInput> errors) {

    public SaveAssessmentResultDetailsCommand {
        skillScores = skillScores == null ? null : List.copyOf(skillScores);
        itemResults = itemResults == null ? null : List.copyOf(itemResults);
        errors = errors == null ? null : List.copyOf(errors);
    }
}
