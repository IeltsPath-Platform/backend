package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.PracticeSessionResult;
import java.time.Instant;
import java.util.UUID;

public record PracticeSessionResponse(UUID id, String kind, Instant expiresAt) {
    public static PracticeSessionResponse from(PracticeSessionResult result) {
        return new PracticeSessionResponse(result.id(), result.kind(), result.expiresAt());
    }
}
