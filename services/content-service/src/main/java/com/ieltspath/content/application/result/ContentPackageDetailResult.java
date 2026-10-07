package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContentPackageDetailResult(
        UUID id,
        String code,
        String title,
        PackageType packageType,
        String requiredFeatureKey,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        int versionNumber,
        String rulesJson,
        Instant publishedAt,
        UUID publishedBy,
        Instant createdAt,
        Instant updatedAt,
        List<ContentSectionResult> sections
) {}
