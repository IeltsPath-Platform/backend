package com.group01.content.application.command;

import com.group01.content.domain.vo.PackageType;

public record CreateContentPackageCommand(
        String code,
        String title,
        PackageType packageType,
        String requiredFeatureKey
) {}
