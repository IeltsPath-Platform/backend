package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.AssetType;

public record CreateContentAssetCommand(
        AssetType assetType,
        String textContent,
        String mediaReference,
        Integer durationSeconds,
        String checksum
) {}

