package com.ieltspath.content.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The plan of a course: topics, lessons, the practice sets and questions that go in them, and the final tests. It is
 * data only; approving it is what creates content. Lists are never null.
 */
public record CourseDraftStructure(CourseInfo course, List<TopicPlan> topics, FinalTestPlan courseTest,
                                   List<MilestonePlan> practiceTestMilestones, List<GapNote> gaps) {

    public CourseDraftStructure {
        topics = topics == null ? List.of() : List.copyOf(topics);
        practiceTestMilestones = practiceTestMilestones == null ? List.of() : List.copyOf(practiceTestMilestones);
        gaps = gaps == null ? List.of() : List.copyOf(gaps);
    }

    public static CourseDraftStructure empty() {
        return new CourseDraftStructure(null, List.of(), null, List.of(), List.of());
    }

    public record CourseInfo(String code, String name, BigDecimal bandLevel) {
    }

    public record TopicPlan(String code, String name, Skill skill, Integer sortOrder, List<LessonPlan> lessons,
                            FinalTestPlan topicTest) {
        public TopicPlan {
            lessons = lessons == null ? List.of() : List.copyOf(lessons);
        }
    }

    /**
     * {@code summaryMarkdown} is the summary the learner reads on the lesson page, {@code estimatedMinutes} how long the
     * lesson takes, and {@code resources} its slides and documents (links). {@code vocabularySenseIds} are senses the
     * library already has; {@code newVocabulary} are words the model proposed, written only when the draft is approved.
     * {@code newPracticeSets} are practice sets the model wrote for this lesson; approval creates them as drafts.
     * A draft that predates these fields reads them as empty.
     */
    public record LessonPlan(String code, String title, String summary, List<UUID> knowledgePointIds,
                             List<BlockPlan> blocks, List<UUID> practiceSetPackageIds, String summaryMarkdown,
                             Integer estimatedMinutes, List<ResourcePlan> resources, List<UUID> vocabularySenseIds,
                             List<NewWord> newVocabulary, List<PracticeSetPlan> newPracticeSets) {
        public LessonPlan {
            knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
            blocks = blocks == null ? List.of() : List.copyOf(blocks);
            practiceSetPackageIds = practiceSetPackageIds == null ? List.of() : List.copyOf(practiceSetPackageIds);
            resources = resources == null ? List.of() : List.copyOf(resources);
            vocabularySenseIds = vocabularySenseIds == null ? List.of() : List.copyOf(vocabularySenseIds);
            newVocabulary = newVocabulary == null ? List.of() : List.copyOf(newVocabulary);
            newPracticeSets = newPracticeSets == null ? List.of() : List.copyOf(newPracticeSets);
        }

        /** A lesson with no practice sets of its own to create. */
        public LessonPlan(String code, String title, String summary, List<UUID> knowledgePointIds,
                          List<BlockPlan> blocks, List<UUID> practiceSetPackageIds, String summaryMarkdown,
                          Integer estimatedMinutes, List<ResourcePlan> resources, List<UUID> vocabularySenseIds,
                          List<NewWord> newVocabulary) {
            this(code, title, summary, knowledgePointIds, blocks, practiceSetPackageIds, summaryMarkdown,
                    estimatedMinutes, resources, vocabularySenseIds, newVocabulary, List.of());
        }

        /** A lesson with no vocabulary of its own. */
        public LessonPlan(String code, String title, String summary, List<UUID> knowledgePointIds,
                          List<BlockPlan> blocks, List<UUID> practiceSetPackageIds, String summaryMarkdown,
                          Integer estimatedMinutes, List<ResourcePlan> resources) {
            this(code, title, summary, knowledgePointIds, blocks, practiceSetPackageIds, summaryMarkdown,
                    estimatedMinutes, resources, List.of(), List.of(), List.of());
        }

        /** A lesson with only the fields a draft had before the summary, minutes and resources existed. */
        public LessonPlan(String code, String title, String summary, List<UUID> knowledgePointIds,
                          List<BlockPlan> blocks, List<UUID> practiceSetPackageIds) {
            this(code, title, summary, knowledgePointIds, blocks, practiceSetPackageIds, null, null, List.of());
        }

        /** Every knowledge point the lesson teaches: its own and those of its blocks. */
        public List<UUID> allKnowledgePointIds() {
            java.util.LinkedHashSet<UUID> all = new java.util.LinkedHashSet<>(knowledgePointIds);
            blocks.forEach(block -> all.addAll(block.knowledgePointIds()));
            return List.copyOf(all);
        }
    }

    /**
     * A word the model proposed for a lesson, with the sense it wrote. The author reviews it; on approval the library
     * keeps its own sense for a lemma it already has and otherwise stores this one. {@code partOfSpeech} is the name of
     * a library part of speech and {@code ipa} may be empty.
     */
    public record NewWord(String lemma, String ipa, String partOfSpeech, String englishDefinition,
                          String vietnameseMeaning, String exampleSentence) {

        /** The lemma as the library compares it: trimmed and in lower case. */
        public String normalizedLemma() {
            return lemma == null ? "" : lemma.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * A practice set the model wrote for a lesson that had none: a passage and choice questions about it, on the
     * given knowledge points of the lesson. Approval creates a draft package, section, passage and draft questions;
     * nothing is published.
     */
    public record PracticeSetPlan(String title, String instructions, String passage, QuestionType questionType,
                                  List<UUID> knowledgePointIds, List<QuestionPlan> questions) {
        public PracticeSetPlan {
            knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
            questions = questions == null ? List.of() : List.copyOf(questions);
        }
    }

    /**
     * A choice question of a proposed set. {@code correctKey} is the {@code optionKey} of the right answer;
     * {@code knowledgePointIds} are the points of the set this question tests, and none means all of the set's points.
     */
    public record QuestionPlan(String stem, List<OptionPlan> options, String correctKey, String explanation,
                               QuestionDifficulty difficulty, List<UUID> knowledgePointIds) {
        public QuestionPlan {
            options = options == null ? List.of() : List.copyOf(options);
            knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
        }

        /** A question that tests every point of its set. */
        public QuestionPlan(String stem, List<OptionPlan> options, String correctKey, String explanation,
                            QuestionDifficulty difficulty) {
            this(stem, options, correctKey, explanation, difficulty, List.of());
        }
    }

    public record OptionPlan(String optionKey, String content) {
    }

    /** A slide or document of a lesson: a title and the link to open. */
    public record ResourcePlan(com.ieltspath.content.domain.vo.LessonResourceType type, String title, String url) {
    }

    /** {@code type} is {@code TEXT} or {@code EXERCISE}; an exercise lists question references. */
    public record BlockPlan(BlockType type, String text, List<UUID> knowledgePointIds, List<String> questionRefs) {
        public BlockPlan {
            knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
            questionRefs = questionRefs == null ? List.of() : List.copyOf(questionRefs);
        }
    }

    public record FinalTestPlan(String code, String title) {
    }

    public record MilestonePlan(UUID packageId, int afterTopicSequence) {
    }

    public record GapNote(UUID knowledgePointId, String reason) {
    }

    /** All lessons in learning order: topics by sort order, then the order of the list. */
    public List<LessonPlan> lessonsInOrder() {
        return topicsInOrder().stream().flatMap(topic -> topic.lessons().stream()).toList();
    }

    public List<TopicPlan> topicsInOrder() {
        return topics.stream()
                .sorted(java.util.Comparator.comparingInt(topic -> topic.sortOrder() == null ? Integer.MAX_VALUE
                        : topic.sortOrder()))
                .toList();
    }
}
