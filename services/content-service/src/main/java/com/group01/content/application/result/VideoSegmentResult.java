package com.group01.content.application.result;

import com.group01.content.domain.entity.VideoSegmentLexicalEntry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VideoSegmentResult(
        UUID id,
        UUID videoId,
        int sequenceNo,
        int startMs,
        int endMs,
        String transcript,
        String translationVi,
        Instant createdAt,
        Instant updatedAt,
        List<VideoSegmentLexicalEntry> lexicalEntries
) {}

