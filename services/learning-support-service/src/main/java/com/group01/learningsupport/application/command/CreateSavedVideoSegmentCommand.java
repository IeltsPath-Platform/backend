package com.group01.learningsupport.application.command;

import java.util.UUID;

public record CreateSavedVideoSegmentCommand(
        UUID userId,
        UUID videoId,
        UUID segmentId,
        String transcriptSnapshot,
        String note
) {
}
