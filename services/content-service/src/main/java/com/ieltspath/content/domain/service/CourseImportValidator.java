package com.ieltspath.content.domain.service;

import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import com.ieltspath.content.domain.vo.CourseDraftStructure.BlockPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.CourseInfo;
import com.ieltspath.content.domain.vo.CourseDraftStructure.LessonPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.TopicPlan;
import com.ieltspath.content.domain.vo.CourseImportModel;
import com.ieltspath.content.domain.vo.CourseImportModel.Issue;
import com.ieltspath.content.domain.vo.CourseImportModel.Row;
import com.ieltspath.content.domain.vo.CourseImportPlan;
import com.ieltspath.content.domain.vo.CourseImportPlan.Option;
import com.ieltspath.content.domain.vo.CourseImportPlan.PackagePlan;
import com.ieltspath.content.domain.vo.CourseImportPlan.QuestionPlan;
import com.ieltspath.content.domain.vo.ImportCatalogFacts;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.Skill;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Checks the rows of a course import workbook and, when they hold together, resolves them into an import plan. It
 * covers what the rows can say about themselves and about the codes in the catalogue; the knowledge tree checks (coverage,
 * prerequisite order, practice material) are made afterwards on the plan's structure. Reads no storage and no file.
 */
public class CourseImportValidator {

    public static final Set<QuestionType> SUPPORTED_TYPES = Set.of(QuestionType.MULTIPLE_CHOICE,
            QuestionType.TRUE_FALSE_NOT_GIVEN, QuestionType.FILL_IN_BLANK, QuestionType.SHORT_ANSWER);
    static final int MAX_OPTIONS = 6;
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    /** The outcome: the errors found, and the plan when there are none. */
    public record Validated(List<Issue> errors, CourseImportPlan plan, Summary summary) {
    }

    public record Summary(int topics, int lessons, int questions, int packages) {
    }

    private final List<Issue> errors = new ArrayList<>();

    private CourseImportValidator() {
    }

    /** One check collects the problems of its own run; nothing is kept between calls. */
    public static Validated validate(CourseImportModel model, ImportCatalogFacts facts) {
        return new CourseImportValidator().run(model, facts);
    }

    private Validated run(CourseImportModel model, ImportCatalogFacts facts) {
        CourseInfo course = course(model);
        Map<String, Row> topicRows = new LinkedHashMap<>();
        List<TopicDraft> topics = topics(model, topicRows);
        Map<String, Row> lessonRows = new LinkedHashMap<>();
        Map<String, QuestionPlan> questions = questions(model, facts);
        List<LessonDraft> lessons = lessons(model, topicRows.keySet(), lessonRows, facts);
        attachBlocks(model, lessons, lessonRows, questions, facts);
        List<PackagePlan> packages = packages(model, topicRows.keySet(), lessonRows.keySet(), questions, facts);

        Summary summary = new Summary(topics.size(), lessons.size(), questions.size(), packages.size());
        if (!errors.isEmpty()) {
            return new Validated(List.copyOf(errors), null, summary);
        }
        List<TopicPlan> plannedTopics = new ArrayList<>();
        for (TopicDraft topic : topics) {
            List<LessonPlan> plannedLessons = lessons.stream().filter(l -> l.topicCode.equals(topic.code))
                    .sorted(Comparator.comparingInt((LessonDraft l) -> l.order).thenComparingInt(l -> l.row))
                    .map(LessonDraft::plan).toList();
            plannedTopics.add(new TopicPlan(topic.code, topic.name, topic.skill, topic.sortOrder, plannedLessons, null));
        }
        CourseDraftStructure structure = new CourseDraftStructure(course, plannedTopics, null, List.of(), List.of());
        return new Validated(List.of(), new CourseImportPlan(structure, List.copyOf(questions.values()), packages,
                facts.knowledgePointIdsByCode()), summary);
    }

    // --- Course

    private CourseInfo course(CourseImportModel model) {
        List<Row> rows = model.rows(CourseImportModel.COURSE);
        if (rows.size() != 1) {
            add(CourseImportModel.COURSE, rows.isEmpty() ? 0 : rows.get(1).number(), "", rows.isEmpty() ? "REQUIRED"
                    : "DUPLICATE", rows.isEmpty() ? "The Course sheet needs one row." : "The Course sheet takes one row.");
            return null;
        }
        Row row = rows.get(0);
        String code = code(CourseImportModel.COURSE, row, "code");
        String name = text(CourseImportModel.COURSE, row, "name", 255);
        BigDecimal band = band(CourseImportModel.COURSE, row, "bandLevel");
        return code == null || name == null || band == null ? null : new CourseInfo(code, name, band);
    }

