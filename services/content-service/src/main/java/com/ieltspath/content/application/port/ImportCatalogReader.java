package com.ieltspath.content.application.port;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** What a course import workbook is checked against: knowledge points, import references and package codes. */
public interface ImportCatalogReader {

    /** The ids of the active knowledge points with these codes. Unknown and inactive codes are absent. */
    Map<String, UUID> activeKnowledgePoints(Collection<String> codes);

    /** The given import references that a question already has. */
    Set<String> existingImportRefs(Collection<String> refs);

    /** The given package codes that a package already has. */
    Set<String> existingPackageCodes(Collection<String> codes);

    default Set<String> existingCourseCodes(Collection<String> codes) { return Set.of(); }
    default Set<String> existingTopicCodes(Collection<String> codes) { return Set.of(); }
    default Set<String> existingLessonCodes(Collection<String> codes) { return Set.of(); }
    default Set<String> existingBandLevels(Collection<BigDecimal> levels) { return Set.of(); }
    default Set<Integer> existingTopicSortOrders(Collection<Integer> orders) { return Set.of(); }
    default java.util.Map<String, String> existingQuestionSkills(Collection<String> refs) { return java.util.Map.of(); }
}
