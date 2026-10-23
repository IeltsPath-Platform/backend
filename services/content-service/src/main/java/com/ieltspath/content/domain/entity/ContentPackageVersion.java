package com.ieltspath.content.domain.entity;

import com.ieltspath.content.domain.exception.ContentSectionNotFoundException;
import com.ieltspath.content.domain.exception.VersionNotDraftException;
import com.ieltspath.content.domain.vo.PublicationStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ContentPackageVersion {
    private final UUID id;
    private final UUID packageId;
    private final int versionNumber;
    private PublicationStatus status;
    private String rulesJson;
    private int schemaVersion;
    private Instant publishedAt;
    private UUID publishedBy;
    private Instant publishAt;
    private String lastPublishError;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<ContentSection> sections;

    public ContentPackageVersion(UUID id, UUID packageId, int versionNumber,
                                 PublicationStatus status, String rulesJson, int schemaVersion,
                                 Instant publishedAt, UUID publishedBy,
                                 Instant createdAt, Instant updatedAt, List<ContentSection> sections) {
        this(id, packageId, versionNumber, status, rulesJson, schemaVersion, publishedAt, publishedBy,
                null, null, createdAt, updatedAt, sections);
    }

    public ContentPackageVersion(UUID id, UUID packageId, int versionNumber,
                                 PublicationStatus status, String rulesJson, int schemaVersion,
                                 Instant publishedAt, UUID publishedBy, Instant publishAt, String lastPublishError,
                                 Instant createdAt, Instant updatedAt, List<ContentSection> sections) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.packageId = Objects.requireNonNull(packageId, "packageId must not be null");
        this.versionNumber = versionNumber;
        this.status = status != null ? status : PublicationStatus.DRAFT;
        this.rulesJson = rulesJson != null ? rulesJson : "{}";
        this.schemaVersion = schemaVersion > 0 ? schemaVersion : 1;
        this.publishedAt = publishedAt;
        this.publishedBy = publishedBy;
        this.publishAt = publishAt;
        this.lastPublishError = lastPublishError;
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
        this.publishAt = null;
        this.lastPublishError = null;
        this.updatedAt = this.publishedAt;
    }

    public void schedulePublication(Instant publishAt) {
        this.publishAt = Objects.requireNonNull(publishAt, "publishAt must not be null");
        this.lastPublishError = null;
        this.updatedAt = Instant.now();
    }

    public void cancelScheduledPublication() {
        this.publishAt = null;
        this.lastPublishError = null;
        this.updatedAt = Instant.now();
    }

    /** Keeps the due time, so the job tries again: for a failure that may pass by itself. */
    public void recordScheduledPublicationError(String errorCode) {
        this.lastPublishError = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.updatedAt = Instant.now();
    }

    /**
     * Drops the due time and keeps the reason: the package breaks a publishing rule, and the same attempt would fail
     * again until an author changes it, possibly weeks later and out of the blue. The author reschedules after fixing.
     */
    public void rejectScheduledPublication(String errorCode) {
        this.publishAt = null;
        this.lastPublishError = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.updatedAt = Instant.now();
    }

    /** Every change to a version's content goes through here: a published version is what learners are served. */
    public void requireDraft() {
        if (status != PublicationStatus.DRAFT) {
            throw new VersionNotDraftException();
        }
    }

    /** A sort order is used by one section at a time in a version; the section being changed may keep its own. */
    public void requireFreeSortOrder(int sortOrder, UUID exceptSectionId) {
        boolean taken = sections.stream()
                .anyMatch(s -> s.getSortOrder() == sortOrder && !s.getId().equals(exceptSectionId));
        if (taken) {
            throw new IllegalArgumentException("sortOrder " + sortOrder + " is already used in this version");
        }
    }

    /** A question version may appear once in this version, in any of its sections. */
    public void requireQuestionNotInVersion(UUID questionVersionId) {
        boolean used = sections.stream()
                .flatMap(section -> section.getQuestions().stream())
                .anyMatch(question -> question.getQuestionVersionId().equals(questionVersionId));
        if (used) {
            throw new com.ieltspath.content.domain.exception.QuestionAlreadyInVersionException(questionVersionId);
        }
    }

    public void addSection(ContentSection section) {
        requireDraft();
        requireFreeSortOrder(section.getSortOrder(), null);
        this.sections.add(Objects.requireNonNull(section, "section must not be null"));
        this.updatedAt = Instant.now();
    }

    public ContentSection requireSection(UUID sectionId) {
        return sections.stream()
                .filter(section -> section.getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new ContentSectionNotFoundException(sectionId));
    }

    /** Removes a section of this draft; its questions go with it, since the aggregate owns them. */
    public void removeSection(UUID sectionId) {
        requireDraft();
        sections.remove(requireSection(sectionId));
        this.updatedAt = Instant.now();
    }

    /**
     * Gives the sections the contiguous orders 1..n in the given sequence. The ids must be exactly this version's
     * sections, each once, so no section is left out or counted twice.
     */
    public void reorderSections(List<UUID> orderedIds) {
        requireDraft();
        Set<UUID> current = sections.stream().map(ContentSection::getId).collect(Collectors.toSet());
        if (orderedIds == null || orderedIds.size() != current.size() || !current.equals(new HashSet<>(orderedIds))) {
            throw new IllegalArgumentException("sectionIds must list every section of the version exactly once");
        }
        int order = 1;
        for (UUID id : orderedIds) {
            requireSection(id).changeSortOrder(order++);
        }
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
    public Instant getPublishAt() { return publishAt; }
    public String getLastPublishError() { return lastPublishError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ContentSection> getSections() { return Collections.unmodifiableList(sections); }
}

