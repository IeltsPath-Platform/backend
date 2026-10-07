package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.Skill;

import java.util.UUID;

public record AddContentSectionCommand(
        UUID packageVersionId,
        String title,
        Skill skill,
        int sortOrder,
        Integer timeLimitSeconds,
        String instructions
) {}

