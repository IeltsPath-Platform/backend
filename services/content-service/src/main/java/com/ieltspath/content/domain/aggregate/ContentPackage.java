package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.exception.ContentSectionNotFoundException;
import com.ieltspath.content.domain.exception.DraftVersionExistsException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.InvalidPackageLessonException;
import com.ieltspath.content.domain.exception.PackageVersionNotFoundException;
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
    private String sourceCode;
    private PublicationStatus status;
    private UUID currentPublishedVersionId;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<ContentPackageVersion> versions;
    private final UUID lessonId;
    private final UUID courseId;
    private final UUID topicId;

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
        this(id, code, title, packageType, requiredFeatureKey, status, currentPublishedVersionId, createdAt, updatedAt,
                versions, lessonId, courseId, null);
    }

    public ContentPackage(UUID id, String code, String title, PackageType packageType,
                          String requiredFeatureKey, PublicationStatus status,
                          UUID currentPublishedVersionId, Instant createdAt, Instant updatedAt,
                          List<ContentPackageVersion> versions, UUID lessonId, UUID courseId, String sourceCode) {
        this(id, code, title, packageType, requiredFeatureKey, status, currentPublishedVersionId, createdAt, updatedAt,
                versions, lessonId, courseId, sourceCode, null);
    }

    public ContentPackage(UUID id, String code, String title, PackageType packageType,
                          String requiredFeatureKey, PublicationStatus status,
                          UUID currentPublishedVersionId, Instant createdAt, Instant updatedAt,
                          List<ContentPackageVersion> versions, UUID lessonId, UUID courseId, String sourceCode,
                          UUID topicId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.packageType = Objects.requireNonNull(packageType, "packageType must not be null");
        this.requiredFeatureKey = requiredFeatureKey;
        this.sourceCode = sourceCode;
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
        this.topicId = topicId;
    }

    public static ContentPackage create(String code, String title, PackageType packageType, String requiredFeatureKey) {
        return create(code, title, packageType, requiredFeatureKey, null, null);
    }

    public static ContentPackage create(String code, String title, PackageType packageType, String requiredFeatureKey,
                                        UUID lessonId) {
        return create(code, title, packageType, requiredFeatureKey, lessonId, null);
    }

    public static ContentPackage create(String code, String title, PackageType packageType, String requiredFeatureKey,
                                        UUID lessonId, String sourceCode) {
        Instant now = Instant.now();
        return new ContentPackage(UUID.randomUUID(), code, title, packageType, requiredFeatureKey,
                PublicationStatus.DRAFT, null, now, now, new ArrayList<>(), lessonId, null, sourceCode);
    }

    /** A final test is bound to its topic or course when created and stays bound. */
    public static ContentPackage createFinalTest(String code, String title, PackageType packageType,
                                                 String requiredFeatureKey, UUID topicId, UUID courseId) {
        if (packageType == PackageType.TOPIC_TEST && topicId == null) {
            throw new IllegalArgumentException("A TOPIC_TEST package must belong to a topic");
        }
        Instant now = Instant.now();
        return new ContentPackage(UUID.randomUUID(), code, title, packageType, requiredFeatureKey,
                PublicationStatus.DRAFT, null, now, now, new ArrayList<>(), null, courseId, null, topicId);
    }

    /** The title and who may open the package. The code, type and what it is bound to stay as created. */
    public void updateDetails(String title, String requiredFeatureKey) {
        requireNotArchived();
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.requiredFeatureKey = requiredFeatureKey;
        this.updatedAt = Instant.now();
    }

    /**
     * The author's soft delete. Nothing is removed, because attempts and history refer to the package, but learners
     * no longer see it and it can no longer be changed or published. Versions waiting for a scheduled publication
     * stop waiting.
     */
    public void archive() {
        requireNotArchived();
        versions.stream().filter(version -> version.getPublishAt() != null)
                .forEach(ContentPackageVersion::cancelScheduledPublication);
        this.status = PublicationStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    private void requireNotArchived() {
        if (status == PublicationStatus.ARCHIVED) {
            throw new InvalidContentStateException("The package is already deleted");
        }
    }

    public void publishVersion(UUID versionId) {
        Objects.requireNonNull(versionId, "versionId must not be null");
        requireNotArchived();
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

    /** Starts the next draft. A package has at most one draft; the number follows the highest existing version. */
    public ContentPackageVersion createDraftVersion(String rulesJson) {
        requireNotArchived();
        if (versions.stream().anyMatch(v -> v.getStatus() == PublicationStatus.DRAFT)) {
            throw new DraftVersionExistsException();
        }
        int next = versions.stream().mapToInt(ContentPackageVersion::getVersionNumber).max().orElse(0) + 1;
        ContentPackageVersion draft = ContentPackageVersion.create(id, next, rulesJson);
        addVersion(draft);
        return draft;
    }

    /**
     * A section named in a version's path must belong to that version. A section of another version of this package is
     * a bad request, while one that exists nowhere is not found.
     */
    public ContentSection requireSectionInVersion(UUID versionId, UUID sectionId) {
        ContentPackageVersion version = requireVersion(versionId);
        if (version.getSections().stream().anyMatch(s -> s.getId().equals(sectionId))) {
            return version.requireSection(sectionId);
        }
        boolean elsewhere = versions.stream()
                .anyMatch(v -> v.getSections().stream().anyMatch(s -> s.getId().equals(sectionId)));
        if (elsewhere) {
            throw new IllegalArgumentException("section " + sectionId + " does not belong to version " + versionId);
        }
        throw new ContentSectionNotFoundException(sectionId);
    }

    public ContentPackageVersion requireVersion(UUID versionId) {
        return versions.stream()
                .filter(version -> version.getId().equals(versionId))
                .findFirst()
                .orElseThrow(() -> new PackageVersionNotFoundException(versionId));
    }

    public UUID getId() { return id; }
    public UUID getLessonId() { return lessonId; }
    public UUID getCourseId() { return courseId; }
    public UUID getTopicId() { return topicId; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public PackageType getPackageType() { return packageType; }

    public String getRequiredFeatureKey() {
        return requiredFeatureKey;
    }
    public String getSourceCode() { return sourceCode; }
    public PublicationStatus getStatus() { return status; }
    public UUID getCurrentPublishedVersionId() { return currentPublishedVersionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ContentPackageVersion> getVersions() { return Collections.unmodifiableList(versions); }
}
