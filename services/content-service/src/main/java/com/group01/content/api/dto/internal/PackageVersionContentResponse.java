package com.group01.content.api.dto.internal;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.group01.content.application.result.PackageVersionContentResult;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.Skill;

import java.util.List;
import java.util.UUID;

/** Internal package payload; it carries answer specs and must never be proxied to a learner unchanged. */
public record PackageVersionContentResponse(
        UUID packageVersionId,
        UUID packageId,
        PackageType packageType,
        UUID topicId,
        @JsonRawValue String rules,
        List<Section> sections
) {
    public record Section(UUID sectionId, String title, Skill skill, String instructions, int sortOrder,
                          String passage, List<Item> items) {}

    public record Item(
            UUID questionVersionId,
            int sortOrder,
            String stem,
            @JsonRawValue String options,
            @JsonRawValue String answerSpec,
            String explanation,
            double maxScore,
            List<KnowledgePointMapping> knowledgePointMappings
    ) {}

    public record KnowledgePointMapping(UUID knowledgePointId, double weight) {}

    public static PackageVersionContentResponse from(PackageVersionContentResult result) {
        return new PackageVersionContentResponse(result.packageVersionId(), result.packageId(), result.packageType(),
                result.topicId(), result.rulesJson(),
                result.sections().stream()
                        .map(s -> new Section(s.sectionId(), s.title(), s.skill(), s.instructions(), s.sortOrder(),
                                s.passage(), s.items().stream()
                                .map(i -> new Item(i.questionVersionId(), i.sortOrder(), i.stem(), i.optionsJson(),
                                        i.answerSpecJson(), i.explanation(), i.maxScore(),
                                        i.knowledgePointMappings().stream()
                                                .map(m -> new KnowledgePointMapping(m.knowledgePointId(), m.weight()))
                                                .toList()))
                                .toList()))
                        .toList());
    }
}
