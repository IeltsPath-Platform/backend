package com.ieltspath.learning.application.port;

import com.ieltspath.learning.domain.vo.LearningSkill;

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
    ProgressMetadata progressMetadata(List<UUID> courseIds);

    record ProgressMetadata(String revision, boolean complete, List<ProgressLesson> lessons) {}
    record ProgressLesson(UUID courseId, String courseName, BigDecimal bandLevel, UUID topicId,
                          UUID lessonId, Set<LearningSkill> skills) {}

    List<Topic> getTopicSequence();

    default List<Topic> getTopicSequence(boolean includeErrorTraps) {
        return getTopicSequence();
    }

    List<LessonSummary> getTopicLessons(UUID topicId);

    Lesson getLesson(UUID lessonId);

    List<TestPackage> getTopicTestPackages(UUID topicId);

    List<TestPackage> getCourseTestPackages(UUID courseId);

    List<PracticeTestMilestone> getPracticeTestMilestones(UUID courseId);

    /** A published lesson's Markdown summary and its slide and document links. Callers apply the lesson gate. */
    LessonResources getLessonResources(UUID lessonId);

    /** {@code summaryMarkdown} is {@code null} when the lesson has none; links are in order. */
    record LessonResources(UUID lessonId, String summaryMarkdown, List<ResourceLink> slides,
                           List<ResourceLink> documents) {
        public LessonResources {
            slides = slides == null ? List.of() : List.copyOf(slides);
            documents = documents == null ? List.of() : List.copyOf(documents);
        }
    }

    record ResourceLink(UUID id, String title, String url, int sortOrder) {}

    record PracticeTestMilestone(UUID packageId, UUID packageVersionId, String code, String title,
                                 int afterTopicSequence) {}

    /** Published placement tests, ordered by package id. */
    List<TestPackage> getPlacementTestPackages();

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

    default List<PracticeSet> searchPracticeSets(UUID knowledgePointId, List<UUID> excludePackageIds,
                                                 int minQuestions, int limit, UUID preferredLessonId,
                                                 String trapErrorCode) {
        return searchPracticeSets(knowledgePointId, excludePackageIds, minQuestions, limit, preferredLessonId);
    }

    PackageVersion getPackageVersion(UUID versionId);

    /** The active prerequisites of each knowledge point; a point without any maps to an empty list. */
    Map<UUID, List<UUID>> prerequisites(List<UUID> knowledgePointIds);

    /**
     * The difficulty tag (EASY, MEDIUM, HARD) of each question version Content knows (at most 500); untagged or unknown
     * versions are absent. Used only for statistics, never to pick what a learner is given.
     */
    default Map<UUID, String> questionDifficulties(Collection<UUID> questionVersionIds) {
        return Map.of();
    }

    /**
     * Names of the knowledge points and the start of the question stems (at most 500 of each) that staff work items are
     * about. Unknown ids are absent. Used only to label the work queue.
     */
    default WorkItemSubjects workItemSubjects(Collection<UUID> knowledgePointIds, Collection<UUID> questionVersionIds) {
        return new WorkItemSubjects(Map.of(), Map.of());
    }

    record WorkItemSubjects(Map<UUID, KnowledgePointLabel> knowledgePoints, Map<UUID, QuestionLabel> questions) {
        public WorkItemSubjects {
            knowledgePoints = knowledgePoints == null ? Map.of() : knowledgePoints;
            questions = questions == null ? Map.of() : questions;
        }
    }

    record KnowledgePointLabel(String code, String name) {}

    record QuestionLabel(UUID questionId, String stemPreview) {}

    /** Published practice sets, practice tests and lessons for these points (at most 50), none above {@code maxBand}. */
    SuggestionCandidates suggestionCandidates(List<UUID> knowledgePointIds, BigDecimal maxBand);

    record SuggestionCandidates(List<PackageCandidate> packages, List<LessonCandidate> lessons) {}
    record PackageCandidate(UUID packageId, UUID packageVersionId, String type, String title,
                            List<UUID> knowledgePointIds, BigDecimal band, String skill) {}
    record LessonCandidate(UUID lessonId, UUID topicId, String title, List<UUID> knowledgePointIds, BigDecimal band,
                           String skill, int sortOrder) {}

    /** Active grading rubric for a writing task. */
    Rubric getActiveRubric(String task);

    /** An immutable published rubric, including retired versions used by an activated prompt. */
    Rubric getRubricVersion(String code, int version);

    /** Error codes currently accepted by Content. */
    List<ErrorType> activeErrorTypes();

    /** Active error types including ones not yet reviewed; only for the wording shown to a learner, never for grading. */
    List<ErrorType> learnerErrorTypes();

    record Rubric(String code, String task, int version, String sourceNote, List<RubricCriterion> criteria,
                  List<RubricExample> examples) {}
    /** {@code knowledgePointId} is the point the criterion is evidence for; null when Content does not map it. */
    record RubricCriterion(String code, String name, String description, int sortOrder,
                           List<RubricBandDescriptor> bandDescriptors, UUID knowledgePointId,
                           List<SubKnowledgePoint> subKnowledgePoints) {
        public RubricCriterion {
            subKnowledgePoints = subKnowledgePoints == null ? List.of() : List.copyOf(subKnowledgePoints);
        }

        public RubricCriterion(String code, String name, String description, int sortOrder,
                               List<RubricBandDescriptor> bandDescriptors) {
            this(code, name, description, sortOrder, bandDescriptors, null, List.of());
        }

        public RubricCriterion(String code, String name, String description, int sortOrder,
                               List<RubricBandDescriptor> bandDescriptors, UUID knowledgePointId) {
            this(code, name, description, sortOrder, bandDescriptors, knowledgePointId, List.of());
        }
    }
    record SubKnowledgePoint(UUID id, String code, String name, String description) {}
    record RubricBandDescriptor(java.math.BigDecimal band, String descriptor) {}
    record RubricExample(java.math.BigDecimal band, String prompt, String essay, String commentary) {}
    record ErrorType(String code, String name, String category, int severity, String fixHint, UUID knowledgePointId) {
        public ErrorType(String code, String name, String category, int severity, String fixHint) {
            this(code, name, category, severity, fixHint, null);
        }
    }

    /** The given skills in enum order, or the single legacy skill when Content did not send any. */
    static Set<LearningSkill> skillsOrSingle(Collection<LearningSkill> skills, LearningSkill single) {
        EnumSet<LearningSkill> result = EnumSet.noneOf(LearningSkill.class);
        if (skills != null) skills.stream().filter(Objects::nonNull).forEach(result::add);
        else if (single != null) result.add(single);
        return Collections.unmodifiableSet(result);
    }

    /**
     * {@code requiredFeatureKey} is the Access feature needed to learn the topic; null means free. {@code skills} are
     * the skills its lessons teach; a response without them falls back to {@code skill}. {@code lessonCount} is the
     * number of published lessons; 0 from Content that does not send it.
     */
    record Topic(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                 List<KnowledgePoint> knowledgePoints, LearningSkill skill, boolean hasTopicTest, Course course,
                 Set<LearningSkill> skills, Boolean active, int lessonCount) {
        public Topic {
            skills = skillsOrSingle(skills, skill);
            active = active == null || active;
        }
        public Topic(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                     List<KnowledgePoint> knowledgePoints, LearningSkill skill, boolean hasTopicTest, Course course,
                     Set<LearningSkill> skills, Boolean active) {
            this(topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, skill, hasTopicTest, course,
                    skills, active, 0);
        }
        public Topic(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                     List<KnowledgePoint> knowledgePoints, LearningSkill skill, boolean hasTopicTest, Course course,
                     Set<LearningSkill> skills) {
            this(topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, skill, hasTopicTest, course,
                    skills, true, 0);
        }
        public Topic(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                     List<KnowledgePoint> knowledgePoints, LearningSkill skill, boolean hasTopicTest, Course course) {
            this(topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, skill, hasTopicTest, course,
                    null, true, 0);
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

    record Course(UUID courseId, String code, String name, BigDecimal bandLevel, boolean hasCourseTest, Boolean active) {
        public Course {
            active = active == null || active;
        }

        public Course(UUID courseId, String code, String name, BigDecimal bandLevel, boolean hasCourseTest) {
            this(courseId, code, name, bandLevel, hasCourseTest, true);
        }
    }

    /** {@code cefrLevel} ({@code A1} to {@code C2}) and {@code canDo} are null when Content has not set them. */
    record KnowledgePoint(UUID id, String code, String name, String learningType, String skill,
                          String description, boolean hasPracticeSet, String cefrLevel, String canDo,
                          UUID parentKnowledgePointId, String errorCode) {
        public KnowledgePoint(UUID id, String code, String name, String learningType, String skill,
                              String description, boolean hasPracticeSet, String cefrLevel, String canDo) {
            this(id, code, name, learningType, skill, description, hasPracticeSet, cefrLevel, canDo, null, null);
        }
        public KnowledgePoint(UUID id, String code, String name, String learningType, String skill,
                              String description, boolean hasPracticeSet) {
            this(id, code, name, learningType, skill, description, hasPracticeSet, null, null, null, null);
        }
    }

    record LessonSummary(UUID lessonId, UUID topicId, String code, String title, String summary,
                         int sortOrder, List<UUID> knowledgePointIds, List<UUID> exerciseBlockIds,
                         Set<LearningSkill> skills, Integer estimatedMinutes) {
        public LessonSummary {
            skills = skillsOrSingle(skills, null);
        }
        public LessonSummary(UUID lessonId, UUID topicId, String code, String title, String summary,
                             int sortOrder, List<UUID> knowledgePointIds, List<UUID> exerciseBlockIds,
                             Set<LearningSkill> skills) {
            this(lessonId, topicId, code, title, summary, sortOrder, knowledgePointIds, exerciseBlockIds, skills, null);
        }
        public LessonSummary(UUID lessonId, UUID topicId, String code, String title, String summary,
                             int sortOrder, List<UUID> knowledgePointIds, List<UUID> exerciseBlockIds) {
            this(lessonId, topicId, code, title, summary, sortOrder, knowledgePointIds, exerciseBlockIds, null, null);
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

    /**
     * {@code band} is the band of the course the package belongs to and {@code requiredFeatureKey} the Access feature it
     * needs; both are null when Content does not say, which means no limit.
     */
    record PracticeSet(UUID packageId, UUID packageVersionId, String code, int questionCount,
                       int matchedQuestionCount, BigDecimal band, String requiredFeatureKey, int trapQuestionCount) {
        public PracticeSet(UUID packageId, UUID packageVersionId, String code, int questionCount,
                           int matchedQuestionCount, BigDecimal band, String requiredFeatureKey) {
            this(packageId, packageVersionId, code, questionCount, matchedQuestionCount, band, requiredFeatureKey, 0);
        }
        public PracticeSet(UUID packageId, UUID packageVersionId, String code, int questionCount,
                           int matchedQuestionCount) {
            this(packageId, packageVersionId, code, questionCount, matchedQuestionCount, null, null, 0);
        }
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
                List<KnowledgePointMapping> knowledgePointMappings, String guide, List<WritingImage> images) {
        public Item {
            images = images == null ? List.of() : List.copyOf(images);
        }

        public Item(UUID questionVersionId, int sortOrder, String stem, List<Option> options,
                    Map<String, Object> answerSpec, String explanation, String hint, BigDecimal maxScore,
                    List<KnowledgePointMapping> knowledgePointMappings) {
            this(questionVersionId, sortOrder, stem, options, answerSpec, explanation, hint, maxScore,
                    knowledgePointMappings, null, List.of());
        }

        public Item(UUID questionVersionId, int sortOrder, String stem, List<Option> options,
                    Map<String, Object> answerSpec, String explanation, BigDecimal maxScore,
                    List<KnowledgePointMapping> knowledgePointMappings) {
            this(questionVersionId, sortOrder, stem, options, answerSpec, explanation, null, maxScore,
                    knowledgePointMappings);
        }
    }

    record WritingImage(String mediaUrl, String altText) {}

    record KnowledgePointMapping(UUID knowledgePointId, BigDecimal weight) {
    }
}
