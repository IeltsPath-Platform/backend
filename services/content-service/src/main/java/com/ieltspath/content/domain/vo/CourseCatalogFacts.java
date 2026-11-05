package com.ieltspath.content.domain.vo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the catalogue says about the things a course draft mentions, read once so the coverage check can stay free of
 * any storage. Anything the draft names but the catalogue lacks is simply absent.
 */
public record CourseCatalogFacts(
        Map<UUID, KnowledgePointFact> knowledgePoints,
        Set<UUID> expectedKnowledgePoints,
        Map<UUID, Set<UUID>> prerequisites,
        Map<UUID, BigDecimal> lowestBandTaught,
        Map<UUID, PackageFact> packages,
        Set<UUID> milestonePackagesLinkedElsewhere,
        Map<UUID, Integer> unlinkedPracticeSets,
        Set<UUID> withoutExerciseQuestion,
        Map<String, UUID> resolvedQuestionRefs,
        Set<String> takenCourseCodes,
        Set<String> takenTopicCodes,
        Set<String> takenLessonCodes,
        Set<String> takenPackageCodes,
        boolean bandTaken,
        Set<Integer> takenTopicSortOrders,
        Set<UUID> ownedQuestionVersions) {

    /** The facts of a catalogue in which none of the draft's questions belongs to a lesson or a package yet. */
    public CourseCatalogFacts(Map<UUID, KnowledgePointFact> knowledgePoints, Set<UUID> expectedKnowledgePoints,
                              Map<UUID, Set<UUID>> prerequisites, Map<UUID, BigDecimal> lowestBandTaught,
                              Map<UUID, PackageFact> packages, Set<UUID> milestonePackagesLinkedElsewhere,
                              Map<UUID, Integer> unlinkedPracticeSets, Set<UUID> withoutExerciseQuestion,
                              Map<String, UUID> resolvedQuestionRefs, Set<String> takenCourseCodes,
                              Set<String> takenTopicCodes, Set<String> takenLessonCodes, Set<String> takenPackageCodes,
                              boolean bandTaken, Set<Integer> takenTopicSortOrders) {
        this(knowledgePoints, expectedKnowledgePoints, prerequisites, lowestBandTaught, packages,
                milestonePackagesLinkedElsewhere, unlinkedPracticeSets, withoutExerciseQuestion, resolvedQuestionRefs,
                takenCourseCodes, takenTopicCodes, takenLessonCodes, takenPackageCodes, bandTaken,
                takenTopicSortOrders, Set.of());
    }

    public CourseCatalogFacts {
        knowledgePoints = Map.copyOf(knowledgePoints);
        expectedKnowledgePoints = Set.copyOf(expectedKnowledgePoints);
        prerequisites = Map.copyOf(prerequisites);
        lowestBandTaught = Map.copyOf(lowestBandTaught);
        packages = Map.copyOf(packages);
        milestonePackagesLinkedElsewhere = Set.copyOf(milestonePackagesLinkedElsewhere);
        unlinkedPracticeSets = Map.copyOf(unlinkedPracticeSets);
        withoutExerciseQuestion = Set.copyOf(withoutExerciseQuestion);
        resolvedQuestionRefs = Map.copyOf(resolvedQuestionRefs);
        takenCourseCodes = Set.copyOf(takenCourseCodes);
        takenTopicCodes = Set.copyOf(takenTopicCodes);
        takenLessonCodes = Set.copyOf(takenLessonCodes);
        takenPackageCodes = Set.copyOf(takenPackageCodes);
        takenTopicSortOrders = Set.copyOf(takenTopicSortOrders);
        ownedQuestionVersions = Set.copyOf(ownedQuestionVersions);
    }

    /**
     * The same facts, also treating these question references as resolvable. A workbook import creates its questions
     * itself, so references to them cannot be looked up before the import has run; only that they exist matters here.
     */
    public CourseCatalogFacts withQuestionRefs(Set<String> refs) {
        Map<String, UUID> resolved = new java.util.HashMap<>(resolvedQuestionRefs);
        refs.forEach(ref -> resolved.putIfAbsent(ref, UUID.nameUUIDFromBytes(ref.getBytes(
                java.nio.charset.StandardCharsets.UTF_8))));
        return new CourseCatalogFacts(knowledgePoints, expectedKnowledgePoints, prerequisites, lowestBandTaught,
                packages, milestonePackagesLinkedElsewhere, unlinkedPracticeSets, withoutExerciseQuestion, resolved,
                takenCourseCodes, takenTopicCodes, takenLessonCodes, takenPackageCodes, bandTaken,
                takenTopicSortOrders, ownedQuestionVersions);
    }

    /** {@code status} is {@code ACTIVE} or {@code INACTIVE}. */
    public record KnowledgePointFact(UUID id, String code, String status) {
        public boolean active() {
            return "ACTIVE".equals(status);
        }
    }

    /** The links are null when the package belongs to no lesson, topic or course. */
    public record PackageFact(UUID id, String code, String type, String status, UUID lessonId, UUID topicId,
                              UUID courseId) {
        public boolean published() {
            return "PUBLISHED".equals(status);
        }

        public boolean linked() {
            return lessonId != null || topicId != null || courseId != null;
        }
    }

    /** What to look up: the ids, references and codes a draft mentions. */
    public record Query(Set<UUID> knowledgePointIds, Set<UUID> practiceSetPackageIds, Set<UUID> milestonePackageIds,
                        Set<String> questionRefs, Set<String> courseCodes, Set<String> topicCodes,
                        Set<String> lessonCodes, Set<String> packageCodes, BigDecimal band, String cefrLevel,
                        Set<String> skills) {
    }
}
