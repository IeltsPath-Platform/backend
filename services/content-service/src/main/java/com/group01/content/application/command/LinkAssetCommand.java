package com.group01.content.application.command;

import java.util.UUID;

public record LinkAssetCommand(
        UUID assetId,
        UUID sectionId,
        UUID questionVersionId,
        int sortOrder
) {}