    // --- Topics

    private record TopicDraft(String code, String name, Skill skill, int sortOrder) {
    }

    private List<TopicDraft> topics(CourseImportModel model, Map<String, Row> rowsByCode) {
        List<TopicDraft> topics = new ArrayList<>();
        Set<Integer> sortOrders = new HashSet<>();
        List<Row> rows = model.rows(CourseImportModel.TOPICS);
        if (rows.isEmpty()) add(CourseImportModel.TOPICS, 0, "", "REQUIRED", "The Topics sheet needs at least one row.");
        for (Row row : rows) {
            String code = code(CourseImportModel.TOPICS, row, "code");
            String name = text(CourseImportModel.TOPICS, row, "name", 255);
            Skill skill = enumValue(CourseImportModel.TOPICS, row, "skill", Skill.class);
            Integer sortOrder = integer(CourseImportModel.TOPICS, row, "sortOrder", true);
            if (code != null && rowsByCode.put(code, row) != null) {
                add(CourseImportModel.TOPICS, row.number(), "code", "DUPLICATE", "This topic code is used twice.");
            }
            if (sortOrder != null && !sortOrders.add(sortOrder)) {
                add(CourseImportModel.TOPICS, row.number(), "sortOrder", "DUPLICATE", "This sort order is used twice.");
            }
            if (code != null && name != null && skill != null && sortOrder != null) {
                topics.add(new TopicDraft(code, name, skill, sortOrder));
            }
        }
        return topics;
    }

    // --- Lessons and blocks

    private static final class LessonDraft {
        final String code;
        final String topicCode;
        final String title;
        final String summary;
        final int order;
        final int row;
        final List<UUID> knowledgePoints;
        final List<BlockDraft> blocks = new ArrayList<>();

        LessonDraft(String code, String topicCode, String title, String summary, int order, int row,
                    List<UUID> knowledgePoints) {
            this.code = code;
            this.topicCode = topicCode;
            this.title = title;
            this.summary = summary;
            this.order = order;
            this.row = row;
            this.knowledgePoints = knowledgePoints;
        }

        LessonPlan plan() {
            List<BlockPlan> planned = blocks.stream()
                    .sorted(Comparator.comparingInt((BlockDraft b) -> b.order).thenComparingInt(b -> b.row))
                    .map(b -> b.plan).toList();
            return new LessonPlan(code, title, summary, knowledgePoints, planned, List.of());
        }
    }

    private record BlockDraft(int order, int row, BlockPlan plan) {
    }

    private List<LessonDraft> lessons(CourseImportModel model, Set<String> topicCodes, Map<String, Row> rowsByCode,
                                      ImportCatalogFacts facts) {
        List<LessonDraft> lessons = new ArrayList<>();
        List<Row> rows = model.rows(CourseImportModel.LESSONS);
        if (rows.isEmpty()) add(CourseImportModel.LESSONS, 0, "", "REQUIRED", "The Lessons sheet needs at least one row.");
        for (Row row : rows) {
            String code = code(CourseImportModel.LESSONS, row, "code");
            String topicCode = row.get("topicCode");
            String title = text(CourseImportModel.LESSONS, row, "title", 255);
            if (topicCode.isEmpty()) {
                add(CourseImportModel.LESSONS, row.number(), "topicCode", "REQUIRED", "A lesson needs its topic.");
            } else if (!topicCodes.contains(topicCode)) {
                add(CourseImportModel.LESSONS, row.number(), "topicCode", "UNKNOWN_REFERENCE",
                        "This topic code is not in the Topics sheet.");
            }
            String summary = row.get("summary");
            if (summary.length() > 2000) add(CourseImportModel.LESSONS, row.number(), "summary", "TOO_LONG",
                    "The summary is longer than 2000 characters.");
            Integer order = integer(CourseImportModel.LESSONS, row, "sortOrder", false);
            List<UUID> points = knowledgePoints(CourseImportModel.LESSONS, row, facts);
            if (code != null && rowsByCode.put(code, row) != null) {
                add(CourseImportModel.LESSONS, row.number(), "code", "DUPLICATE", "This lesson code is used twice.");
            }
            if (code != null && title != null && topicCodes.contains(topicCode)) {
                lessons.add(new LessonDraft(code, topicCode, title, summary.isEmpty() ? null : summary,
                        order == null ? row.number() : order, row.number(), points));
            }
        }
        return lessons;
    }

