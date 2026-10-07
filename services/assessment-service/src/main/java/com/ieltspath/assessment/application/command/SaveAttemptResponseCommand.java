package com.ieltspath.assessment.application.command;

import java.util.UUID;

public record SaveAttemptResponseCommand(UUID userId, UUID attemptId, UUID itemId,
                                         String payload, int schemaVersion, long expectedRevision) {}
