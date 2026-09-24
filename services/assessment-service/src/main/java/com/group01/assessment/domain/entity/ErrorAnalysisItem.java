package com.group01.assessment.domain.entity;

import java.util.UUID;

public record ErrorAnalysisItem(UUID id, UUID itemResultId, UUID knowledgePointId,
                                String errorType, String explanation) {
}