    private void attachBlocks(CourseImportModel model, List<LessonDraft> lessons, Map<String, Row> lessonRows,
                              Map<String, QuestionPlan> questions, ImportCatalogFacts facts) {
        Map<String, LessonDraft> byCode = new HashMap<>();
        lessons.forEach(lesson -> byCode.put(lesson.code, lesson));
        for (Row row : model.rows(CourseImportModel.BLOCKS)) {
            String lessonCode = row.get("lessonCode");
            if (!lessonRows.containsKey(lessonCode)) {
                add(CourseImportModel.BLOCKS, row.number(), "lessonCode", lessonCode.isEmpty() ? "REQUIRED"
                        : "UNKNOWN_REFERENCE", "This lesson code is not in the Lessons sheet.");
                continue;
            }
            Integer order = integer(CourseImportModel.BLOCKS, row, "sortOrder", false);
            BlockType type = blockType(row);
            if (type == null) continue;
            BlockPlan plan;
            if (type == BlockType.TEXT) {
                if (!row.get("questionRefs").isEmpty()) {
                    add(CourseImportModel.BLOCKS, row.number(), "questionRefs", "INVALID_VALUE",
                            "A TEXT block takes no questions.");
                }
                if (row.get("text").length() > 20000) {
                    add(CourseImportModel.BLOCKS, row.number(), "text", "TOO_LONG", "The text is too long.");
                }
                plan = new BlockPlan(BlockType.TEXT, row.get("text"), knowledgePoints(CourseImportModel.BLOCKS, row,
                        facts), List.of());
            } else {
                if (!row.get("knowledgePointCodes").isEmpty()) {
                    add(CourseImportModel.BLOCKS, row.number(), "knowledgePointCodes", "INVALID_VALUE",
                            "An EXERCISE block takes its knowledge points from its questions.");
                }
                if (!row.get("text").isEmpty()) {
                    add(CourseImportModel.BLOCKS, row.number(), "text", "INVALID_VALUE", "An EXERCISE block has no text.");
                }
                plan = new BlockPlan(BlockType.EXERCISE, null, List.of(),
                        questionRefs(CourseImportModel.BLOCKS, row, questions, facts, true));
            }
            LessonDraft lesson = byCode.get(lessonCode);
            if (lesson != null) {
                lesson.blocks.add(new BlockDraft(order == null ? row.number() : order, row.number(), plan));
            }
        }
    }

    private BlockType blockType(Row row) {
        String value = row.get("type");
        if (value.equals("TEXT")) return BlockType.TEXT;
        if (value.equals("EXERCISE")) return BlockType.EXERCISE;
        add(CourseImportModel.BLOCKS, row.number(), "type", value.isEmpty() ? "REQUIRED" : "INVALID_VALUE",
                "The type is TEXT or EXERCISE.");
        return null;
    }

    // --- Questions

