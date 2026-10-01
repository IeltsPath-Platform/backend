package com.group01.library.domain.aggregate;

import com.group01.library.domain.exception.InvalidDataException;

import com.group01.library.domain.DomainChecks;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class SavedVideoSegment {
    private final UUID id;
    private final UUID userId;
    private final UUID videoId;
    private final UUID segmentId;
    private final String transcriptSnapshot;
    private final String note;
    private final Instant createdAt;

    public static SavedVideoSegment create(
            UUID userId,
            UUID videoId,
            UUID segmentId,
            String transcriptSnapshot,
            String note
    ) {
        if (videoId == null || segmentId == null) {
            throw new InvalidDataException("saved segment không hợp lệ");
        }
        return new SavedVideoSegment(
                UUID.randomUUID(),
                DomainChecks.userId(userId),
                videoId,
                segmentId,
                DomainChecks.required(transcriptSnapshot, DomainChecks.TRANSCRIPT_MAX, "transcriptSnapshot"),
                DomainChecks.optional(note, DomainChecks.SEGMENT_NOTE_MAX, "note"),
                null
        );
    }
}
