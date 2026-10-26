package com.ieltspath.content.domain.vo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The shape of the course import workbook: version 1 of its sheets and columns, with what each column means and the
 * values it may take. The reader checks against it, and the template file is written from it.
 */
public final class CourseImportTemplate {

    public static final String VERSION = "1";

    private CourseImportTemplate() {
    }

    /** {@code allowed} is the fixed set of values for a column, or empty when it is free text. */
    public record Column(String name, String description, String example, List<String> allowed) {
        static Column of(String name, String description, String example) {
            return new Column(name, description, example, List.of());
        }

        static Column of(String name, String description, String example, String... allowed) {
            return new Column(name, description, example, List.of(allowed));
        }
    }

    /** The data sheets in workbook order. The {@code Info} sheet is separate: it only holds the version. */
    public static final Map<String, List<Column>> SHEETS = sheets();

    public static List<String> columnNames(String sheet) {
        return SHEETS.get(sheet).stream().map(Column::name).toList();
    }

    private static Map<String, List<Column>> sheets() {
        Map<String, List<Column>> sheets = new LinkedHashMap<>();
        sheets.put(CourseImportModel.COURSE, List.of(
                Column.of("code", "Course code: letters, digits, . _ - (up to 100)", "IELTS_6_5_READING"),
                Column.of("name", "Course name", "IELTS 6.5 Reading"),
                Column.of("bandLevel", "Half band from 0 to 9; no other course may have it", "6.5")));
        sheets.put(CourseImportModel.TOPICS, List.of(
                Column.of("code", "Topic code, unique", "R65_PARAPHRASE"),
                Column.of("name", "Topic name", "Paraphrase"),
                Column.of("skill", "Skill of the topic", "READING", "LISTENING", "READING", "WRITING", "SPEAKING"),
                Column.of("sortOrder", "Position among all top-level topics; must be free", "970")));
        sheets.put(CourseImportModel.LESSONS, List.of(
                Column.of("code", "Lesson code, unique", "R65_PARA_L1"),
                Column.of("topicCode", "Code of a topic in the Topics sheet", "R65_PARAPHRASE"),
                Column.of("title", "Lesson title", "Spot the paraphrase"),
                Column.of("summary", "Short summary (optional, up to 2000 characters)", "How meaning survives rewording"),
                Column.of("sortOrder", "Order inside the topic (optional; row order if empty)", "1"),
                Column.of("knowledgePointCodes", "Active knowledge point codes, separated by , or ;", "R_PARA_1; R_PARA_2")));
        sheets.put(CourseImportModel.BLOCKS, List.of(
                Column.of("lessonCode", "Code of a lesson in the Lessons sheet", "R65_PARA_L1"),
                Column.of("sortOrder", "Order inside the lesson (optional; row order if empty)", "1"),
                Column.of("type", "TEXT carries text; EXERCISE lists questions", "EXERCISE", "TEXT", "EXERCISE"),
                Column.of("text", "Text of a TEXT block (may be empty)", ""),
                Column.of("questionRefs", "EXERCISE: importRef values, separated by , or ;", "Q1; Q2"),
                Column.of("knowledgePointCodes", "TEXT only: knowledge point codes", "R_PARA_1")));
        sheets.put(CourseImportModel.QUESTIONS, List.of(
                Column.of("importRef", "Your own unique reference: letters, digits, . _ - (up to 100)", "Q1"),
                Column.of("type", "Question type", "MULTIPLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE_NOT_GIVEN",
                        "FILL_IN_BLANK", "SHORT_ANSWER"),
                Column.of("skill", "Skill of the question", "READING", "LISTENING", "READING", "WRITING", "SPEAKING"),
                Column.of("stem", "The question text", "Which sentence keeps the meaning?"),
                Column.of("optionA", "Choice questions: option A", "He arrived late."),
                Column.of("optionB", "Option B", "He came after the start."),
                Column.of("optionC", "Option C", ""),
                Column.of("optionD", "Option D", ""),
                Column.of("optionE", "Option E", ""),
                Column.of("optionF", "Option F", ""),
                Column.of("answer", "Choice: the right letter. Typed: every accepted answer, separated by |", "B"),
                Column.of("explanation", "Why the answer is right (required)", "B repeats the idea in other words."),
                Column.of("difficulty", "Difficulty", "MEDIUM", "EASY", "MEDIUM", "HARD"),
                Column.of("knowledgePointCodes", "Active knowledge point codes this question measures", "R_PARA_1"),
                Column.of("distractorErrorCodes", "Choice: wrong option and error code, like A=ERROR_CODE;C=ERROR_CODE", "")));
        sheets.put(CourseImportModel.PRACTICE_SETS, List.of(
                Column.of("packageCode", "Package code, unique across the workbook and existing packages", "R65_PS_1"),
                Column.of("title", "Package title", "Paraphrase practice 1"),
                Column.of("lessonCode", "Code of the lesson it belongs to", "R65_PARA_L1"),
                Column.of("knowledgePointCodes", "For your own notes; the questions decide what is measured", ""),
                Column.of("questionRefs", "importRef values in the order they appear", "Q1; Q2")));
        sheets.put(CourseImportModel.TESTS, List.of(
                Column.of("kind", "Kind of test", "TOPIC_TEST", "TOPIC_TEST", "COURSE_TEST", "PRACTICE_TEST_MILESTONE"),
                Column.of("packageCode", "Package code, unique", "R65_TT_1"),
                Column.of("title", "Package title", "Paraphrase final test"),
                Column.of("topicCode", "TOPIC_TEST only: code of a topic in the Topics sheet", "R65_PARAPHRASE"),
                Column.of("afterTopicSequence", "PRACTICE_TEST_MILESTONE only: after topic number (1 up to the topic count)", ""),
                Column.of("questionRefs", "importRef values in the order they appear", "Q1; Q2")));
        return java.util.Collections.unmodifiableMap(sheets);
    }
}