    private Map<String, QuestionPlan> questions(CourseImportModel model, ImportCatalogFacts facts) {
        Map<String, QuestionPlan> plans = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (Row row : model.rows(CourseImportModel.QUESTIONS)) {
            String sheet = CourseImportModel.QUESTIONS;
            int before = errors.size();
            String importRef = code(sheet, row, "importRef");
            if (importRef != null) {
                if (!seen.add(importRef)) {
                    add(sheet, row.number(), "importRef", "DUPLICATE", "This import reference is used twice.");
                } else if (facts.existingImportRefs().contains(importRef)) {
                    add(sheet, row.number(), "importRef", "CODE_CONFLICT", "A question already has this import reference.");
                }
            }
            QuestionType type = questionType(row);
            Skill skill = enumValue(sheet, row, "skill", Skill.class);
            String stem = text(sheet, row, "stem", 4000);
            String explanation = row.get("explanation");
            if (explanation.isEmpty()) {
                add(sheet, row.number(), "explanation", "NO_EXPLANATION", "Every question needs an explanation.");
            } else if (explanation.length() > 4000) {
                add(sheet, row.number(), "explanation", "TOO_LONG", "The explanation is too long.");
            }
            QuestionDifficulty difficulty = difficulty(row);
            List<UUID> points = knowledgePoints(sheet, row, facts);
            if (points.isEmpty() && row.get("knowledgePointCodes").isEmpty()) {
                add(sheet, row.number(), "knowledgePointCodes", "REQUIRED", "A question needs at least one knowledge point.");
            }

            List<Option> options = new ArrayList<>();
            String correct = "";
            List<String> accepted = new ArrayList<>();
            Map<String, String> distractors = new LinkedHashMap<>();
            if (type != null && isChoice(type)) {
                for (char key = 'A'; key < 'A' + MAX_OPTIONS; key++) {
                    String content = row.get("option" + key);
                    if (!content.isEmpty()) options.add(new Option(String.valueOf(key), content));
                }
                correct = row.get("answer").toUpperCase(java.util.Locale.ROOT);
                String answer = correct;
                if (options.size() < 2) {
                    add(sheet, row.number(), "optionA", "NOT_ENOUGH_OPTIONS", "A choice question needs at least two options.");
                } else if (options.stream().noneMatch(option -> option.key().equals(answer))) {
                    add(sheet, row.number(), "answer", "ANSWER_NOT_IN_OPTIONS", "The answer is not one of the options.");
                }
                distractors(sheet, row, options, correct, facts, distractors);
            } else if (type != null) {
                for (String value : row.get("answer").split("\\|")) {
                    if (!value.isBlank()) accepted.add(value.strip());
                }
                if (accepted.isEmpty()) {
                    add(sheet, row.number(), "answer", "NO_ACCEPTED_ANSWER", "List at least one accepted answer.");
                }
                if (!row.get("distractorErrorCodes").isEmpty()) {
                    add(sheet, row.number(), "distractorErrorCodes", "INVALID_VALUE",
                            "Distractor error codes belong to choice questions.");
                }
            }
            if (errors.size() == before && importRef != null && type != null && skill != null && stem != null
                    && difficulty != null) {
                plans.put(importRef, new QuestionPlan(importRef, type, skill, stem, options, correct, accepted, distractors,
                        explanation, difficulty, points, row.number()));
            }
        }
        return plans;
    }

    private QuestionType questionType(Row row) {
        String value = row.get("type");
        if (value.isEmpty()) {
            add(CourseImportModel.QUESTIONS, row.number(), "type", "REQUIRED", "The question type is required.");
            return null;
        }
        try {
            QuestionType type = QuestionType.valueOf(value);
            if (SUPPORTED_TYPES.contains(type)) return type;
        } catch (IllegalArgumentException unknown) {
            // reported below
        }
        add(CourseImportModel.QUESTIONS, row.number(), "type", "UNSUPPORTED_TYPE",
                "The type is MULTIPLE_CHOICE, TRUE_FALSE_NOT_GIVEN, FILL_IN_BLANK or SHORT_ANSWER.");
        return null;
    }

    private QuestionDifficulty difficulty(Row row) {
        String value = row.get("difficulty");
        if (value.isEmpty()) {
            add(CourseImportModel.QUESTIONS, row.number(), "difficulty", "DIFFICULTY_REQUIRED",
                    "Every question needs a difficulty.");
            return null;
        }
        try {
            return QuestionDifficulty.valueOf(value);
        } catch (IllegalArgumentException unknown) {
            add(CourseImportModel.QUESTIONS, row.number(), "difficulty", "INVALID_VALUE", "The difficulty is EASY, MEDIUM or HARD.");
            return null;
        }
    }

    private static boolean isChoice(QuestionType type) {
        return type == QuestionType.MULTIPLE_CHOICE || type == QuestionType.TRUE_FALSE_NOT_GIVEN;
    }

    /** {@code B=ERROR_CODE;C=ERROR_CODE}: each key is a wrong option, each code an active error type. */
    private void distractors(String sheet, Row row, List<Option> options, String correct, ImportCatalogFacts facts,
                             Map<String, String> into) {
        String text = row.get("distractorErrorCodes");
        if (text.isEmpty()) return;
        Set<String> keys = new HashSet<>();
        options.forEach(option -> keys.add(option.key()));
        for (String pair : text.split("[;,]")) {
            if (pair.isBlank()) continue;
            String[] parts = pair.split("=", 2);
            String key = parts[0].strip().toUpperCase(java.util.Locale.ROOT);
            String code = parts.length == 2 ? parts[1].strip() : "";
            if (!keys.contains(key) || key.equals(correct) || code.isEmpty() || into.containsKey(key)) {
                add(sheet, row.number(), "distractorErrorCodes", "INVALID_VALUE",
                        "Use B=ERROR_CODE;C=ERROR_CODE, naming each wrong option once.");
            } else if (!facts.activeErrorCodes().contains(code)) {
                add(sheet, row.number(), "distractorErrorCodes", "UNKNOWN_ERROR_CODE", "This error code is not an active one.");
            } else {
                into.put(key, code);
            }
        }
    }

