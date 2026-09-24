package com.group01.game.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "game_answers")
public class GameAnswerJpaEntity {
    @Id
    private UUID id;
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;
    @Column(name = "item_sequence", nullable = false)
    private int itemSequence;
    @Column(name = "vocabulary_sense_id")
    private UUID vocabularySenseId;
    @Column(name = "question_version_id")
    private UUID questionVersionId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "item_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> itemSnapshot;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> responsePayload;
    @Column(name = "is_correct", nullable = false)
    private boolean correct;
    @Column(name = "duration_milliseconds", nullable = false)
    private long durationMilliseconds;

    protected GameAnswerJpaEntity() {
    }

    public GameAnswerJpaEntity(UUID id, UUID sessionId, int itemSequence, UUID vocabularySenseId,
                               UUID questionVersionId, Map<String, Object> itemSnapshot,
                               Map<String, Object> responsePayload, boolean correct, long durationMilliseconds) {
        this.id = id;
        this.sessionId = sessionId;
        this.itemSequence = itemSequence;
        this.vocabularySenseId = vocabularySenseId;
        this.questionVersionId = questionVersionId;
        this.itemSnapshot = itemSnapshot;
        this.responsePayload = responsePayload;
        this.correct = correct;
        this.durationMilliseconds = durationMilliseconds;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public int getItemSequence() {
        return itemSequence;
    }

    public UUID getVocabularySenseId() {
        return vocabularySenseId;
    }

    public UUID getQuestionVersionId() {
        return questionVersionId;
    }

    public Map<String, Object> getItemSnapshot() {
        return itemSnapshot;
    }

    public Map<String, Object> getResponsePayload() {
        return responsePayload;
    }

    public boolean isCorrect() {
        return correct;
    }

    public long getDurationMilliseconds() {
        return durationMilliseconds;
    }
}
