package com.group01.content.domain.aggregate;

import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.KnowledgePointKind;
import com.group01.content.domain.vo.Skill;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class KnowledgePoint {
    private final UUID id;
    private final UUID topicId;
    private String code;
    private String name;
    private KnowledgePointKind kind;
    private Skill skill;
    private String description;
    private ContentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public KnowledgePoint(UUID id, UUID topicId, String code, String name, KnowledgePointKind kind,
                          Skill skill, String description, ContentStatus status,
                          Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.topicId = Objects.requireNonNull(topicId, "topicId must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.skill = skill;
        this.description = description;
        this.status = status != null ? status : ContentStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static KnowledgePoint create(UUID topicId, String code, String name,
                                        KnowledgePointKind kind, Skill skill, String description) {
        Instant now = Instant.now();
        return new KnowledgePoint(UUID.randomUUID(), topicId, code, name, kind, skill, description,
                ContentStatus.ACTIVE, now, now);
    }

    public void update(String name, KnowledgePointKind kind, Skill skill, String description, ContentStatus status) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.skill = skill;
        this.description = description;
        if (status != null) {
            this.status = status;
        }
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTopicId() { return topicId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public KnowledgePointKind getKind() { return kind; }
    public Skill getSkill() { return skill; }
    public String getDescription() { return description; }
    public ContentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

