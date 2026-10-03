package com.group01.content.application.command;

import com.group01.content.domain.vo.PackageType;

import java.util.UUID;

public record CreateContentPackageCommand(
        String code,
        String title,
        PackageType packageType,
        String requiredFeatureKey,
        UUID lessonId
) {}
