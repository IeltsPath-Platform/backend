package com.ieltspath.assessment.application.port;

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
     * {@link com.ieltspath.assessment.application.exception.ContentUnavailableException}.
     */
    Optional<PackageVersion> findPackageVersion(UUID packageVersionId);

    record PackageVersion(UUID packageVersionId, UUID packageId, String packageType, List<Section> sections) {
        public PackageVersion(UUID packageVersionId, String packageType, List<Section> sections) {
            this(packageVersionId, null, packageType, sections);
        }
    }

    record Section(UUID sectionId, String title, String skill, String instructions, int sortOrder, String passage,
                   Audio audio, List<Item> items, Integer partNo, Integer timeLimitSeconds, List<QuestionGroup> groups) {
        public Section(UUID sectionId, String title, String skill, String instructions, int sortOrder, String passage,
                       Audio audio, List<Item> items) {
            this(sectionId, title, skill, instructions, sortOrder, passage, audio, items, null, null, List.of());
        }
    }

    record QuestionGroup(UUID groupId, int sortOrder, String formatCode, String instructions) {}

    /** Duration and transcript may be absent on older Content assets. */
    record Audio(UUID assetId, String mediaUrl, Integer durationSeconds, String transcript) {}

    /** {@code options} is null for a fill question; {@code answerSpec} follows answer-spec v1. */
    record Item(UUID questionVersionId, int sortOrder, String stem, List<Option> options,
                Map<String, Object> answerSpec, String explanation, BigDecimal maxScore,
                List<KnowledgePointWeight> knowledgePoints, UUID groupId) {
        public Item(UUID questionVersionId, int sortOrder, String stem, List<Option> options,
                    Map<String, Object> answerSpec, String explanation, BigDecimal maxScore,
                    List<KnowledgePointWeight> knowledgePoints) {
            this(questionVersionId, sortOrder, stem, options, answerSpec, explanation, maxScore, knowledgePoints, null);
        }
    }

    record Option(String optionKey, String content, int sortOrder) {}

    record KnowledgePointWeight(UUID knowledgePointId, BigDecimal weight) {}
}
