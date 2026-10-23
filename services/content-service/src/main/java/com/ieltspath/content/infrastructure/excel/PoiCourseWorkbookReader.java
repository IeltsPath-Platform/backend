package com.ieltspath.content.infrastructure.excel;

import com.ieltspath.content.application.port.CourseWorkbookReader;
import com.ieltspath.content.domain.vo.CourseImportModel;
import com.ieltspath.content.domain.vo.CourseImportModel.Issue;
import com.ieltspath.content.domain.vo.CourseImportModel.Row;
import com.ieltspath.content.domain.vo.CourseImportTemplate;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reads the course import workbook with Apache POI. Only the shapes the template defines are accepted. The file is
 * untrusted, so decompression is bounded, formulas and macros are refused, and rows and cell sizes are limited. Cell
 * contents never reach a log or an error message.
 */
@Component
public class PoiCourseWorkbookReader implements CourseWorkbookReader {

    public static final int MAX_DATA_ROWS = 2000;
    public static final int MAX_CELL_CHARACTERS = 20_000;
    private static final int FIRST_DATA_ROW_INDEX = 2; // row 1 titles, row 2 descriptions, data from row 3
    private static final Pattern MACRO_PART = Pattern.compile(".*vbaProject.*", Pattern.CASE_INSENSITIVE);
    private static final Issue UNREADABLE = new Issue("", 0, "", "UNREADABLE_FILE",
            "The file could not be read as an Excel workbook.");

    static {
        // A workbook that expands past these bounds is refused instead of exhausting memory.
        ZipSecureFile.setMinInflateRatio(0.01);
        ZipSecureFile.setMaxEntrySize(50L * 1024 * 1024);
        ZipSecureFile.setMaxTextSize(20L * 1024 * 1024);
    }

    @Override
    public ReadResult read(InputStream content) throws IOException {
        List<Issue> issues = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(content)) {
            if (!workbook.getPackage().getPartsByName(MACRO_PART).isEmpty()) {
                return new ReadResult(null, List.of(new Issue("", 0, "", "MACRO_NOT_ALLOWED",
                        "The workbook contains macros.")));
            }
            String version = version(workbook, issues);
            Map<String, List<Row>> sheets = new LinkedHashMap<>();
            int[] rowBudget = {MAX_DATA_ROWS};
            for (String name : CourseImportTemplate.SHEETS.keySet()) {
                Sheet sheet = workbook.getSheet(name);
                if (sheet == null) {
                    issues.add(new Issue(name, 0, "", "SHEET_MISSING", "The sheet " + name + " is missing."));
                    continue;
                }
                sheets.put(name, readSheet(name, CourseImportTemplate.columnNames(name), sheet, rowBudget, issues));
            }
            return new ReadResult(new CourseImportModel(version, sheets), issues);
        } catch (IOException | RuntimeException unreadable) {
            // Not a workbook, damaged, or a decompression bound was hit. The cause may quote the file, so it is not used.
            return new ReadResult(null, List.of(UNREADABLE));
        }
    }

    private String version(XSSFWorkbook workbook, List<Issue> issues) {
        Sheet info = workbook.getSheet(CourseImportModel.INFO);
        String found = "";
        if (info != null) {
            for (int index = 0; index <= info.getLastRowNum(); index++) {
                var row = info.getRow(index);
                if (row != null && "templateVersion".equals(text(row.getCell(0), "", 0, "", null))) {
                    found = text(row.getCell(1), "", 0, "", null);
                }
            }
        }
        if (!CourseImportTemplate.VERSION.equals(found)) {
            issues.add(new Issue(CourseImportModel.INFO, 0, "", "TEMPLATE_VERSION_MISMATCH",
                    "The workbook does not use template version " + CourseImportTemplate.VERSION + "."));
        }
        return found;
    }

    private List<Row> readSheet(String name, List<String> required, Sheet sheet, int[] rowBudget, List<Issue> issues) {
        Map<Integer, String> titles = new HashMap<>();
        var header = sheet.getRow(0);
        if (header != null) {
            for (Cell cell : header) {
                String title = text(cell, "", 0, "", null);
                if (!title.isEmpty()) titles.put(cell.getColumnIndex(), title);
            }
        }
        for (String column : required) {
            if (!titles.containsValue(column)) {
                issues.add(new Issue(name, 1, column, "COLUMN_MISSING", "The column " + column + " is missing."));
            }
        }
        List<Row> rows = new ArrayList<>();
        for (int index = FIRST_DATA_ROW_INDEX; index <= sheet.getLastRowNum(); index++) {
            var row = sheet.getRow(index);
            if (row == null) continue;
            Map<String, String> cells = new HashMap<>();
            for (Map.Entry<Integer, String> title : titles.entrySet()) {
                cells.put(title.getValue(), text(row.getCell(title.getKey()), name, index + 1, title.getValue(), issues));
            }
            Row data = new Row(index + 1, cells);
            if (data.blank()) continue;
            if (rowBudget[0]-- <= 0) {
                issues.add(new Issue(name, index + 1, "", "TOO_MANY_ROWS",
                        "The workbook has more than " + MAX_DATA_ROWS + " data rows."));
                return rows;
            }
            rows.add(data);
        }
        return rows;
    }

    /**
     * A cell as trimmed, normalised text. A formula, an error value or an oversize text is reported to {@code issues}
     * when it is not null; a formula and an error value read as empty.
     */
    private static String text(Cell cell, String sheet, int row, String column, List<Issue> issues) {
        if (cell == null) return "";
        String value;
        switch (cell.getCellType()) {
            case STRING -> value = cell.getStringCellValue();
            case NUMERIC -> value = number(cell.getNumericCellValue());
            case BOOLEAN -> value = Boolean.toString(cell.getBooleanCellValue());
            case FORMULA -> {
                if (issues != null) issues.add(new Issue(sheet, row, column, "FORMULA_NOT_ALLOWED", "A cell holds a formula."));
                return "";
            }
            case ERROR -> {
                if (issues != null) issues.add(new Issue(sheet, row, column, "INVALID_VALUE", "A cell holds an error value."));
                return "";
            }
            default -> value = "";
        }
        String clean = Normalizer.normalize(value, Normalizer.Form.NFC).replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").strip();
        if (clean.length() > MAX_CELL_CHARACTERS) {
            if (issues != null) {
                issues.add(new Issue(sheet, row, column, "CELL_TOO_LONG", "A cell is longer than "
                        + MAX_CELL_CHARACTERS + " characters."));
            }
            return clean.substring(0, MAX_CELL_CHARACTERS);
        }
        return clean;
    }

    private static String number(double value) {
        BigDecimal decimal = BigDecimal.valueOf(value).stripTrailingZeros();
        return decimal.scale() < 0 ? decimal.setScale(0).toPlainString() : decimal.toPlainString();
    }
}
