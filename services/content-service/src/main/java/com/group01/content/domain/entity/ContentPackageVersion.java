package com.group01.content.domain.entity;

import com.group01.content.domain.vo.PublicationStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ContentPackageVersion {
    private final UUID id;
    private final UUID packageId;
    private final int versionNumber;
    private PublicationStatus status;
    private String rulesJson;
    private int schemaVersion;
    private Instant publishedAt;
    private UUID publishedBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<ContentSection> sections;

    public ContentPackageVersion(UUID id, UUID packageId, int versionNumber,
                                 PublicationStatus status, String rulesJson, int schemaVersion,
                                 Instant publishedAt, UUID publishedBy,
                                 Instant createdAt, Instant updatedAt, List<ContentSection> sections) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.packageId = Objects.requireNonNull(packageId, "packageId must not be null");
        this.versionNumber = versionNumber;
        this.status = status != null ? status : PublicationStatus.DRAFT;
        this.rulesJson = rulesJson != null ? rulesJson : "{}";
        this.schemaVersion = schemaVersion > 0 ? schemaVersion : 1;
        this.publishedAt = publishedAt;
        this.publishedBy = publishedBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.sections = sections != null ? new ArrayList<>(sections) : new ArrayList<>();
    }

    public static ContentPackageVersion create(UUID packageId, int versionNumber, String rulesJson) {
        Instant now = Instant.now();
        return new ContentPackageVersion(UUID.randomUUID(), packageId, versionNumber,
                PublicationStatus.DRAFT, rulesJson, 1, null, null, now, now, new ArrayList<>());
    }

    public void publish(UUID publishedBy) {
        this.status = PublicationStatus.PUBLISHED;
        this.publishedBy = publishedBy;
        this.publishedAt = Instant.now();
        this.updatedAt = this.publishedAt;
    }

    public void addSection(ContentSection section) {
        this.sections.add(Objects.requireNonNull(section, "section must not be null"));
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPackageId() { return packageId; }
    public int getVersionNumber() { return versionNumber; }
    public PublicationStatus getStatus() { return status; }
    public String getRulesJson() { return rulesJson; }
    public int getSchemaVersion() { return schemaVersion; }
    public Instant getPublishedAt() { return publishedAt; }
    public UUID getPublishedBy() { return publishedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ContentSection> getSections() { return Collections.unmodifiableList(sections); }
}

