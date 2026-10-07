package com.group01.learning.application.port;

import com.group01.learning.domain.vo.LearningSkill;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface LearningContentClient {
    List<Topic> getTopicSequence();

    List<LessonSummary> getTopicLessons(UUID topicId);

    Lesson getLesson(UUID lessonId);

    List<TestPackage> getTopicTestPackages(UUID topicId);

    List<TestPackage> getCourseTestPackages(UUID courseId);

    List<LessonPracticeSet> lessonPracticeSets(UUID lessonId);

    /** Only the sets whose questions are all of {@code skill}; every set when empty. */
    default List<LessonPracticeSet> lessonPracticeSets(UUID lessonId, Optional<LearningSkill> skill) {
        List<LessonPracticeSet> sets = lessonPracticeSets(lessonId);
        return skill.isEmpty() || sets == null ? sets : sets.stream()
                .filter(set -> set.skills().equals(Set.of(skill.get()))).toList();
    }

    TopicPracticeSets topicPracticeSets(UUID topicId);

    Map<UUID, Integer> practiceSetAvailability(List<UUID> knowledgePointIds, List<UUID> excludePackageIds,
                                                int minQuestions);

    List<PracticeSet> searchPracticeSets(UUID knowledgePointId, List<UUID> excludePackageIds,
                                       int minQuestions, int limit);

    default List<PracticeSet> searchPracticeSets(UUID knowledgePointId, List<UUID> excludePackageIds,
                                                 int minQuestions, int limit, UUID preferredLessonId) {
        return searchPracticeSets(knowledgePointId, excludePackageIds, minQuestions, limit);
    }

    PackageVersion getPackageVersion(UUID versionId);

    /** The given skills in enum order, or the single legacy skill when Content did not send any. */
    static Set<LearningSkill> skillsOrSingle(Collection<LearningSkill> skills, LearningSkill single) {
        EnumSet<LearningSkill> result = EnumSet.noneOf(LearningSkill.class);
        if (skills != null) skills.stream().filter(Objects::nonNull).forEach(result::add);
        else if (single != null) result.add(single);
        return Collections.unmodifiableSet(result);
    }

    /**
     * {@code requiredFeatureKey} is the Access feature needed to learn the topic; null means free. {@code skills} are
     * the skills its lessons teach; a response without them falls back to {@code skill}.
     */
    record Topic(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                 List<KnowledgePoint> knowledgePoints, LearningSkill skill, boolean hasTopicTest, Course course,
                 Set<LearningSkill> skills) {
        public Topic {
            skills = skillsOrSingle(skills, skill);
        }
        public Topic(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                     List<KnowledgePoint> knowledgePoints, LearningSkill skill, boolean hasTopicTest, Course course) {
            this(topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, skill, hasTopicTest, course, null);
        }
        public Topic(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                     List<KnowledgePoint> knowledgePoints, LearningSkill skill, boolean hasTopicTest) {
            this(topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, skill, hasTopicTest, null);
        }
        public Topic(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                     List<KnowledgePoint> knowledgePoints) {
            this(topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, null, true, null);
        }
        public Topic(UUID topicId, String code, String name, int sortOrder, List<KnowledgePoint> knowledgePoints) {
            this(topicId, code, name, sortOrder, null, knowledgePoints);
        }
    }

    record Course(UUID courseId, String code, String name, BigDecimal bandLevel, boolean hasCourseTest) {}

    record KnowledgePoint(UUID id, String code, String name, String learningType, String skill,
                          String description, boolean hasPracticeSet) {
    }

    record LessonSummary(UUID lessonId, UUID topicId, String code, String title, String summary,
                         int sortOrder, List<UUID> knowledgePointIds, List<UUID> exerciseBlockIds,
                         Set<LearningSkill> skills) {
        public LessonSummary {
            skills = skillsOrSingle(skills, null);
        }
        public LessonSummary(UUID lessonId, UUID topicId, String code, String title, String summary,
                             int sortOrder, List<UUID> knowledgePointIds, List<UUID> exerciseBlockIds) {
            this(lessonId, topicId, code, title, summary, sortOrder, knowledgePointIds, exerciseBlockIds, null);
        }
    }

    /** {@code skills} are the skills the lesson teaches; a response without them falls back to {@code skill}. */
    record Lesson(UUID lessonId, UUID topicId, String code, String title, String summary,
                  int sortOrder, List<UUID> knowledgePointIds, List<Block> blocks, LearningSkill skill,
                  Set<LearningSkill> skills) {
        public Lesson {
            skills = skillsOrSingle(skills, skill);
        }
        public Lesson(UUID lessonId, UUID topicId, String code, String title, String summary,
                      int sortOrder, List<UUID> knowledgePointIds, List<Block> blocks, LearningSkill skill) {
            this(lessonId, topicId, code, title, summary, sortOrder, knowledgePointIds, blocks, skill, null);
        }
        public Lesson(UUID lessonId, UUID topicId, String code, String title, String summary,
                      int sortOrder, List<UUID> knowledgePointIds, List<Block> blocks) {
            this(lessonId, topicId, code, title, summary, sortOrder, knowledgePointIds, blocks, null);
        }
    }

    /** {@code blockKind} is {@code EXERCISE} or {@code ESSAY} on exercise blocks; absent means {@code EXERCISE}. */
    /** {@code knowledgePointIds}: the KPs a TEXT block teaches, or the KPs of an exercise block's questions. */
    record Block(UUID blockId, String blockType, int sortOrder, String textContent, Asset asset,
                 List<UUID> vocabularySenseIds, List<Question> questions, String blockKind,
                 List<UUID> knowledgePointIds) {
        public Block(UUID blockId, String blockType, int sortOrder, String textContent, Asset asset,
                     List<UUID> vocabularySenseIds, List<Question> questions, String blockKind) {
            this(blockId, blockType, sortOrder, textContent, asset, vocabularySenseIds, questions, blockKind, null);
        }

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

    /** {@code skills} are the skills of the set's questions; empty from Content that does not send them. */
    record LessonPracticeSet(UUID packageId, UUID packageVersionId, String code, String title,
                             int questionCount, List<UUID> knowledgePointIds, String requiredFeatureKey,
                             Set<LearningSkill> skills) {
        public LessonPracticeSet {
            skills = skillsOrSingle(skills, null);
        }
        public LessonPracticeSet(UUID packageId, UUID packageVersionId, String code, String title,
                                 int questionCount, List<UUID> knowledgePointIds, String requiredFeatureKey) {
            this(packageId, packageVersionId, code, title, questionCount, knowledgePointIds, requiredFeatureKey, null);
        }
    }

    record TopicPracticeSets(List<LessonSets> lessons) {
        public record LessonSets(UUID lessonId, List<LessonPracticeSet> practiceSets) {}
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

    /** The transcript is an answer: learner responses expose it only after the permitted submission. */
    record SectionAudio(UUID assetId, String mediaUrl, Integer durationSeconds, String transcript) {
    }

    record Item(UUID questionVersionId, int sortOrder, String stem, List<Option> options,
                Map<String, Object> answerSpec, String explanation, String hint, BigDecimal maxScore,
                List<KnowledgePointMapping> knowledgePointMappings) {
        public Item(UUID questionVersionId, int sortOrder, String stem, List<Option> options,
                    Map<String, Object> answerSpec, String explanation, BigDecimal maxScore,
                    List<KnowledgePointMapping> knowledgePointMappings) {
            this(questionVersionId, sortOrder, stem, options, answerSpec, explanation, null, maxScore,
                    knowledgePointMappings);
        }
    }

    record KnowledgePointMapping(UUID knowledgePointId, BigDecimal weight) {
    }
}
