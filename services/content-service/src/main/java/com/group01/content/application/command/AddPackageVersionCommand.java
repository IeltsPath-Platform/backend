package com.group01.content.application.command;

import java.util.UUID;

public record AddPackageVersionCommand(
        UUID packageId,
        int versionNumber,
        String rulesJson
) {}