    // --- Packages

    private List<PackagePlan> packages(CourseImportModel model, Set<String> topicCodes, Set<String> lessonCodes,
                                       Map<String, QuestionPlan> questions, ImportCatalogFacts facts) {
        List<PackagePlan> packages = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        for (Row row : model.rows(CourseImportModel.PRACTICE_SETS)) {
            String sheet = CourseImportModel.PRACTICE_SETS;
            String code = packageCode(sheet, row, codes, facts);
            String title = text(sheet, row, "title", 255);
            String lessonCode = row.get("lessonCode");
            if (!lessonCodes.contains(lessonCode)) {
                add(sheet, row.number(), "lessonCode", lessonCode.isEmpty() ? "REQUIRED" : "UNKNOWN_REFERENCE",
                        "This lesson code is not in the Lessons sheet.");
            }
            knowledgePoints(sheet, row, facts);
            List<String> refs = questionRefs(sheet, row, questions, facts, true);
            if (code != null && title != null && lessonCodes.contains(lessonCode)) {
                packages.add(new PackagePlan(PackageType.PRACTICE_SET, code, title, lessonCode, null, null, refs,
                        row.number()));
            }
        }
        boolean courseTest = false;
        for (Row row : model.rows(CourseImportModel.TESTS)) {
            String sheet = CourseImportModel.TESTS;
            String kind = row.get("kind");
            String code = packageCode(sheet, row, codes, facts);
            String title = text(sheet, row, "title", 255);
            List<String> refs = questionRefs(sheet, row, questions, facts, true);
            String topicCode = row.get("topicCode");
            Integer after = null;
            PackageType type = null;
            boolean placed = false;
            switch (kind) {
                case "TOPIC_TEST" -> {
                    type = PackageType.TOPIC_TEST;
                    placed = topicCodes.contains(topicCode);
                    if (!placed) {
                        add(sheet, row.number(), "topicCode", topicCode.isEmpty() ? "REQUIRED" : "UNKNOWN_REFERENCE",
                                "A TOPIC_TEST names a topic of the Topics sheet.");
                    }
                }
                case "COURSE_TEST" -> {
                    type = PackageType.COURSE_TEST;
                    placed = topicCode.isEmpty() && !courseTest;
                    if (!topicCode.isEmpty()) {
                        add(sheet, row.number(), "topicCode", "INVALID_VALUE", "A COURSE_TEST has no topicCode.");
                    }
                    if (courseTest) add(sheet, row.number(), "kind", "DUPLICATE", "The course has one COURSE_TEST.");
                    courseTest = true;
                }
                case "PRACTICE_TEST_MILESTONE" -> {
                    type = PackageType.PRACTICE_TEST;
                    after = integer(sheet, row, "afterTopicSequence", true);
                    placed = after != null && after >= 1 && after <= Math.max(1, topicCodes.size());
                    if (after != null && !placed) {
                        add(sheet, row.number(), "afterTopicSequence", "INVALID_VALUE",
                                "The position is from 1 to the number of topics.");
                    }
                }
                default -> add(sheet, row.number(), "kind", kind.isEmpty() ? "REQUIRED" : "INVALID_VALUE",
                        "The kind is TOPIC_TEST, COURSE_TEST or PRACTICE_TEST_MILESTONE.");
            }
            if (code != null && title != null && type != null && placed) {
                packages.add(new PackagePlan(type, code, title, null, type == PackageType.TOPIC_TEST ? topicCode : null,
                        after, refs, row.number()));
            }
        }
        return packages;
    }

