package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.PackageType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** Builds a workbook from the maintained template and current read-only catalog data. */
@Service
public class DownloadImportTemplateUseCase {

    public static final String FILE_NAME = "course-import-template-v1.xlsx";
    static final String RESOURCE = "import/" + FILE_NAME;
    private final KnowledgePointRepository knowledgePoints;
    private final ErrorTypeRepository errorTypes;
    private final CodeSettingsUseCases codeSettings;

    public DownloadImportTemplateUseCase() {
        this(null, null, null);
    }

    @Autowired
    public DownloadImportTemplateUseCase(KnowledgePointRepository knowledgePoints, ErrorTypeRepository errorTypes, CodeSettingsUseCases codeSettings) {
        this.knowledgePoints = knowledgePoints;
        this.errorTypes = errorTypes;
        this.codeSettings = codeSettings;
    }

    public byte[] execute() {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (knowledgePoints != null && errorTypes != null) appendCatalogs(workbook);
            if (codeSettings != null) addCodeSuggestions(workbook);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException missing) {
            throw new UncheckedIOException("Không thể tạo tệp mẫu nhập khóa học", missing);
        }
    }

    private void addCodeSuggestions(XSSFWorkbook workbook) {
        String course = suggestion("COURSE", "6.5", null, null, null, null);
        String topic = suggestion("TOPIC", "6.5", course, Skill.READING, null, null);
        String lesson = suggestion("LESSON", "6.5", course, Skill.READING, topic, null);
        putSuggestion(workbook, "Course", "code", course);
        putSuggestion(workbook, "Topics", "code", topic);
        putSuggestion(workbook, "Lessons", "code", lesson);
        putSuggestion(workbook, "PracticeSets", "packageCode",
                suggestion("PACKAGE", "6.5", course, Skill.READING, topic, PackageType.PRACTICE_SET));
        putSuggestion(workbook, "Tests", "packageCode",
                suggestion("PACKAGE", "6.5", course, Skill.READING, topic, PackageType.TOPIC_TEST));
    }

    private String suggestion(String kind, String band, String course, Skill skill, String topic, PackageType type) {
        try {
            return codeSettings.suggestForImport(kind, band, course, skill, topic, type).code();
        } catch (IllegalArgumentException noSetting) {
            return "";
        }
    }

    private static void putSuggestion(XSSFWorkbook workbook, String sheetName, String columnName, String code) {
        if (code == null || code.isBlank()) return;
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) return;
        Row header = sheet.getRow(0);
        int column = -1;
        for (int i = 0; i < header.getLastCellNum(); i++) {
            if (columnName.equals(header.getCell(i).getStringCellValue())) { column = i; break; }
        }
        if (column < 0) return;
        Row data = sheet.getRow(2);
        if (data == null) data = sheet.createRow(2);
        data.createCell(column).setCellValue(code);
        Row description = sheet.getRow(1);
        if (description != null && description.getCell(column) != null) {
            description.getCell(column).setCellValue(description.getCell(column).getStringCellValue()
                    + " Gợi ý mã tiếp theo: " + code + ".");
        }
    }

    private void appendCatalogs(XSSFWorkbook workbook) {
        Sheet kpSheet = workbook.createSheet("KnowledgePoints");
        Row kpHeader = kpSheet.createRow(0);
        String[] kpColumns = {"code", "name", "skill", "level", "status"};
        for (int i = 0; i < kpColumns.length; i++) kpHeader.createCell(i).setCellValue(kpColumns[i]);
        int rowNum = 1;
        for (KnowledgePoint point : knowledgePoints.findAll()) {
            if (point.getStatus() != ContentStatus.ACTIVE) continue;
            Row row = kpSheet.createRow(rowNum++);
            row.createCell(0).setCellValue(point.getCode());
            row.createCell(1).setCellValue(point.getName());
            row.createCell(2).setCellValue(point.getSkill() == null ? "" : point.getSkill().name());
            row.createCell(3).setCellValue(point.getCefrLevel() == null ? "" : point.getCefrLevel().name());
            row.createCell(4).setCellValue(point.getStatus().name());
        }

        Sheet errorSheet = workbook.createSheet("ErrorTypes");
        Row errorHeader = errorSheet.createRow(0);
        String[] errorColumns = {"code", "name", "group"};
        for (int i = 0; i < errorColumns.length; i++) errorHeader.createCell(i).setCellValue(errorColumns[i]);
        rowNum = 1;
        for (ErrorType type : errorTypes.findApprovedActive()) {
            Row row = errorSheet.createRow(rowNum++);
            row.createCell(0).setCellValue(type.getCode());
            row.createCell(1).setCellValue(type.getName());
            row.createCell(2).setCellValue(type.getCategory().name());
        }
    }
}
