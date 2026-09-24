package com.group01.content.domain.aggregate;

import com.group01.content.domain.entity.QuestionVersion;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.Skill;

import java.time.Instant;
import java.util.*;

public class Question {

    private final UUID id;
    private QuestionType questionType;
    private Skill skill;
    private String requiredFeatureKey;
    private PublicationStatus status;
    private UUID currentPublishedVersionId;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<QuestionVersion> versions;

    public Question(UUID id, QuestionType questionType, Skill skill, String requiredFeatureKey,
            PublicationStatus status, UUID currentPublishedVersionId,
            Instant createdAt, Instant updatedAt, List<QuestionVersion> versions) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.questionType = Objects.requireNonNull(questionType, "questionType must not be null");
        this.skill = skill;
        this.requiredFeatureKey = requiredFeatureKey;
        this.status = status != null ? status : PublicationStatus.DRAFT;
        this.currentPublishedVersionId = currentPublishedVersionId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.versions = versions != null ? new ArrayList<>(versions) : new ArrayList<>();
    }

    public static Question create(QuestionType questionType, Skill skill, String requiredFeatureKey) {
        Instant now = Instant.now();
        return new Question(UUID.randomUUID(), questionType, skill, requiredFeatureKey,
                PublicationStatus.DRAFT, null, now, now, new ArrayList<>());
    }

    public void publishVersion(UUID versionId) {
        Objects.requireNonNull(versionId, "versionId must not be null");
        boolean exists = versions.stream().anyMatch(v -> v.getId().equals(versionId));
        if (!exists) {
            throw new IllegalArgumentException("Version does not belong to question: " + versionId);
        }
        this.currentPublishedVersionId = versionId;
        this.status = PublicationStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public void addVersion(QuestionVersion version) {
        this.versions.add(Objects.requireNonNull(version, "version must not be null"));
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.status = PublicationStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public Skill getSkill() {
        return skill;
    }

    public String getRequiredFeatureKey() {
        return requiredFeatureKey;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public UUID getCurrentPublishedVersionId() {
        return currentPublishedVersionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<QuestionVersion> getVersions() {
        return Collections.unmodifiableList(versions);
    }
}
