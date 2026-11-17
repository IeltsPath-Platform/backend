package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.Skill;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SuggestionCandidatesResult(List<PackageCandidate> packages, List<LessonCandidate> lessons) {
    public record PackageCandidate(UUID packageId, UUID packageVersionId, PackageType type, String title,
                                   List<UUID> knowledgePointIds, BigDecimal band, Skill skill) {}

    /**
     * {@code courseId} and {@code skillPosition} let Learning apply its paywall rule to the lesson without another call;
     * {@code skillPosition} is the same ranking the lesson summary reports. {@code skills} is the set the reading gate
     * uses (exercise questions and text-block points), which can differ from the single {@code skill} of the lesson's points.
     */
    public record LessonCandidate(UUID lessonId, UUID topicId, String title, List<UUID> knowledgePointIds,
                                  BigDecimal band, Skill skill, int sortOrder, UUID courseId, Integer skillPosition,
                                  List<Skill> skills) {}
}
