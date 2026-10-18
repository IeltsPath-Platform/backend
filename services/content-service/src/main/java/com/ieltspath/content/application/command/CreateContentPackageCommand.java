package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.PackageType;

import java.util.UUID;

public record CreateContentPackageCommand(
        String code,
        String title,
        PackageType packageType,
        String requiredFeatureKey,
        UUID lessonId,
        String sourceCode
) {
    public CreateContentPackageCommand(String code, String title, PackageType packageType,
                                       String requiredFeatureKey, UUID lessonId) {
        this(code, title, packageType, requiredFeatureKey, lessonId, null);
    }
}
