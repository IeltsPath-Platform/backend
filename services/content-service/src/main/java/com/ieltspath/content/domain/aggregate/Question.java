package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.exception.QuestionVersionNotFoundException;

import java.time.Instant;
import java.util.*;

public class Question {

    private final UUID id;
    private final QuestionPurpose purpose;
    private QuestionType questionType;
    private Skill skill;
    private String requiredFeatureKey;
    private PublicationStatus status;
    private UUID currentPublishedVersionId;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<QuestionVersion> versions;
    /** Stable key that Excel rows and drafts use to refer to this question; never shown to learners. */
    private final String importRef;

    private static final java.util.regex.Pattern IMPORT_REF = java.util.regex.Pattern.compile("[A-Za-z0-9._-]{1,100}");

    public Question(UUID id, QuestionType questionType, Skill skill, String requiredFeatureKey,
            PublicationStatus status, UUID currentPublishedVersionId,
            Instant createdAt, Instant updatedAt, List<QuestionVersion> versions, QuestionPurpose purpose) {
        this(id, questionType, skill, requiredFeatureKey, status, currentPublishedVersionId, createdAt, updatedAt,
                versions, purpose, null);
    }

    public Question(UUID id, QuestionType questionType, Skill skill, String requiredFeatureKey,
            PublicationStatus status, UUID currentPublishedVersionId,
            Instant createdAt, Instant updatedAt, List<QuestionVersion> versions, QuestionPurpose purpose,
            String importRef) {
        this.importRef = importRef;
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.purpose = purpose != null ? purpose : QuestionPurpose.LEARNING;
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
        return create(questionType, skill, requiredFeatureKey, QuestionPurpose.LEARNING);
    }

    public static Question create(QuestionType questionType, Skill skill, String requiredFeatureKey,
                                  QuestionPurpose purpose) {
        return create(questionType, skill, requiredFeatureKey, purpose, null);
    }

    public static Question create(QuestionType questionType, Skill skill, String requiredFeatureKey,
                                  QuestionPurpose purpose, String importRef) {
        Instant now = Instant.now();
        return new Question(UUID.randomUUID(), questionType, skill, requiredFeatureKey,
                PublicationStatus.DRAFT, null, now, now, new ArrayList<>(), purpose, normalizeImportRef(importRef));
    }

    /**
     * Trims an import reference and checks its shape. Blank means none. Only letters, digits, dot, underscore and dash
     * are allowed, up to 100 characters, so references can be typed in a spreadsheet and used in URLs safely.
     */
    public static String normalizeImportRef(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (!IMPORT_REF.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "importRef may use letters, digits, dot, underscore and dash, up to 100 characters");
        }
        return trimmed;
    }

    public String getImportRef() { return importRef; }

    public void publishVersion(UUID versionId) {
        Objects.requireNonNull(versionId, "versionId must not be null");
        QuestionVersion version = version(versionId);
        version.publish();
        this.currentPublishedVersionId = versionId;
        this.status = PublicationStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public void archiveVersion(UUID versionId) {
        QuestionVersion target = version(versionId);
        target.archive();
        if (Objects.equals(currentPublishedVersionId, versionId)) {
            currentPublishedVersionId = versions.stream()
                    .filter(v -> v.getStatus() == PublicationStatus.PUBLISHED)
                    .max(Comparator.comparing(QuestionVersion::getUpdatedAt))
                    .map(QuestionVersion::getId)
                    .orElse(null);
        }
        status = currentPublishedVersionId == null ? PublicationStatus.DRAFT : PublicationStatus.PUBLISHED;
        updatedAt = Instant.now();
    }

    private QuestionVersion version(UUID versionId) {
        return versions.stream().filter(v -> v.getId().equals(versionId)).findFirst()
                .orElseThrow(() -> new QuestionVersionNotFoundException(versionId));
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

    public QuestionPurpose getPurpose() {
        return purpose;
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
