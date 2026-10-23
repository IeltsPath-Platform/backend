package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.vo.PublicationStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record AuthorVersionResult(
        UUID id,
        int versionNumber,
        PublicationStatus status,
        String rulesJson,
        Instant publishedAt,
        Instant publishAt,
        String lastPublishError,
        List<AuthorSectionResult> sections
) {
    public static AuthorVersionResult of(ContentPackageVersion version) {
        List<AuthorSectionResult> sections = version.getSections().stream()
                .sorted(Comparator.comparingInt(s -> s.getSortOrder()))
                .map(AuthorSectionResult::of)
                .toList();
        return new AuthorVersionResult(version.getId(), version.getVersionNumber(), version.getStatus(),
                version.getRulesJson(), version.getPublishedAt(), version.getPublishAt(),
                version.getLastPublishError(), sections);
    }
}
