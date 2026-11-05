package com.ieltspath.learning.application.command;

import java.util.UUID;

public record ChangeLearningGoalStatusCommand(
        UUID goalId,
        UUID userId,
        String status
) {
}

