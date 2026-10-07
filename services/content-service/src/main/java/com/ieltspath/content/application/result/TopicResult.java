package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.BandRange;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.Skill;

import java.time.Instant;
import java.util.UUID;

public record TopicResult(
        UUID id,
        UUID parentTopicId,
        String code,
        String name,
        int sortOrder,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt,
        BandRange band,
        Skill skill,
        UUID courseId
) {
    public TopicResult(UUID id, UUID parentTopicId, String code, String name, int sortOrder, ContentStatus status,
                       Instant createdAt, Instant updatedAt, BandRange band, Skill skill) {
        this(id, parentTopicId, code, name, sortOrder, status, createdAt, updatedAt, band, skill, null);
    }
}

