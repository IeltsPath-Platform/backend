package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.Skill;

import java.util.UUID;

/** Fields an author may set on a section. Used for both create and update. */
public record SaveContentSectionCommand(
        UUID packageId,
        UUID versionId,
        UUID sectionId,
        String title,
        Skill skill,
        int sortOrder,
        Integer timeLimitSeconds,
        String instructions,
        Integer partNo,
        UUID coverAssetId
) {
}
