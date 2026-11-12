package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.aggregate.PracticeSession;
import java.time.Instant;
import java.util.UUID;

public record PracticeSessionResult(UUID id, String kind, Instant expiresAt) {
    public static PracticeSessionResult from(PracticeSession session) {
        return new PracticeSessionResult(session.id(), session.kind(), session.expiresAt());
    }
}
