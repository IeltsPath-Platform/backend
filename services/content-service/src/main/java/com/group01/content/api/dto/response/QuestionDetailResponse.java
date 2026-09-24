package com.group01.content.api.dto.response;

import com.group01.content.api.AccessLevelCompatibility;
import com.group01.content.api.dto.AccessLevel;
import com.group01.content.application.result.QuestionDetailResult;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.Skill;

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
        List<QuestionVersionResponse> versions
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
                versionResponses
        );
    }
}
