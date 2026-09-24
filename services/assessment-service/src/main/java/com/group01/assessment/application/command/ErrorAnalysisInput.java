package com.group01.assessment.application.command;

import java.util.UUID;

public record ErrorAnalysisInput(
        UUID id,
        UUID attemptItemId,
        UUID knowledgePointId,
        String errorType,
        String explanation) {
}
