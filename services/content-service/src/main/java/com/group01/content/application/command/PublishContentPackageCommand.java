package com.group01.content.application.command;

import java.util.UUID;

public record PublishContentPackageCommand(
        UUID packageId,
        UUID versionId,
        UUID publishedBy
) {}

