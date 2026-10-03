package com.group01.content.application.result;

import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.PublicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ContentPackageResult(
        UUID id,
        String code,
        String title,
        PackageType packageType,
        String requiredFeatureKey,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        Instant createdAt,
        Instant updatedAt,
        UUID lessonId
) {
    public static ContentPackageResult of(ContentPackage pkg) {
        return new ContentPackageResult(pkg.getId(), pkg.getCode(), pkg.getTitle(), pkg.getPackageType(),
                pkg.getRequiredFeatureKey(), pkg.getStatus(), pkg.getCurrentPublishedVersionId(), pkg.getCreatedAt(),
                pkg.getUpdatedAt(), pkg.getLessonId());
    }
}
