package com.group01.content.application.command;

import com.group01.content.domain.vo.BandRange;
import java.util.UUID;

public record CreateTopicCommand(
        UUID parentTopicId,
        String code,
        String name,
        int sortOrder,
        BandRange band
) {}

