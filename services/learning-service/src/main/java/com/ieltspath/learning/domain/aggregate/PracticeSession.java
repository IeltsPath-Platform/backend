package com.ieltspath.learning.domain.aggregate;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record PracticeSession(UUID id, UUID userId, String kind, UUID requestId,
        Instant createdAt, Instant expiresAt) {
    public static final long VALIDITY_HOURS = 6;
    private static final Set<String> KINDS = Set.of("WRITING", "SPEAKING", "VOCABULARY");

    public PracticeSession {
        Objects.requireNonNull(id);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(requestId);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(expiresAt);
        if (!KINDS.contains(kind)) throw new IllegalArgumentException("Unsupported practice session kind");
        if (!expiresAt.isAfter(createdAt)) throw new IllegalArgumentException("Practice session expiry must follow creation");
    }

    public static PracticeSession start(UUID userId, String kind, UUID requestId, Instant now) {
        return new PracticeSession(UUID.randomUUID(), userId, kind, requestId, now, now.plusSeconds(VALIDITY_HOURS * 3600));
    }

    public boolean isUsableFor(UUID user, String expectedKind, Instant now) {
        return userId.equals(user) && kind.equals(expectedKind) && now.isBefore(expiresAt);
    }
}
