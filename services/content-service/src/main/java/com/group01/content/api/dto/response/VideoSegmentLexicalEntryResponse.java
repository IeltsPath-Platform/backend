package com.group01.content.api.dto.response;

import com.group01.content.domain.entity.VideoSegmentLexicalEntry;

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
    public static VideoSegmentLexicalEntryResponse from(VideoSegmentLexicalEntry entity) {
        return new VideoSegmentLexicalEntryResponse(
                entity.getId(),
                entity.getSegmentId(),
                entity.getVocabularySenseId(),
                entity.getSurfaceText(),
                entity.getStartChar(),
                entity.getEndChar(),
                entity.getSortOrder(),
                entity.getCreatedAt()
        );
    }
}

