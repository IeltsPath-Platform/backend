package com.group01.learningsupport.api.dto.response;

import com.group01.learningsupport.application.result.SavedVideoSegmentResult;

import java.time.Instant;
import java.util.UUID;

public record SavedVideoSegmentResponse(UUID id, UUID userId, UUID videoId, UUID segmentId, String transcriptSnapshot,
                                        String note, Instant createdAt) {
    public static SavedVideoSegmentResponse from(SavedVideoSegmentResult result) {
        return new SavedVideoSegmentResponse(result.id(), result.userId(), result.videoId(), result.segmentId(),
                result.transcriptSnapshot(), result.note(), result.createdAt());
    }
}
