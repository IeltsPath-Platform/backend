package com.group01.content.application.result;

import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.KnowledgePointKind;
import com.group01.content.domain.vo.Skill;

import java.time.Instant;
import java.util.UUID;

public record KnowledgePointResult(
        UUID id,
        UUID topicId,
        String code,
        String name,
        KnowledgePointKind kind,
        Skill skill,
        String description,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt
) {}

