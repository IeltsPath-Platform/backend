package com.group01.content.domain.aggregate;

import com.group01.content.domain.vo.BandRange;
import com.group01.content.domain.vo.ContentStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Topic {
    private final UUID id;
    private UUID parentTopicId;
    private String code;
    private String name;
    private int sortOrder;
    private ContentStatus status;
    private BandRange band;
    private final Instant createdAt;
    private Instant updatedAt;

    public Topic(UUID id, UUID parentTopicId, String code, String name, int sortOrder,
                 ContentStatus status, BandRange band, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.parentTopicId = parentTopicId;
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.code = code.trim();
        this.name = name.trim();
        this.sortOrder = sortOrder;
        this.status = status != null ? status : ContentStatus.ACTIVE;
        this.band = band != null ? band : BandRange.UNBOUNDED;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static Topic create(UUID parentTopicId, String code, String name, int sortOrder) {
        return create(parentTopicId, code, name, sortOrder, BandRange.UNBOUNDED);
    }

    public static Topic create(UUID parentTopicId, String code, String name, int sortOrder, BandRange band) {
        Instant now = Instant.now();
        return new Topic(UUID.randomUUID(), parentTopicId, code, name, sortOrder, ContentStatus.ACTIVE, band, now, now);
    }

    /** Replaces every editable field, band included: a null band clears it. */
    public void update(UUID parentTopicId, String name, int sortOrder, ContentStatus status, BandRange band) {
        this.parentTopicId = parentTopicId;
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.sortOrder = sortOrder;
        this.band = band != null ? band : BandRange.UNBOUNDED;
        if (status != null) {
            this.status = status;
        }
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getParentTopicId() { return parentTopicId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }
    public ContentStatus getStatus() { return status; }
    public BandRange getBand() { return band; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
