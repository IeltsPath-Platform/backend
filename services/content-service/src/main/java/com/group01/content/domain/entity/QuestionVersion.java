package com.group01.content.domain.entity;

import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.QuestionDifficulty;
import com.group01.content.domain.vo.QuestionOptionPayload;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class QuestionVersion {
    private final UUID id;
    private final UUID questionId;
    private final int versionNumber;
    private String stem;
    private List<QuestionOptionPayload> options;
    private String answerSpecJson;
    private int schemaVersion;
    private String explanation;
    private QuestionDifficulty difficulty;
    private PublicationStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<QuestionKnowledgePoint> knowledgePoints;

    public QuestionVersion(UUID id, UUID questionId, int versionNumber, String stem,
                           List<QuestionOptionPayload> options, String answerSpecJson,
                           int schemaVersion, String explanation, QuestionDifficulty difficulty,
                           PublicationStatus status, Instant createdAt, Instant updatedAt,
                           List<QuestionKnowledgePoint> knowledgePoints) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.questionId = Objects.requireNonNull(questionId, "questionId must not be null");
        this.versionNumber = versionNumber;
        this.stem = Objects.requireNonNull(stem, "stem must not be null");
        this.options = options != null ? new ArrayList<>(options) : new ArrayList<>();
        this.answerSpecJson = answerSpecJson != null ? answerSpecJson : "{}";
        this.schemaVersion = schemaVersion > 0 ? schemaVersion : 1;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.status = status != null ? status : PublicationStatus.DRAFT;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.knowledgePoints = knowledgePoints != null ? new ArrayList<>(knowledgePoints) : new ArrayList<>();
    }

    public static QuestionVersion create(UUID questionId, int versionNumber, String stem,
                                         List<QuestionOptionPayload> options, String answerSpecJson,
                                         String explanation, QuestionDifficulty difficulty) {
        Instant now = Instant.now();
        return new QuestionVersion(UUID.randomUUID(), questionId, versionNumber, stem, options,
                answerSpecJson, 1, explanation, difficulty, PublicationStatus.DRAFT, now, now, new ArrayList<>());
    }

    public void addKnowledgePoint(QuestionKnowledgePoint kp) {
        this.knowledgePoints.add(Objects.requireNonNull(kp, "kp must not be null"));
        this.updatedAt = Instant.now();
    }

    public void publish() {
        this.status = PublicationStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getQuestionId() { return questionId; }
    public int getVersionNumber() { return versionNumber; }
    public String getStem() { return stem; }
    public List<QuestionOptionPayload> getOptions() { return Collections.unmodifiableList(options); }
    public String getAnswerSpecJson() { return answerSpecJson; }
    public int getSchemaVersion() { return schemaVersion; }
    public String getExplanation() { return explanation; }
    public QuestionDifficulty getDifficulty() { return difficulty; }
    public PublicationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<QuestionKnowledgePoint> getKnowledgePoints() { return Collections.unmodifiableList(knowledgePoints); }
}

