package com.group01.learning.application.port;

import com.group01.learning.domain.vo.LearningSkill;

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

    /** {@code requiredFeatureKey} is the Access feature needed to learn the topic; null means free. */
    record Topic(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                 List<KnowledgePoint> knowledgePoints, LearningSkill skill, boolean hasTopicTest) {
        public Topic(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                     List<KnowledgePoint> knowledgePoints) {
            this(topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, null, true);
        }
        public Topic(UUID topicId, String code, String name, int sortOrder, List<KnowledgePoint> knowledgePoints) {
            this(topicId, code, name, sortOrder, null, knowledgePoints);
        }
    }

    record KnowledgePoint(UUID id, String code, String name, String learningType, String skill,
                          String description, boolean hasPracticeSet) {
    }

    record LessonSummary(UUID lessonId, UUID topicId, String code, String title, String summary,
                         int sortOrder, List<UUID> knowledgePointIds, List<UUID> exerciseBlockIds) {
    }

    record Lesson(UUID lessonId, UUID topicId, String code, String title, String summary,
                  int sortOrder, List<UUID> knowledgePointIds, List<Block> blocks, LearningSkill skill) {
        public Lesson(UUID lessonId, UUID topicId, String code, String title, String summary,
                      int sortOrder, List<UUID> knowledgePointIds, List<Block> blocks) {
            this(lessonId, topicId, code, title, summary, sortOrder, knowledgePointIds, blocks, null);
        }
    }

    /** {@code blockKind} is {@code EXERCISE} or {@code ESSAY} on exercise blocks; absent means {@code EXERCISE}. */
    record Block(UUID blockId, String blockType, int sortOrder, String textContent, Asset asset,
                 List<UUID> vocabularySenseIds, List<Question> questions, String blockKind) {
        public Block(UUID blockId, String blockType, int sortOrder, String textContent, Asset asset,
                     List<UUID> vocabularySenseIds, List<Question> questions) {
            this(blockId, blockType, sortOrder, textContent, asset, vocabularySenseIds, questions, null);
        }

        public boolean isExercise() {
            return "EXERCISE".equals(blockType) && !isEssay();
        }

        public boolean isEssay() {
            return "EXERCISE".equals(blockType) && "ESSAY".equals(blockKind);
        }
    }

    /** {@code assets} are the images of an essay question; Content omits them elsewhere. */
    record Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options,
                    Map<String, Object> answerSpec, String explanation, List<UUID> knowledgePointIds,
                    List<QuestionAsset> assets, String hint) {
        public Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options,
                        Map<String, Object> answerSpec, String explanation, List<UUID> knowledgePointIds,
                        List<QuestionAsset> assets) {
            this(questionVersionId, sortOrder, stem, options, answerSpec, explanation, knowledgePointIds, assets, null);
        }

        public Question(UUID questionVersionId, int sortOrder, String stem, List<Option> options,
                        Map<String, Object> answerSpec, String explanation, List<UUID> knowledgePointIds) {
            this(questionVersionId, sortOrder, stem, options, answerSpec, explanation, knowledgePointIds, null);
        }
    }

    record QuestionAsset(UUID assetId, String assetType, String mediaUrl, String altText, int sortOrder) {
    }

    record Option(String optionKey, String content, int sortOrder) {
    }

    /** For {@code AUDIO}, {@code textContent} is the transcript and {@code mediaUrl} the playable URL. */
    record Asset(UUID id, String assetType, String textContent, String mediaReference,
                 Integer durationSeconds, String mediaUrl) {
        public Asset(UUID id, String assetType, String textContent, String mediaReference, Integer durationSeconds) {
            this(id, assetType, textContent, mediaReference, durationSeconds, null);
        }
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
                   String passage, List<Item> items, SectionAudio audio) {
        public Section(UUID sectionId, String title, String skill, String instructions, int sortOrder,
                       String passage, List<Item> items) {
            this(sectionId, title, skill, instructions, sortOrder, passage, items, null);
        }
    }

    /** The transcript is an answer: learners see it only after passing. */
    record SectionAudio(UUID assetId, String mediaUrl, Integer durationSeconds, String transcript) {
    }

    record Item(UUID questionVersionId, int sortOrder, String stem, List<Option> options,
                Map<String, Object> answerSpec, String explanation, BigDecimal maxScore,
                List<KnowledgePointMapping> knowledgePointMappings) {
    }

    record KnowledgePointMapping(UUID knowledgePointId, BigDecimal weight) {
    }
}
