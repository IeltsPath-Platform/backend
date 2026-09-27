package com.group01.content.application.result;

import com.group01.content.domain.vo.BandRange;
import com.group01.content.domain.vo.ContentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TopicTreeResult(
        UUID id,
        UUID parentTopicId,
        String code,
        String name,
        int sortOrder,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt,
        BandRange band,
        List<TopicTreeResult> children
) {}