    private String packageCode(String sheet, Row row, Set<String> seen, ImportCatalogFacts facts) {
        String code = code(sheet, row, "packageCode");
        if (code == null) return null;
        if (!seen.add(code)) {
            add(sheet, row.number(), "packageCode", "DUPLICATE", "This package code is used twice.");
        } else if (facts.takenPackageCodes().contains(code)) {
            add(sheet, row.number(), "packageCode", "CODE_CONFLICT", "A package already has this code.");
        }
        return code;
    }

    // --- Shared cell rules

    private List<String> questionRefs(String sheet, Row row, Map<String, QuestionPlan> inFile, ImportCatalogFacts facts,
                                      boolean required) {
        List<String> refs = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String ref : split(row.get("questionRefs"))) {
            if (!seen.add(ref)) {
                add(sheet, row.number(), "questionRefs", "DUPLICATE", "A question is listed twice in this row.");
            } else if (!inFile.containsKey(ref) && !facts.existingImportRefs().contains(ref)) {
                add(sheet, row.number(), "questionRefs", "UNKNOWN_QUESTION_REF",
                        "A listed question is neither in the Questions sheet nor an existing import reference.");
            } else {
                refs.add(ref);
            }
        }
        if (required && refs.isEmpty() && row.get("questionRefs").isEmpty()) {
            add(sheet, row.number(), "questionRefs", "REQUIRED", "List at least one question.");
        }
        return refs;
    }

    private List<UUID> knowledgePoints(String sheet, Row row, ImportCatalogFacts facts) {
        List<UUID> ids = new ArrayList<>();
        for (String code : split(row.get("knowledgePointCodes"))) {
            UUID id = facts.knowledgePointIdsByCode().get(code);
            if (id == null) {
                add(sheet, row.number(), "knowledgePointCodes", "UNKNOWN_KNOWLEDGE_POINT",
                        "A knowledge point code is not an active one.");
            } else if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private String code(String sheet, Row row, String column) {
        String value = row.get(column);
        if (value.isEmpty()) {
            add(sheet, row.number(), column, "REQUIRED", "This cell is required.");
            return null;
        }
        if (!CODE.matcher(value).matches()) {
            add(sheet, row.number(), column, "INVALID_VALUE", "Use letters, digits, dot, underscore and dash, up to 100.");
            return null;
        }
        return value;
    }

    private String text(String sheet, Row row, String column, int max) {
        String value = row.get(column);
        if (value.isEmpty()) {
            add(sheet, row.number(), column, "REQUIRED", "This cell is required.");
            return null;
        }
        if (value.length() > max) {
            add(sheet, row.number(), column, "TOO_LONG", "The text is longer than " + max + " characters.");
            return null;
        }
        return value;
    }

    private BigDecimal band(String sheet, Row row, String column) {
        String value = row.get(column);
        if (value.isEmpty()) {
            add(sheet, row.number(), column, "REQUIRED", "This cell is required.");
            return null;
        }
        try {
            BigDecimal band = new BigDecimal(value);
            if (band.signum() >= 0 && band.compareTo(new BigDecimal("9")) <= 0
                    && band.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() <= 0) {
                return band;
            }
        } catch (NumberFormatException notANumber) {
            // reported below
        }
        add(sheet, row.number(), column, "INVALID_VALUE", "The band is a half band from 0 to 9.");
        return null;
    }

    private Integer integer(String sheet, Row row, String column, boolean required) {
        String value = row.get(column);
        if (value.isEmpty()) {
            if (required) add(sheet, row.number(), column, "REQUIRED", "This cell is required.");
            return null;
        }
        try {
            int number = Integer.parseInt(value);
            if (number >= 0 && number <= 1_000_000) return number;
        } catch (NumberFormatException notANumber) {
            // reported below
        }
        add(sheet, row.number(), column, "INVALID_VALUE", "Use a whole number from 0 to 1000000.");
        return null;
    }

    private <E extends Enum<E>> E enumValue(String sheet, Row row, String column, Class<E> type) {
        String value = row.get(column);
        if (value.isEmpty()) {
            add(sheet, row.number(), column, "REQUIRED", "This cell is required.");
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException unknown) {
            add(sheet, row.number(), column, "INVALID_VALUE", "This value is not one the template allows.");
            return null;
        }
    }

    private static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        for (String part : text.split("[,;]")) {
            if (!part.isBlank()) parts.add(part.strip());
        }
        return parts;
    }

    private void add(String sheet, int row, String column, String code, String message) {
        errors.add(new Issue(sheet, row, column, code, message));
    }
}
