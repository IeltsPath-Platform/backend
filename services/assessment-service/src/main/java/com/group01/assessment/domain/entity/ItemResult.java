package com.group01.assessment.domain.entity;

import java.util.UUID;

public record ItemResult(UUID id, UUID resultId, UUID attemptItemId, Double score, Boolean correct,
                         Long durationMilliseconds, String feedbackSnapshot) {
}
