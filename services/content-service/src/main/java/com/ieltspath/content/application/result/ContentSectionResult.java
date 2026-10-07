package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.Skill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContentSectionResult(
        UUID id,
        UUID packageVersionId,
        String title,
        Skill skill,
        int sortOrder,
        Integer timeLimitSeconds,
        String instructions,
        Instant createdAt,
        Instant updatedAt,
        List<SectionQuestionResult> questions
) {}

