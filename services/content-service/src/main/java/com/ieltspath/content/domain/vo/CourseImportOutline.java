package com.ieltspath.content.domain.vo;

import com.ieltspath.content.domain.vo.CourseImportModel.Row;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a workbook says about the course, grouped as the course will be shaped: topics with their lessons, practice
 * sets and tests. It is built from the raw rows, so it exists even when the file has errors, and every node keeps its
 * Excel row so an error can be shown where it belongs. Values are never validated here.
 */
public record CourseImportOutline(CourseNode course, List<TopicNode> topics, List<PackageNode> courseTests,
                                  List<PackageNode> milestones, List<QuestionNode> questions) {

    public record CourseNode(int row, String code, String name, String bandLevel, Map<String, String> cells) {
    }

    public record TopicNode(int row, String code, String name, String skill, String sortOrder,
                            List<LessonNode> lessons, List<PackageNode> tests, Map<String, String> cells) {
    }

    public record LessonNode(int row, String code, String title, List<String> knowledgePointCodes,
                             List<BlockNode> blocks, List<PackageNode> practiceSets, Map<String, String> cells) {
    }

    public record BlockNode(int row, String type, List<String> questionRefs, Map<String, String> cells) {
    }

    /** {@code sheet} is the workbook sheet of the row, {@code kind} the test kind or PRACTICE_SET. */
    public record PackageNode(String sheet, int row, String kind, String code, String title,
                              String afterTopicSequence, List<String> questionRefs, Map<String, String> cells) {
    }

    public record QuestionNode(int row, String importRef, String type, String skill, Map<String, String> cells) {
    }

    public static CourseImportOutline from(CourseImportModel model) {
        CourseNode course = model.rows(CourseImportModel.COURSE).stream().findFirst()
                .map(r -> new CourseNode(r.number(), r.get("code"), r.get("name"), r.get("bandLevel"), r.cells())).orElse(null);

        Map<String, List<Row>> blocksByLesson = group(model.rows(CourseImportModel.BLOCKS), "lessonCode");
        Map<String, List<Row>> setsByLesson = group(model.rows(CourseImportModel.PRACTICE_SETS), "lessonCode");
        List<Row> tests = model.rows(CourseImportModel.TESTS);
        Map<String, List<Row>> lessonsByTopic = group(model.rows(CourseImportModel.LESSONS), "topicCode");

        List<TopicNode> topics = new ArrayList<>();
        for (Row topic : model.rows(CourseImportModel.TOPICS)) {
            List<LessonNode> lessons = new ArrayList<>();
            for (Row lesson : ordered(lessonsByTopic.getOrDefault(topic.get("code"), List.of()))) {
                List<BlockNode> blocks = ordered(blocksByLesson.getOrDefault(lesson.get("code"), List.of())).stream()
                        .map(b -> new BlockNode(b.number(), b.get("type"), split(b.get("questionRefs")), b.cells())).toList();
                List<PackageNode> sets = setsByLesson.getOrDefault(lesson.get("code"), List.of()).stream()
                        .map(s -> new PackageNode(CourseImportModel.PRACTICE_SETS, s.number(), "PRACTICE_SET",
                                s.get("packageCode"), s.get("title"), "", split(s.get("questionRefs")), s.cells())).toList();
                lessons.add(new LessonNode(lesson.number(), lesson.get("code"), lesson.get("title"),
                        split(lesson.get("knowledgePointCodes")), blocks, sets, lesson.cells()));
            }
            List<PackageNode> topicTests = tests.stream()
                    .filter(t -> "TOPIC_TEST".equals(t.get("kind")) && topic.get("code").equals(t.get("topicCode")))
                    .map(CourseImportOutline::testNode).toList();
            topics.add(new TopicNode(topic.number(), topic.get("code"), topic.get("name"), topic.get("skill"),
                    topic.get("sortOrder"), lessons, topicTests, topic.cells()));
        }
        topics.sort(Comparator.comparingInt((TopicNode t) -> number(t.sortOrder())).thenComparingInt(TopicNode::row));

        List<PackageNode> courseTests = tests.stream().filter(t -> "COURSE_TEST".equals(t.get("kind")))
                .map(CourseImportOutline::testNode).toList();
        List<PackageNode> milestones = tests.stream().filter(t -> "PRACTICE_TEST_MILESTONE".equals(t.get("kind")))
                .map(CourseImportOutline::testNode).toList();
        List<QuestionNode> questions = model.rows(CourseImportModel.QUESTIONS).stream()
                .map(q -> new QuestionNode(q.number(), q.get("importRef"), q.get("type"), q.get("skill"), q.cells())).toList();
        return new CourseImportOutline(course, topics, courseTests, milestones, questions);
    }

    private static PackageNode testNode(Row t) {
        return new PackageNode(CourseImportModel.TESTS, t.number(), t.get("kind"), t.get("packageCode"),
                t.get("title"), t.get("afterTopicSequence"), split(t.get("questionRefs")), t.cells());
    }

    private static Map<String, List<Row>> group(List<Row> rows, String column) {
        Map<String, List<Row>> grouped = new LinkedHashMap<>();
        rows.forEach(r -> grouped.computeIfAbsent(r.get(column), k -> new ArrayList<>()).add(r));
        return grouped;
    }

    /** Rows by their sortOrder cell, then by row; a missing or malformed order keeps the row order. */
    private static List<Row> ordered(List<Row> rows) {
        return rows.stream().sorted(Comparator.comparingInt((Row r) -> {
            String value = r.get("sortOrder");
            return value.isEmpty() ? r.number() : number(value);
        }).thenComparingInt(Row::number)).toList();
    }

    private static int number(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException notANumber) {
            return Integer.MAX_VALUE;
        }
    }

    private static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        for (String part : text.split("[,;]")) {
            if (!part.isBlank()) parts.add(part.strip());
        }
        return parts;
    }
}
