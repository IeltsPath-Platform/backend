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

    public record LessonPlan(String code, String title, String summary, List<UUID> knowledgePointIds,
                             List<BlockPlan> blocks, List<UUID> practiceSetPackageIds) {
        public LessonPlan {
            knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
            blocks = blocks == null ? List.of() : List.copyOf(blocks);
            practiceSetPackageIds = practiceSetPackageIds == null ? List.of() : List.copyOf(practiceSetPackageIds);
        }

        /** Every knowledge point the lesson teaches: its own and those of its blocks. */
        public List<UUID> allKnowledgePointIds() {
            java.util.LinkedHashSet<UUID> all = new java.util.LinkedHashSet<>(knowledgePointIds);
            blocks.forEach(block -> all.addAll(block.knowledgePointIds()));
            return List.copyOf(all);
        }
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
