package com.group01.content.application.result;

import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.QuestionPurpose;
import com.group01.content.domain.vo.Skill;

import java.time.Instant;
import java.util.UUID;

public record QuestionResult(
        UUID id,
        QuestionType questionType,
        Skill skill,
        String requiredFeatureKey,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        Instant createdAt,
        Instant updatedAt,
        QuestionPurpose purpose
) {
    public QuestionResult(UUID id, QuestionType questionType, Skill skill, String requiredFeatureKey,
                          PublicationStatus status, UUID currentPublishedVersionId, Instant createdAt,
                          Instant updatedAt) {
        this(id, questionType, skill, requiredFeatureKey, status, currentPublishedVersionId, createdAt, updatedAt,
                QuestionPurpose.LEARNING);
    }
}
