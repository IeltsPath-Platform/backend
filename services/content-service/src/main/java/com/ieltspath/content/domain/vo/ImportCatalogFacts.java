package com.ieltspath.content.domain.vo;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the catalogue says about the codes a workbook mentions: which knowledge point codes are active (with their ids),
 * which error codes are active, which {@code importRef} values and package codes already exist.
 */
public record ImportCatalogFacts(Map<String, UUID> knowledgePointIdsByCode, Set<String> activeErrorCodes,
                                 Set<String> existingImportRefs, Set<String> takenPackageCodes,
                                 Set<String> takenCourseCodes, Set<String> takenTopicCodes, Set<String> takenLessonCodes,
                                 Set<String> takenBandLevels, Set<Integer> takenTopicSortOrders,
                                 Map<String, String> existingQuestionSkillsByRef) {

    public ImportCatalogFacts {
        knowledgePointIdsByCode = Map.copyOf(knowledgePointIdsByCode);
        activeErrorCodes = Set.copyOf(activeErrorCodes);
        existingImportRefs = Set.copyOf(existingImportRefs);
        takenPackageCodes = Set.copyOf(takenPackageCodes);
        takenCourseCodes = Set.copyOf(takenCourseCodes);
        takenTopicCodes = Set.copyOf(takenTopicCodes);
        takenLessonCodes = Set.copyOf(takenLessonCodes);
        takenBandLevels = Set.copyOf(takenBandLevels);
        takenTopicSortOrders = Set.copyOf(takenTopicSortOrders);
        existingQuestionSkillsByRef = Map.copyOf(existingQuestionSkillsByRef);
    }

    public ImportCatalogFacts(Map<String, UUID> knowledgePointIdsByCode, Set<String> activeErrorCodes,
                              Set<String> existingImportRefs, Set<String> takenPackageCodes) {
        this(knowledgePointIdsByCode, activeErrorCodes, existingImportRefs, takenPackageCodes, Set.of(), Set.of(),
                Set.of(), Set.of(), Set.of(), Map.of());
    }
}
