package com.group01.content.application.command;

import com.group01.content.domain.vo.BandRange;
import com.group01.content.domain.vo.Skill;
import java.util.UUID;

public record CreateTopicCommand(
        UUID parentTopicId,
        String code,
        String name,
        int sortOrder,
        BandRange band,
        Skill skill,
        UUID courseId
) {
    public CreateTopicCommand(UUID parentTopicId, String code, String name, int sortOrder, BandRange band, Skill skill) {
        this(parentTopicId, code, name, sortOrder, band, skill, null);
    }
}

