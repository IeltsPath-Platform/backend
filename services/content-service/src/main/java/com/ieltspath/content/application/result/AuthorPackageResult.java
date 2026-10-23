package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A package as its author sees it: every version, not only the published one. */
public record AuthorPackageResult(
        UUID id,
        String code,
        String title,
        PackageType packageType,
        String requiredFeatureKey,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        UUID lessonId,
        UUID courseId,
        String sourceCode,
        Instant createdAt,
        Instant updatedAt,
        List<AuthorVersionResult> versions
) {
    public static AuthorPackageResult of(ContentPackage pkg) {
        List<AuthorVersionResult> versions = pkg.getVersions().stream()
                .sorted(java.util.Comparator.comparingInt(v -> v.getVersionNumber()))
                .map(AuthorVersionResult::of)
                .toList();
        return new AuthorPackageResult(pkg.getId(), pkg.getCode(), pkg.getTitle(), pkg.getPackageType(),
                pkg.getRequiredFeatureKey(), pkg.getStatus(), pkg.getCurrentPublishedVersionId(), pkg.getLessonId(),
                pkg.getCourseId(), pkg.getSourceCode(), pkg.getCreatedAt(), pkg.getUpdatedAt(), versions);
    }
}
