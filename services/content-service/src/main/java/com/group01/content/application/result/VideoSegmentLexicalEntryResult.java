package com.group01.content.application.result;

import java.time.Instant;
import java.util.UUID;

public record VideoSegmentLexicalEntryResult(
        UUID id,
        UUID segmentId,
        UUID vocabularySenseId,
        String surfaceText,
        int startChar,
        int endChar,
        int sortOrder,
        Instant createdAt
) {
}
