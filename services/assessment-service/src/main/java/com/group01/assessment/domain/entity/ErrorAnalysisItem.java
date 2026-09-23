package com.group01.assessment.domain.entity;
import java.util.UUID;
public record ErrorAnalysisItem(UUID id,UUID resultId,UUID attemptItemId,String taxonomyCode,String severity,String note) {}
