package com.group01.content.api.dto.response;

import com.group01.content.application.result.VideoSegmentLexicalEntryResult;

import java.time.Instant;
import java.util.UUID;

public record VideoSegmentLexicalEntryResponse(
        UUID id,
        UUID segmentId,
        UUID vocabularySenseId,
        String surfaceText,
        int startChar,
        int endChar,
        int sortOrder,
        Instant createdAt
) {
    public static VideoSegmentLexicalEntryResponse from(VideoSegmentLexicalEntryResult result) {
        return new VideoSegmentLexicalEntryResponse(
                result.id(),
                result.segmentId(),
                result.vocabularySenseId(),
                result.surfaceText(),
                result.startChar(),
                result.endChar(),
                result.sortOrder(),
                result.createdAt()
        );
    }
}
