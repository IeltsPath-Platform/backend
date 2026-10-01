package com.group01.library.application.command;

import java.util.UUID;

public record AddSegmentLexicalEntryCommand(
        UUID segmentId,
        UUID vocabularySenseId,
        String surfaceText,
        int startChar,
        int endChar,
        int sortOrder
) {}
