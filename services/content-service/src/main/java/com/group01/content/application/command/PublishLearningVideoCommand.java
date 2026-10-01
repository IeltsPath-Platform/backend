package com.group01.content.application.command;

import java.util.UUID;

public record PublishLearningVideoCommand(
        UUID videoId
) {}

