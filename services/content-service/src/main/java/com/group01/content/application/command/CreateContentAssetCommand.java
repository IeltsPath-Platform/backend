package com.group01.content.application.command;

import com.group01.content.domain.vo.AssetType;

public record CreateContentAssetCommand(
        AssetType assetType,
        String textContent,
        String mediaReference,
        Integer durationSeconds,
        String checksum
) {}

