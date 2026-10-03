package com.group01.content.application.result;

import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.QuestionPurpose;
import com.group01.content.domain.vo.Skill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionDetailResult(
        UUID id,
        QuestionType questionType,
        Skill skill,
        String requiredFeatureKey,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        Instant createdAt,
        Instant updatedAt,
        List<QuestionVersionResult> versions,
        QuestionPurpose purpose
) {
    public QuestionDetailResult(UUID id, QuestionType questionType, Skill skill, String requiredFeatureKey,
                                PublicationStatus status, UUID currentPublishedVersionId, Instant createdAt,
                                Instant updatedAt, List<QuestionVersionResult> versions) {
        this(id, questionType, skill, requiredFeatureKey, status, currentPublishedVersionId, createdAt, updatedAt,
                versions, QuestionPurpose.LEARNING);
    }
}
