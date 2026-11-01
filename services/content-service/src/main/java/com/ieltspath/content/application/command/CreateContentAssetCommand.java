package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.AccessTier;

public record CreateContentAssetCommand(
        AssetType assetType,
        String textContent,
        String mediaReference,
        Integer durationSeconds,
        String checksum,
        AccessTier accessTier
) {
    public CreateContentAssetCommand(AssetType assetType, String textContent, String mediaReference,
                                     Integer durationSeconds, String checksum) {
        this(assetType, textContent, mediaReference, durationSeconds, checksum, AccessTier.PUBLIC);
    }
}

