package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.PlacementPool;
import com.ieltspath.content.domain.vo.PackageType;

import java.util.UUID;

public record CreateContentPackageCommand(
        String code,
        String title,
        PackageType packageType,
        String requiredFeatureKey,
        UUID lessonId,
        String sourceCode,
        UUID topicId,
        UUID courseId,
        PlacementPool placementPool
) {
    public CreateContentPackageCommand(String code, String title, PackageType packageType,
                                       String requiredFeatureKey, UUID lessonId, String sourceCode, UUID topicId,
                                       UUID courseId) {
        this(code, title, packageType, requiredFeatureKey, lessonId, sourceCode, topicId, courseId, null);
    }

    public CreateContentPackageCommand(String code, String title, PackageType packageType,
                                       String requiredFeatureKey, UUID lessonId, String sourceCode) {
        this(code, title, packageType, requiredFeatureKey, lessonId, sourceCode, null, null);
    }

    public CreateContentPackageCommand(String code, String title, PackageType packageType,
                                       String requiredFeatureKey, UUID lessonId) {
        this(code, title, packageType, requiredFeatureKey, lessonId, null, null, null);
    }
}
