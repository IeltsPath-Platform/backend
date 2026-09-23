package com.group01.content.api.dto.response;

import com.group01.content.application.result.ContentPackageResult;
import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.PublicationStatus;

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
        Instant updatedAt
) {
    public static ContentPackageResponse from(ContentPackageResult result) {
        return new ContentPackageResponse(
                result.id(),
                result.code(),
                result.title(),
                result.packageType(),
                result.accessLevel(),
                result.status(),
                result.currentPublishedVersionId(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}

