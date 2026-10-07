package com.ieltspath.content.api.dto.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.Skill;

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
    /** {@code audio} appears only on sections with an AUDIO asset; its transcript is internal-only. */
    public record Section(UUID sectionId, String title, Skill skill, String instructions, int sortOrder,
                          String passage, @JsonInclude(JsonInclude.Include.NON_NULL) Audio audio, List<Item> items) {}

    public record Audio(UUID assetId, String mediaUrl, Integer durationSeconds, String transcript) {}

    public record Item(
            UUID questionVersionId,
            int sortOrder,
            String stem,
            @JsonRawValue String options,
            @JsonRawValue String answerSpec,
            String explanation,
            String hint,
            double maxScore,
            List<KnowledgePointMapping> knowledgePointMappings
    ) {}

    public record KnowledgePointMapping(UUID knowledgePointId, double weight) {}

    public static PackageVersionContentResponse from(PackageVersionContentResult result) {
        return new PackageVersionContentResponse(result.packageVersionId(), result.packageId(), result.packageType(),
                result.topicId(), result.rulesJson(),
                result.sections().stream()
                        .map(s -> new Section(s.sectionId(), s.title(), s.skill(), s.instructions(), s.sortOrder(),
                                s.passage(),
                                s.audio() == null ? null : new Audio(s.audio().assetId(), s.audio().mediaUrl(),
                                        s.audio().durationSeconds(), s.audio().transcript()),
                                s.items().stream()
                                .map(i -> new Item(i.questionVersionId(), i.sortOrder(), i.stem(), i.optionsJson(),
                                        i.answerSpecJson(), i.explanation(), i.hint(), i.maxScore(),
                                        i.knowledgePointMappings().stream()
                                                .map(m -> new KnowledgePointMapping(m.knowledgePointId(), m.weight()))
                                                .toList()))
                                .toList()))
                        .toList());
    }
}
