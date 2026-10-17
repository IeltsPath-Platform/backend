package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.Skill;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SuggestionCandidatesResult(List<PackageCandidate> packages, List<LessonCandidate> lessons) {
    public record PackageCandidate(UUID packageId, UUID packageVersionId, PackageType type, String title,
                                   List<UUID> knowledgePointIds, BigDecimal band, Skill skill) {}

    public record LessonCandidate(UUID lessonId, UUID topicId, String title, List<UUID> knowledgePointIds,
                                  BigDecimal band, Skill skill, int sortOrder) {}
}
