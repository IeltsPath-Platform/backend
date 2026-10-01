package com.group01.learningsupport.application.result;

import com.group01.learningsupport.domain.aggregate.SavedVideoSegment;

import java.time.Instant;
import java.util.UUID;

public record SavedVideoSegmentResult(UUID id, UUID userId, UUID videoId, UUID segmentId, String transcriptSnapshot,
                                      String note, Instant createdAt) {
    public static SavedVideoSegmentResult from(SavedVideoSegment segment) {
        return new SavedVideoSegmentResult(segment.getId(), segment.getUserId(), segment.getVideoId(),
                segment.getSegmentId(), segment.getTranscriptSnapshot(), segment.getNote(), segment.getCreatedAt());
    }
}
