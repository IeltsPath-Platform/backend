package com.group01.content.api.dto.response;

import com.group01.content.api.AccessLevelCompatibility;
import com.group01.content.api.dto.AccessLevel;
import com.group01.content.application.result.ContentPackageDetailResult;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.PublicationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContentPackageDetailResponse(
        UUID id,
        String code,
        String title,
        PackageType packageType,
        AccessLevel accessLevel,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        int versionNumber,
        String rulesJson,
        Instant publishedAt,
        UUID publishedBy,
        Instant createdAt,
        Instant updatedAt,
        List<ContentSectionResponse> sections
) {
    public static ContentPackageDetailResponse from(ContentPackageDetailResult result) {
        List<ContentSectionResponse> sectionResponses = result.sections() != null
                ? result.sections().stream().map(ContentSectionResponse::from).toList()
                : List.of();
        return new ContentPackageDetailResponse(
                result.id(),
                result.code(),
                result.title(),
                result.packageType(),
                AccessLevelCompatibility.toAccessLevel(result.requiredFeatureKey()),
                result.status(),
                result.currentPublishedVersionId(),
                result.versionNumber(),
                result.rulesJson(),
                result.publishedAt(),
                result.publishedBy(),
                result.createdAt(),
                result.updatedAt(),
                sectionResponses
        );
    }
}
