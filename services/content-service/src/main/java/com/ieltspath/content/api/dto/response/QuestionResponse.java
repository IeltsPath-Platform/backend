package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.api.AccessLevelCompatibility;
import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.application.result.QuestionPreviewKnowledgePointResult;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

public record QuestionResponse(
        UUID id,
        QuestionType questionType,
        Skill skill,
        AccessLevel accessLevel,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        Instant createdAt,
        Instant updatedAt,
        QuestionPurpose purpose,
        String importRef,
        String code,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean ownedElsewhere,
        @JsonInclude(JsonInclude.Include.NON_NULL) String stemExcerpt,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<QuestionPreviewKnowledgePointResult> knowledgePoints,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer knowledgePointTotal
) {
    public static QuestionResponse from(QuestionResult result) {
        return new QuestionResponse(
                result.id(),
                result.questionType(),
                result.skill(),
                AccessLevelCompatibility.toAccessLevel(result.requiredFeatureKey()),
                result.status(),
                result.currentPublishedVersionId(),
                result.createdAt(),
                result.updatedAt(),
                result.purpose(),
                result.importRef(),
                result.code(),
                result.ownedElsewhere(),
                result.stemExcerpt(),
                result.knowledgePoints(),
                result.knowledgePointTotal()
        );
    }
}
