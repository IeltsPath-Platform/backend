package com.group01.game.domain.aggregate;

import java.util.Map;
import java.util.UUID;

public record GameAnswer(UUID id, UUID sessionId, int itemSequence, UUID vocabularySenseId,
                         UUID questionVersionId, Map<String, Object> itemSnapshot,
                         Map<String, Object> responsePayload, boolean correct, long durationMilliseconds) {
    public GameAnswer {
        if (itemSequence < 1) throw new IllegalArgumentException("itemSequence must be positive");
        if (durationMilliseconds < 0) throw new IllegalArgumentException("durationMilliseconds must not be negative");
        itemSnapshot = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(itemSnapshot));
        responsePayload = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(responsePayload));
    }
}
