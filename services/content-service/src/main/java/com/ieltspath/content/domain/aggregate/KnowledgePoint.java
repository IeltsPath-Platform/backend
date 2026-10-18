package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class KnowledgePoint {
    private final UUID id;
    private UUID topicId;
    private String code;
    private String name;
    private KnowledgePointKind kind;
    private LearningType learningType;
    private Skill skill;
    private String description;
    private CefrLevel cefrLevel;
    private String canDo;
    private String scopeIn;
    private String scopeOut;
    private ContentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public KnowledgePoint(UUID id, UUID topicId, String code, String name, KnowledgePointKind kind,
                          LearningType learningType,
                          Skill skill, String description, ContentStatus status,
                          Instant createdAt, Instant updatedAt) {
        this(id, topicId, code, name, kind, learningType, skill, description, null, null, null, null, status,
                createdAt, updatedAt);
    }

    public KnowledgePoint(UUID id, UUID topicId, String code, String name, KnowledgePointKind kind,
                          LearningType learningType, Skill skill, String description, CefrLevel cefrLevel,
                          String canDo, String scopeIn, String scopeOut, ContentStatus status,
                          Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.topicId = Objects.requireNonNull(topicId, "topicId must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.learningType = learningType;
        this.skill = skill;
        this.description = description;
        this.cefrLevel = cefrLevel;
        this.canDo = canDo;
        this.scopeIn = scopeIn;
        this.scopeOut = scopeOut;
        this.status = status != null ? status : ContentStatus.ACTIVE;
        if (this.status == ContentStatus.ACTIVE && this.learningType == null) {
            throw new IllegalArgumentException("learningType is required for an active knowledge point");
        }
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static KnowledgePoint create(UUID topicId, String code, String name,
                                        KnowledgePointKind kind, LearningType learningType,
                                        Skill skill, String description) {
        return create(topicId, code, name, kind, learningType, skill, description, null, null, null, null);
    }

    public static KnowledgePoint create(UUID topicId, String code, String name,
                                        KnowledgePointKind kind, LearningType learningType, Skill skill,
                                        String description, CefrLevel cefrLevel, String canDo,
                                        String scopeIn, String scopeOut) {
        Instant now = Instant.now();
        return new KnowledgePoint(UUID.randomUUID(), topicId, code, name, kind, learningType, skill, description,
                cefrLevel, canDo, scopeIn, scopeOut, ContentStatus.ACTIVE, now, now);
    }

    public void update(UUID topicId, String code, String name, KnowledgePointKind kind, LearningType learningType,
                       Skill skill, String description) {
        update(topicId, code, name, kind, learningType, skill, description, null, null, null, null);
    }

    public void update(UUID topicId, String code, String name, KnowledgePointKind kind, LearningType learningType,
                       Skill skill, String description, CefrLevel cefrLevel, String canDo,
                       String scopeIn, String scopeOut) {
        Objects.requireNonNull(topicId, "topicId must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        if (this.status == ContentStatus.ACTIVE && learningType == null) {
            throw new IllegalArgumentException("learningType is required for an active knowledge point");
        }
        this.topicId = topicId;
        this.code = code;
        this.name = name;
        this.kind = kind;
        this.learningType = learningType;
        this.skill = skill;
        this.description = description;
        this.cefrLevel = cefrLevel;
        this.canDo = canDo;
        this.scopeIn = scopeIn;
        this.scopeOut = scopeOut;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        if (this.status != ContentStatus.INACTIVE) {
            this.status = ContentStatus.INACTIVE;
            this.updatedAt = Instant.now();
        }
    }

    public void activate() {
        if (this.learningType == null) {
            throw new IllegalArgumentException("learningType is required for an active knowledge point");
        }
        if (this.status != ContentStatus.ACTIVE) {
            this.status = ContentStatus.ACTIVE;
            this.updatedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getTopicId() { return topicId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public KnowledgePointKind getKind() { return kind; }
    public LearningType getLearningType() { return learningType; }
    public Skill getSkill() { return skill; }
    public String getDescription() { return description; }
    public CefrLevel getCefrLevel() { return cefrLevel; }
    public String getCanDo() { return canDo; }
    public String getScopeIn() { return scopeIn; }
    public String getScopeOut() { return scopeOut; }
    public ContentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
