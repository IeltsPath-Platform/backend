package com.group01.library.application.command;

import java.util.UUID;

public record PublishLearningVideoCommand(
        UUID videoId
) {}
