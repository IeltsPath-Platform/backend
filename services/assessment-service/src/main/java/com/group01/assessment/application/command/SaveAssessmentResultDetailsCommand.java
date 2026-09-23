package com.group01.assessment.application.command;
import com.group01.assessment.domain.entity.*; import java.util.List; import java.util.UUID;
public record SaveAssessmentResultDetailsCommand(UUID userId,UUID attemptId,List<SkillScore> skillScores,List<ItemResult> itemResults,List<ErrorAnalysisItem> errors) {}
