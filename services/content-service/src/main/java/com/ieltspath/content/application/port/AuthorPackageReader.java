package com.ieltspath.content.application.port;

import com.ieltspath.content.application.result.AuthorPackageExtras;
import com.ieltspath.content.application.result.AuthorPackageSummary;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;

import java.util.List;
import java.util.UUID;

/** Read side of package authoring: the list and the data the aggregate does not carry, each in a fixed number of queries. */
public interface AuthorPackageReader {

    /** Any filter may be null; {@code q} matches the code or the title, ignoring case. */
    record Filter(PackageType type, PublicationStatus status, UUID lessonId, UUID topicId, UUID courseId, String q) {
    }

    int LIST_LIMIT = 500;

    /** Most recently updated first, at most {@link #LIST_LIMIT} rows. */
    List<AuthorPackageSummary> list(Filter filter);

    AuthorPackageExtras extras(UUID packageId);
}
