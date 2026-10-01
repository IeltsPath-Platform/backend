package com.group01.learning.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LearningContentClient {
    List<Topic> getTopicSequence();

    List<LessonSummary> getTopicLessons(UUID topicId);

    Lesson getLesson(UUID lessonId);

    List<TestPackage> getTopicTestPackages(UUID topicId);

    List<PracticeSet> searchPracticeSets(UUID knowledgePointId, List<UUID> excludePackageIds,
                                       int minQuestions, int limit);

    PackageVersion getPackageVersion(UUID versionId);

    record Topic(UUID topicId, String code, String name, int sortOrder,
                 List<KnowledgePoint> knowledgePoints) {
    }

    record KnowledgePoint(UUID id, String code, String name, String learningType, String skill,
                          String description, boolean hasPracticeSet) {
    }

    record LessonSummary(UUID lessonId, UUID topicId, String code, String title, String summary,
                         int sortOrder, List<UUID> knowledgePointIds, List<UUID> exerciseBlockIds) {
    }

    record Lesson(UUID lessonId, UUID topicId, String code, String title, String summary,
                  int sortOrder, List<UUID> knowledgePointIds, List<Block> blocks) {
    }

    record Block(UUID blockId, String blockType, int sortOrder, String textContent, Asset asset,
                 List<UUID> vocabularySenseIds, List<Question> questions) {
    }

    record Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options,
                    Map<String, Object> answerSpec, String explanation, List<UUID> knowledgePointIds) {
    }

    record Option(String optionKey, String content, int sortOrder) {
    }

    record Asset(UUID id, String assetType, String textContent, String mediaReference,
                 Integer durationSeconds) {
    }

    record TestPackage(UUID packageId, UUID packageVersionId, String code) {
    }

    record PracticeSet(UUID packageId, UUID packageVersionId, String code, int questionCount,
                       int matchedQuestionCount) {
    }

    record PackageVersion(UUID packageVersionId, UUID packageId, String packageType, UUID topicId,
                          Map<String, Object> rules, List<Section> sections) {
    }

    record Section(UUID sectionId, String title, String skill, String instructions, int sortOrder,
                   String passage, List<Item> items) {
    }

    record Item(UUID questionVersionId, int sortOrder, String stem, List<Option> options,
                Map<String, Object> answerSpec, String explanation, BigDecimal maxScore,
                List<KnowledgePointMapping> knowledgePointMappings) {
    }

    record KnowledgePointMapping(UUID knowledgePointId, BigDecimal weight) {
    }
}
