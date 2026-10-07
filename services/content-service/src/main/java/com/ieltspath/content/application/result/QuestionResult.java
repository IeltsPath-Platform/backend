package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;

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
