package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.BandRange;
import com.ieltspath.content.domain.vo.Skill;
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

