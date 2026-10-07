package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;

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
