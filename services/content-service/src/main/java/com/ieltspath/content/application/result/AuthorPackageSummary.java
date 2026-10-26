package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;

import java.time.Instant;
import java.util.UUID;

/** One row of the author's package list. */
public record AuthorPackageSummary(
        UUID id,
        String code,
        String title,
        PackageType packageType,
        String requiredFeatureKey,
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
}
