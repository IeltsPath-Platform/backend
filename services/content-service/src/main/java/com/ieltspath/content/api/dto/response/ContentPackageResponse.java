package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.api.AccessLevelCompatibility;
import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.application.result.ContentPackageResult;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ContentPackageResponse(
        UUID id,
        String code,
        String title,
        PackageType packageType,
        AccessLevel accessLevel,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        Instant createdAt,
        Instant updatedAt,
        UUID lessonId
) {
    public static ContentPackageResponse from(ContentPackageResult result) {
        return new ContentPackageResponse(
                result.id(),
                result.code(),
                result.title(),
                result.packageType(),
                AccessLevelCompatibility.toAccessLevel(result.requiredFeatureKey()),
                result.status(),
                result.currentPublishedVersionId(),
                result.createdAt(),
                result.updatedAt(),
                result.lessonId()
        );
    }
}
