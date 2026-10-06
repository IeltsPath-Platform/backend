package com.group01.content.application.command;

import com.group01.content.domain.vo.BandRange;
import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.Skill;

import java.util.UUID;

public record UpdateTopicCommand(
        UUID id,
        UUID parentTopicId,
        String name,
        int sortOrder,
        ContentStatus status,
        BandRange band,
        Skill skill,
        UUID courseId
) {
    public UpdateTopicCommand(UUID id, UUID parentTopicId, String name, int sortOrder, ContentStatus status,
                              BandRange band, Skill skill) {
        this(id, parentTopicId, name, sortOrder, status, band, skill, null);
    }
}

