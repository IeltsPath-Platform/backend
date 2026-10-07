package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.ContentSectionResult;
import com.ieltspath.content.domain.vo.Skill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContentSectionResponse(
        UUID id,
        UUID packageVersionId,
        String title,
        Skill skill,
        int sortOrder,
        Integer timeLimitSeconds,
        String instructions,
        Instant createdAt,
        Instant updatedAt,
        List<SectionQuestionResponse> questions
) {
    public static ContentSectionResponse from(ContentSectionResult result) {
        List<SectionQuestionResponse> questionResponses = result.questions() != null
                ? result.questions().stream().map(SectionQuestionResponse::from).toList()
                : List.of();
        return new ContentSectionResponse(
                result.id(),
                result.packageVersionId(),
                result.title(),
                result.skill(),
                result.sortOrder(),
                result.timeLimitSeconds(),
                result.instructions(),
                result.createdAt(),
                result.updatedAt(),
                questionResponses
        );
    }
}

