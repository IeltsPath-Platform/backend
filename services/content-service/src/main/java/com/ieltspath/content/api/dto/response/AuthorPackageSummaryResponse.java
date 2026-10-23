package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.api.AccessLevelCompatibility;
import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.application.result.AuthorPackageSummary;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;

import java.time.Instant;
import java.util.UUID;

public record AuthorPackageSummaryResponse(
        UUID id,
        String code,
        String title,
        PackageType packageType,
        AccessLevel accessLevel,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        UUID lessonId,
        UUID topicId,
        UUID courseId,
        String sourceCode,
        Instant createdAt,
        Instant updatedAt,
        Integer latestVersionNumber,
        boolean hasDraft
) {
    public static AuthorPackageSummaryResponse from(AuthorPackageSummary summary) {
        return new AuthorPackageSummaryResponse(summary.id(), summary.code(), summary.title(),
                summary.packageType(), AccessLevelCompatibility.toAccessLevel(summary.requiredFeatureKey()),
                summary.status(), summary.currentPublishedVersionId(), summary.lessonId(), summary.topicId(),
                summary.courseId(), summary.sourceCode(), summary.createdAt(), summary.updatedAt(),
                summary.latestVersionNumber(), summary.hasDraft());
    }
}
