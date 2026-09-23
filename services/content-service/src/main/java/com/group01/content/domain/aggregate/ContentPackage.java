package com.group01.content.domain.aggregate;

import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.PublicationStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ContentPackage {
    private final UUID id;
    private String code;
    private String title;
    private PackageType packageType;
    private AccessLevel accessLevel;
    private PublicationStatus status;
    private UUID currentPublishedVersionId;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<ContentPackageVersion> versions;

    public ContentPackage(UUID id, String code, String title, PackageType packageType,
                          AccessLevel accessLevel, PublicationStatus status,
                          UUID currentPublishedVersionId, Instant createdAt, Instant updatedAt,
                          List<ContentPackageVersion> versions) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.packageType = Objects.requireNonNull(packageType, "packageType must not be null");
        this.accessLevel = accessLevel != null ? accessLevel : AccessLevel.FREE;
        this.status = status != null ? status : PublicationStatus.DRAFT;
        this.currentPublishedVersionId = currentPublishedVersionId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.versions = versions != null ? new ArrayList<>(versions) : new ArrayList<>();
    }

    public static ContentPackage create(String code, String title, PackageType packageType, AccessLevel accessLevel) {
        Instant now = Instant.now();
        return new ContentPackage(UUID.randomUUID(), code, title, packageType, accessLevel,
                PublicationStatus.DRAFT, null, now, now, new ArrayList<>());
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
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public PackageType getPackageType() { return packageType; }
    public AccessLevel getAccessLevel() { return accessLevel; }
    public PublicationStatus getStatus() { return status; }
    public UUID getCurrentPublishedVersionId() { return currentPublishedVersionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ContentPackageVersion> getVersions() { return Collections.unmodifiableList(versions); }
}
