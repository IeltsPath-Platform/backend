package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.InvalidPackageLessonException;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;

import java.time.Instant;
import java.util.*;

public class ContentPackage {
    private final UUID id;
    private String code;
    private String title;
    private PackageType packageType;
    private String requiredFeatureKey;
    private PublicationStatus status;
    private UUID currentPublishedVersionId;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<ContentPackageVersion> versions;
    private final UUID lessonId;
    private final UUID courseId;

    /** {@code lessonId} makes a practice set part of that lesson's Practice; other package types have none. */
    public ContentPackage(UUID id, String code, String title, PackageType packageType,
                          String requiredFeatureKey, PublicationStatus status,
                          UUID currentPublishedVersionId, Instant createdAt, Instant updatedAt,
                          List<ContentPackageVersion> versions, UUID lessonId) {
        this(id, code, title, packageType, requiredFeatureKey, status, currentPublishedVersionId, createdAt, updatedAt,
                versions, lessonId, null);
    }

    public ContentPackage(UUID id, String code, String title, PackageType packageType,
                          String requiredFeatureKey, PublicationStatus status,
                          UUID currentPublishedVersionId, Instant createdAt, Instant updatedAt,
                          List<ContentPackageVersion> versions, UUID lessonId, UUID courseId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.packageType = Objects.requireNonNull(packageType, "packageType must not be null");
        this.requiredFeatureKey = requiredFeatureKey;
        this.status = status != null ? status : PublicationStatus.DRAFT;
        this.currentPublishedVersionId = currentPublishedVersionId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.versions = versions != null ? new ArrayList<>(versions) : new ArrayList<>();
        if (lessonId != null && packageType != PackageType.PRACTICE_SET) {
            throw new InvalidPackageLessonException("Only a PRACTICE_SET package can belong to a lesson");
        }
        this.lessonId = lessonId;
        if (packageType == PackageType.COURSE_TEST && courseId == null) {
            throw new IllegalArgumentException("A COURSE_TEST package must belong to a course");
        }
        this.courseId = courseId;
    }

    public static ContentPackage create(String code, String title, PackageType packageType, String requiredFeatureKey) {
        return create(code, title, packageType, requiredFeatureKey, null);
    }

    public static ContentPackage create(String code, String title, PackageType packageType, String requiredFeatureKey,
                                        UUID lessonId) {
        Instant now = Instant.now();
        return new ContentPackage(UUID.randomUUID(), code, title, packageType, requiredFeatureKey,
                PublicationStatus.DRAFT, null, now, now, new ArrayList<>(), lessonId);
    }

    public void publishVersion(UUID versionId) {
        Objects.requireNonNull(versionId, "versionId must not be null");
        boolean exists = versions.stream().anyMatch(v -> v.getId().equals(versionId));
        if (!exists) {
            throw new IllegalArgumentException("Version does not belong to package: " + versionId);
        }
        this.currentPublishedVersionId = versionId;
        this.status = PublicationStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public void addVersion(ContentPackageVersion version) {
        this.versions.add(Objects.requireNonNull(version, "version must not be null"));
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getLessonId() { return lessonId; }
    public UUID getCourseId() { return courseId; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public PackageType getPackageType() { return packageType; }

    public String getRequiredFeatureKey() {
        return requiredFeatureKey;
    }
    public PublicationStatus getStatus() { return status; }
    public UUID getCurrentPublishedVersionId() { return currentPublishedVersionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ContentPackageVersion> getVersions() { return Collections.unmodifiableList(versions); }
}
