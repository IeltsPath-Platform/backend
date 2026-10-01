package com.group01.content.application.result;

import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.Skill;

import java.util.List;
import java.util.UUID;

/**
 * A published package version with sections, items, answer specs and knowledge point mappings, for services that
 * grade or deliver it. JSON values ({@code rulesJson}, {@code optionsJson}, {@code answerSpecJson}) are kept as stored.
 */
public record PackageVersionContentResult(
        UUID packageVersionId,
        UUID packageId,
        PackageType packageType,
        UUID topicId,
        String rulesJson,
        List<Section> sections
) {
    public PackageVersionContentResult withSections(List<Section> newSections) {
        return new PackageVersionContentResult(packageVersionId, packageId, packageType, topicId, rulesJson,
                newSections);
    }

    /** {@code passage}: text of the section's PASSAGE asset, or null; {@code audio}: its AUDIO asset, or null. */
    public record Section(
            UUID sectionId,
            String title,
            Skill skill,
            String instructions,
            int sortOrder,
            String passage,
            SectionAudio audio,
            List<Item> items
    ) {
        public Section withAudio(SectionAudio newAudio) {
            return new Section(sectionId, title, skill, instructions, sortOrder, passage, newAudio, items);
        }
    }

    /** {@code mediaUrl} is set once the use case has checked the stored reference; the transcript is an answer. */
    public record SectionAudio(UUID assetId, String mediaReference, String mediaUrl, Integer durationSeconds,
                               String transcript) {
        public SectionAudio withMediaUrl(String url) {
            return new SectionAudio(assetId, mediaReference, url, durationSeconds, transcript);
        }
    }

    public record Item(
            UUID questionVersionId,
            int sortOrder,
            String stem,
            String optionsJson,
            String answerSpecJson,
            String explanation,
            double maxScore,
            List<KnowledgePointMapping> knowledgePointMappings
    ) {}

    public record KnowledgePointMapping(UUID knowledgePointId, double weight) {}
}
