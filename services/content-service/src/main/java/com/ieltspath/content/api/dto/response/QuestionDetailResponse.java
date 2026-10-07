package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.api.AccessLevelCompatibility;
import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.application.result.QuestionDetailResult;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionDetailResponse(
        UUID id,
        QuestionType questionType,
        Skill skill,
        AccessLevel accessLevel,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        Instant createdAt,
        Instant updatedAt,
        List<QuestionVersionResponse> versions,
        QuestionPurpose purpose
) {
    public static QuestionDetailResponse from(QuestionDetailResult result) {
        List<QuestionVersionResponse> versionResponses = result.versions() != null
                ? result.versions().stream().map(QuestionVersionResponse::from).toList()
                : List.of();
        return new QuestionDetailResponse(
                result.id(),
                result.questionType(),
                result.skill(),
                AccessLevelCompatibility.toAccessLevel(result.requiredFeatureKey()),
                result.status(),
                result.currentPublishedVersionId(),
                result.createdAt(),
                result.updatedAt(),
                versionResponses,
                result.purpose()
        );
    }
}
