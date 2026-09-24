package com.group01.assessment.application.command;

import java.util.List;
import java.util.UUID;

/** A grader's details for one result version, addressed by result rather than by the learner's attempt. */
public record SaveGradingDetailsCommand(
        UUID resultId,
        Double overallBand,
        List<SkillScoreInput> skillScores,
        List<ItemResultInput> itemResults,
        List<ErrorAnalysisInput> errors,
        List<KnowledgeJudgmentInput> knowledgeJudgments) {

    public SaveGradingDetailsCommand {
        skillScores = skillScores == null ? null : List.copyOf(skillScores);
        itemResults = itemResults == null ? null : List.copyOf(itemResults);
        errors = errors == null ? null : List.copyOf(errors);
        knowledgeJudgments = knowledgeJudgments == null ? null : List.copyOf(knowledgeJudgments);
    }
}
