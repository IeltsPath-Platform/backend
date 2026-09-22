package com.group01.content.application.command;

import java.util.UUID;

public record CreateTopicCommand(
        UUID parentTopicId,
        String code,
        String name,
        int sortOrder
) {}

