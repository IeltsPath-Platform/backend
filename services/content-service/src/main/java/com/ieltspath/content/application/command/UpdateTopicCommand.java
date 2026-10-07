package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.BandRange;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.Skill;

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

