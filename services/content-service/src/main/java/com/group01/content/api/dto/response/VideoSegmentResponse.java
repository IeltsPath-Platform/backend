package com.group01.content.api.dto.response;

import com.group01.content.application.result.VideoSegmentResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VideoSegmentResponse(
        UUID id,
        UUID videoId,
        int sequenceNo,
        int startMs,
        int endMs,
        String transcript,
        String translationVi,
        Instant createdAt,
        Instant updatedAt,
        List<VideoSegmentLexicalEntryResponse> lexicalEntries
) {
    public static VideoSegmentResponse from(VideoSegmentResult result) {
        List<VideoSegmentLexicalEntryResponse> entryResponses = result.lexicalEntries() != null
                ? result.lexicalEntries().stream().map(VideoSegmentLexicalEntryResponse::from).toList()
                : List.of();
        return new VideoSegmentResponse(
                result.id(),
                result.videoId(),
                result.sequenceNo(),
                result.startMs(),
                result.endMs(),
                result.transcript(),
                result.translationVi(),
                result.createdAt(),
                result.updatedAt(),
                entryResponses
        );
    }
}

