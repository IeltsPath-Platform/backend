package com.ieltspath.content.domain.vo;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the catalogue says about the codes a workbook mentions: which knowledge point codes are active (with their ids),
 * which error codes are active, which {@code importRef} values and package codes already exist.
 */
public record ImportCatalogFacts(Map<String, UUID> knowledgePointIdsByCode, Set<String> activeErrorCodes,
                                 Set<String> existingImportRefs, Set<String> takenPackageCodes) {

    public ImportCatalogFacts {
        knowledgePointIdsByCode = Map.copyOf(knowledgePointIdsByCode);
        activeErrorCodes = Set.copyOf(activeErrorCodes);
        existingImportRefs = Set.copyOf(existingImportRefs);
        takenPackageCodes = Set.copyOf(takenPackageCodes);
    }
}
