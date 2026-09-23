package com.group01.assessment.domain.entity;
import com.group01.assessment.domain.vo.GradingSource; import java.util.UUID;
public record ItemResult(UUID id,UUID resultId,UUID attemptItemId,Double rawScore,Double maxScore,Boolean correct,GradingSource gradingSource) {}
