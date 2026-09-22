package com.group01.content.api.dto.response;

import com.group01.content.application.result.QuestionResult;
import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.Skill;

import java.time.Instant;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        QuestionType questionType,
        Skill skill,
        AccessLevel accessLevel,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        Instant createdAt,
        Instant updatedAt
) {
    public static QuestionResponse from(QuestionResult result) {
        return new QuestionResponse(
                result.id(),
                result.questionType(),
                result.skill(),
                result.accessLevel(),
                result.status(),
                result.currentPublishedVersionId(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}

