package com.group01.assessment.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads one published package version from Content Service, including answer specs. The result is used only to
 * snapshot an attempt; answer data never leaves Assessment through a learner response before the result allows it.
 */
public interface ContentPackageProvider {
    /**
     * @return empty when Content does not know the version or it is not published. Any other failure is thrown as
     * {@link com.group01.assessment.application.exception.ContentUnavailableException}.
     */
    Optional<PackageVersion> findPackageVersion(UUID packageVersionId);

    record PackageVersion(UUID packageVersionId, String packageType, List<Section> sections) {}

    record Section(UUID sectionId, String title, String skill, String instructions, int sortOrder, String passage,
                   List<Item> items) {}

    /** {@code options} is null for a fill question; {@code answerSpec} follows answer-spec v1. */
    record Item(UUID questionVersionId, int sortOrder, String stem, List<Option> options,
                Map<String, Object> answerSpec, String explanation, BigDecimal maxScore,
                List<KnowledgePointWeight> knowledgePoints) {}

    record Option(String optionKey, String content, int sortOrder) {}

    record KnowledgePointWeight(UUID knowledgePointId, BigDecimal weight) {}
}
