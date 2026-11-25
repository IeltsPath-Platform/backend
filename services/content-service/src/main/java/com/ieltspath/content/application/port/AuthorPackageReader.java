package com.ieltspath.content.application.port;

import com.ieltspath.content.application.result.AuthorPackageExtras;
import com.ieltspath.content.application.result.AuthorPackageSummary;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PlacementPool;
import com.ieltspath.content.domain.vo.PublicationStatus;

import java.util.List;
import java.util.UUID;

/** Read side of package authoring: the list and the data the aggregate does not carry, each in a fixed number of queries. */
public interface AuthorPackageReader {

    /** Any filter may be null; {@code q} matches the code or the title, ignoring case. */
    record Filter(PackageType type, PublicationStatus status, UUID lessonId, UUID topicId, UUID courseId, String q,
                  PlacementPool placementPool, Boolean premium, String skill, List<PackageType> excludeTypes) {
        public Filter {
            excludeTypes = excludeTypes == null ? List.of()
                    : excludeTypes.stream().filter(java.util.Objects::nonNull).distinct().toList();
        }

        public Filter(PackageType type, PublicationStatus status, UUID lessonId, UUID topicId, UUID courseId, String q,
                      PlacementPool placementPool, Boolean premium, String skill) {
            this(type, status, lessonId, topicId, courseId, q, placementPool, premium, skill, List.of());
        }

        public Filter(PackageType type, PublicationStatus status, UUID lessonId, UUID topicId, UUID courseId, String q,
                      PlacementPool placementPool) {
            this(type, status, lessonId, topicId, courseId, q, placementPool, null, null, List.of());
        }

        public Filter(PackageType type, PublicationStatus status, UUID lessonId, UUID topicId, UUID courseId, String q) {
            this(type, status, lessonId, topicId, courseId, q, null, null, null, List.of());
        }
    }

    /** One page of the list and how many rows match the filter in all. */
    record Page(long totalElements, List<AuthorPackageSummary> items) {
    }

    /** Most recently updated first; {@code page} is zero-based. */
    Page list(Filter filter, int page, int size);

    AuthorPackageExtras extras(UUID packageId);
}
