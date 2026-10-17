package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.Skill;

import java.util.List;
import java.util.UUID;

/** Learner-safe package data plus a separate answer block for author review. */
public record PackageVersionPreviewResult(UUID packageVersionId, UUID packageId, PackageType packageType,
                                          UUID topicId, String rulesJson, List<Section> sections,
                                          List<SectionAnswers> answers) {
    public record Section(UUID sectionId, String title, Skill skill, String instructions, int sortOrder,
                          String passage, Audio audio, List<Item> items) {}

    public record Audio(UUID assetId, String mediaUrl, Integer durationSeconds) {}

    public record Item(UUID questionVersionId, int sortOrder, String stem, String optionsJson,
                       String hint, double maxScore,
                       List<PackageVersionContentResult.KnowledgePointMapping> knowledgePointMappings) {}

    public record SectionAnswers(UUID sectionId, String audioTranscript, List<ItemAnswer> items) {}

    public record ItemAnswer(UUID questionVersionId, String answerSpecJson, String explanation, String hint) {}
}
