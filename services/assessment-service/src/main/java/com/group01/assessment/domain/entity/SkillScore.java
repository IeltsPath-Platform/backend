package com.group01.assessment.domain.entity;
import com.group01.assessment.domain.vo.*; import java.util.UUID;
public record SkillScore(UUID id,UUID resultId,Skill skill,Double rawScore,Double band,GradingSource gradingSource,UUID feedbackRevisionId) {}
