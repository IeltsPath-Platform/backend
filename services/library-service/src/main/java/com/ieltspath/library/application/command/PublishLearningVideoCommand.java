package com.ieltspath.library.application.command;

import java.util.UUID;

public record PublishLearningVideoCommand(
        UUID videoId
) {}
