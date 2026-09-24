package com.group01.assessment.application.command;

import com.group01.assessment.domain.vo.GradingSource;
import com.group01.assessment.domain.vo.Skill;

import java.util.UUID;

public record SkillScoreInput(
        Skill skill,
        Double rawScore,
        Double band,
        GradingSource gradingSource,
        UUID feedbackRevisionId) {
}
