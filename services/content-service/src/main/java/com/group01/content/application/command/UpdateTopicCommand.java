package com.group01.content.application.command;

import com.group01.content.domain.vo.BandRange;
import com.group01.content.domain.vo.ContentStatus;

import java.util.UUID;

public record UpdateTopicCommand(
        UUID id,
        UUID parentTopicId,
        String name,
        int sortOrder,
        ContentStatus status,
        BandRange band
) {}

