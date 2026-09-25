package com.group01.content.api.dto.response;

import java.math.BigDecimal;
import com.group01.content.application.result.TopicTreeResult;
import com.group01.content.domain.vo.ContentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TopicTreeResponse(
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
        List<TopicTreeResponse> children
) {
    public static TopicTreeResponse from(TopicTreeResult result) {
        List<TopicTreeResponse> childResponses = result.children() != null
                ? result.children().stream().map(TopicTreeResponse::from).toList()
                : List.of();
        return new TopicTreeResponse(
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
                childResponses
        );
    }
}

