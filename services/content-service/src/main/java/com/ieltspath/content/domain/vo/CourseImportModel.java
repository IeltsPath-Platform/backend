package com.ieltspath.content.domain.vo;

import java.util.List;
import java.util.Map;

/**
 * What a course import workbook says, as plain data: the template version and, for each sheet, its data rows. A cell is
 * its trimmed text; an empty cell is the empty string. Nothing here depends on how the file was read.
 */
public record CourseImportModel(String templateVersion, Map<String, List<Row>> sheets) {

    public static final String INFO = "Info";
    public static final String COURSE = "Course";
    public static final String TOPICS = "Topics";
    public static final String LESSONS = "Lessons";
    public static final String BLOCKS = "Blocks";
    public static final String QUESTIONS = "Questions";
    public static final String PRACTICE_SETS = "PracticeSets";
    public static final String TESTS = "Tests";

    public CourseImportModel {
        sheets = Map.copyOf(sheets);
    }

    public List<Row> rows(String sheet) {
        return sheets.getOrDefault(sheet, List.of());
    }

    /** One data row. {@code number} is the Excel row number (the first data row is 3). */
    public record Row(int number, Map<String, String> cells) {
        public Row {
            cells = Map.copyOf(cells);
        }

        public String get(String column) {
            return cells.getOrDefault(column, "");
        }

        public boolean blank() {
            return cells.values().stream().allMatch(String::isEmpty);
        }
    }

    /** A problem found in the workbook. {@code row} is 0 for a problem of the whole file; {@code column} may be empty. */
    public record Issue(String sheet, int row, String column, String code, String message) {
    }
}
