package com.group01.content.application.command;

import com.group01.content.domain.vo.Skill;

import java.util.UUID;

public record AddContentSectionCommand(
        UUID packageVersionId,
        String title,
        Skill skill,
        int sortOrder,
        Integer timeLimitSeconds,
        String instructions
) {}

