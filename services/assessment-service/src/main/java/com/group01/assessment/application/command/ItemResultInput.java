package com.group01.assessment.application.command;

import java.util.UUID;

public record ItemResultInput(
        UUID attemptItemId,
        Double score,
        Boolean correct,
        Long durationMilliseconds,
        String feedbackSnapshot) {
}
