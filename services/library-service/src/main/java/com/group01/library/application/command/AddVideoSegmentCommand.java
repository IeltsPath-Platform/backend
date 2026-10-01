package com.group01.library.application.command;

import java.util.UUID;

public record AddVideoSegmentCommand(
        UUID videoId,
        int sequenceNo,
        int startMs,
        int endMs,
        String transcript,
        String translationVi
) {}
