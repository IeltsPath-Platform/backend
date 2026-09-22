package com.group01.content.application.result;

import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.PublicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ContentPackageResult(
        UUID id,
        String code,
        String title,
        PackageType packageType,
        AccessLevel accessLevel,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        Instant createdAt,
        Instant updatedAt
) {}

