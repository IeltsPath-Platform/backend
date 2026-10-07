package com.ieltspath.assessment.application.command;

import com.ieltspath.assessment.domain.vo.GradingSource;
import com.ieltspath.assessment.domain.vo.Skill;

import java.util.UUID;

public record SkillScoreInput(
        Skill skill,
        Double rawScore,
        Double band,
        GradingSource gradingSource,
        UUID feedbackRevisionId) {
}
