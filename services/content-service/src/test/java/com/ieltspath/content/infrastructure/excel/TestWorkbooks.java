package com.ieltspath.content.infrastructure.excel;

import com.ieltspath.content.domain.vo.CourseImportModel;
import com.ieltspath.content.domain.vo.CourseImportTemplate;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds workbooks for tests: the shipped template (also how its file is written) and filled workbooks whose rows,
 * versions and odd cells (formulas, long text) a test chooses.
 */
public final class TestWorkbooks {

    private TestWorkbooks() {
    }

    /** The empty template: titles, a description with an example, notes and drop-downs. */
    public static byte[] template() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet info = workbook.createSheet(CourseImportModel.INFO);
            info.createRow(0).createCell(0).setCellValue("key");
            info.getRow(0).createCell(1).setCellValue("value");
            info.createRow(1).createCell(0).setCellValue("templateVersion");
            info.getRow(1).createCell(1).setCellValue(CourseImportTemplate.VERSION);
            info.createRow(3).createCell(0).setCellValue(
                    "Fill one sheet row per item. Row 1 holds the titles and row 2 the descriptions; your data starts in row 3.");
            info.setColumnWidth(0, 22 * 256);
            info.setColumnWidth(1, 12 * 256);

            Font bold = workbook.createFont();
            bold.setBold(true);
            XSSFCellStyle header = workbook.createCellStyle();
            header.setFont(bold);
            XSSFCellStyle hint = workbook.createCellStyle();
            hint.setWrapText(true);
            Font italic = workbook.createFont();
            italic.setItalic(true);
            hint.setFont(italic);

            CreationHelper helper = workbook.getCreationHelper();
            for (var definition : CourseImportTemplate.SHEETS.entrySet()) {
                Sheet sheet = workbook.createSheet(definition.getKey());
                Drawing<?> drawing = sheet.createDrawingPatriarch();
                Row titles = sheet.createRow(0);
                Row hints = sheet.createRow(1);
                DataValidationHelper validation = sheet.getDataValidationHelper();
                int index = 0;
                for (CourseImportTemplate.Column column : definition.getValue()) {
                    Cell title = titles.createCell(index);
                    title.setCellValue(column.name());
                    title.setCellStyle(header);
                    Cell description = hints.createCell(index);
                    description.setCellValue(column.example().isEmpty() ? column.description()
                            : column.description() + " Example: " + column.example());
                    description.setCellStyle(hint);
                    ClientAnchor anchor = helper.createClientAnchor();
                    anchor.setCol1(index);
                    anchor.setCol2(index + 3);
                    anchor.setRow1(0);
                    anchor.setRow2(4);
                    Comment note = drawing.createCellComment(anchor);
                    note.setString(helper.createRichTextString(column.description()));
                    title.setCellComment(note);
                    if (!column.allowed().isEmpty()) {
                        DataValidationConstraint allowed = validation.createExplicitListConstraint(
                                column.allowed().toArray(String[]::new));
                        DataValidation list = validation.createValidation(allowed,
                                new CellRangeAddressList(2, 1000, index, index));
                        list.setShowErrorBox(true);
                        sheet.addValidationData(list);
                    }
                    sheet.setColumnWidth(index, Math.min(48, Math.max(16, column.name().length() + 4)) * 256);
                    index++;
                }
                hints.setHeightInPoints(60);
                sheet.createFreezePane(0, 2);
            }
            return write(workbook);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public static Builder workbook() {
        return new Builder();
    }

    /** Fills sheets row by row. Rows start at Excel row 3 after the title and description rows. */
    public static final class Builder {
        private String version = CourseImportTemplate.VERSION;
        private final Map<String, List<Map<String, Object>>> rows = new LinkedHashMap<>();
        private final List<String> omittedSheets = new ArrayList<>();
        private final List<String> omittedColumns = new ArrayList<>();

        public Builder version(String value) {
            this.version = value;
            return this;
        }

        public Builder omitSheet(String sheet) {
            omittedSheets.add(sheet);
            return this;
        }

        /** Leaves a column out of the sheet, as {@code sheet.column}. */
        public Builder omitColumn(String sheetAndColumn) {
            omittedColumns.add(sheetAndColumn);
            return this;
        }

        /** A value is a String or a Number; a String starting with {@code =} is written as a formula. */
        public Builder row(String sheet, Object... pairs) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < pairs.length; i += 2) row.put((String) pairs[i], pairs[i + 1]);
            rows.computeIfAbsent(sheet, ignored -> new ArrayList<>()).add(row);
            return this;
        }

        public byte[] build() {
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet info = workbook.createSheet(CourseImportModel.INFO);
                info.createRow(1).createCell(0).setCellValue("templateVersion");
                info.getRow(1).createCell(1).setCellValue(version);
                for (var definition : CourseImportTemplate.SHEETS.entrySet()) {
                    if (omittedSheets.contains(definition.getKey())) continue;
                    Sheet sheet = workbook.createSheet(definition.getKey());
                    Row titles = sheet.createRow(0);
                    sheet.createRow(1).createCell(0).setCellValue("description row, skipped");
                    List<String> columns = new ArrayList<>();
                    for (CourseImportTemplate.Column column : definition.getValue()) {
                        if (!omittedColumns.contains(definition.getKey() + "." + column.name())) {
                            titles.createCell(columns.size()).setCellValue(column.name());
                            columns.add(column.name());
                        }
                    }
                    int rowIndex = 2;
                    for (Map<String, Object> values : rows.getOrDefault(definition.getKey(), List.of())) {
                        Row row = sheet.createRow(rowIndex++);
                        for (int c = 0; c < columns.size(); c++) {
                            Object value = values.get(columns.get(c));
                            if (value == null) continue;
                            Cell cell = row.createCell(c);
                            if (value instanceof Number number) {
                                cell.setCellValue(number.doubleValue());
                            } else if (value.toString().startsWith("=")) {
                                cell.setCellFormula(value.toString().substring(1));
                            } else {
                                cell.setCellValue(value.toString());
                            }
                        }
                    }
                }
                return write(workbook);
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }
    }

    private static byte[] write(XSSFWorkbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}
