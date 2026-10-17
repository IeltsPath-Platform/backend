package com.ieltspath.content.api.dto.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.ieltspath.content.application.result.PackageVersionPreviewResult;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.Skill;

import java.util.List;
import java.util.UUID;

public record PackageVersionPreviewResponse(UUID packageVersionId, UUID packageId, PackageType packageType,
                                            UUID topicId, @JsonRawValue String rules, List<Section> sections,
                                            List<SectionAnswers> answers) {
    public record Section(UUID sectionId, String title, Skill skill, String instructions, int sortOrder,
                          String passage, Audio audio, List<Item> items) {}

    public record Audio(UUID assetId, String mediaUrl, Integer durationSeconds) {}

    public record Item(UUID questionVersionId, int sortOrder, String stem, @JsonRawValue String options,
                       String hint, double maxScore,
                       List<KnowledgePointMapping> knowledgePointMappings) {}

    public record KnowledgePointMapping(UUID knowledgePointId, double weight) {}

    public record SectionAnswers(UUID sectionId, String audioTranscript, List<ItemAnswer> items) {}

    public record ItemAnswer(UUID questionVersionId, @JsonRawValue String answerSpec,
                             String explanation, String hint) {}

    public static PackageVersionPreviewResponse from(PackageVersionPreviewResult result) {
        return new PackageVersionPreviewResponse(result.packageVersionId(), result.packageId(), result.packageType(),
                result.topicId(), result.rulesJson(), result.sections().stream()
                .map(section -> new Section(section.sectionId(), section.title(), section.skill(),
                        section.instructions(), section.sortOrder(), section.passage(),
                        section.audio() == null ? null : new Audio(section.audio().assetId(),
                                section.audio().mediaUrl(), section.audio().durationSeconds()),
                        section.items().stream().map(item -> new Item(item.questionVersionId(), item.sortOrder(),
                                item.stem(), item.optionsJson(), item.hint(), item.maxScore(),
                                item.knowledgePointMappings().stream().map(mapping -> new KnowledgePointMapping(
                                        mapping.knowledgePointId(), mapping.weight())).toList())).toList()))
                .toList(), result.answers().stream()
                .map(section -> new SectionAnswers(section.sectionId(), section.audioTranscript(),
                        section.items().stream().map(answer -> new ItemAnswer(answer.questionVersionId(),
                                answer.answerSpecJson(), answer.explanation(), answer.hint())).toList()))
                .toList());
    }
}
