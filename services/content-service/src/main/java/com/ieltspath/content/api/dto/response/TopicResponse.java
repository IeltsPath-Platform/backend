package com.ieltspath.content.api.dto.response;

import java.math.BigDecimal;
import com.ieltspath.content.application.result.TopicResult;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.Skill;

import java.time.Instant;
import java.util.UUID;

public record TopicResponse(
        UUID id,
        UUID parentTopicId,
        String code,
        String name,
        int sortOrder,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt,
        BigDecimal bandMin,
        BigDecimal bandMax,
        Skill skill,
        UUID courseId
) {
    public static TopicResponse from(TopicResult result) {
        return new TopicResponse(
                result.id(),
                result.parentTopicId(),
                result.code(),
                result.name(),
                result.sortOrder(),
                result.status(),
                result.createdAt(),
                result.updatedAt(),
                result.band().min(),
                result.band().max(),
                result.skill(),
                result.courseId()
        );
    }
}

