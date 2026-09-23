package com.group01.content.application.result;

import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.Skill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionDetailResult(
        UUID id,
        QuestionType questionType,
        Skill skill,
        AccessLevel accessLevel,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        Instant createdAt,
        Instant updatedAt,
        List<QuestionVersionResult> versions
) {}

