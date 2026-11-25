package com.ieltspath.content.infrastructure.excel;

import com.ieltspath.content.application.port.CourseWorkbookReader;
import com.ieltspath.content.application.usecase.DownloadImportTemplateUseCase;
import com.ieltspath.content.application.usecase.CodeSettingsUseCases;
import com.ieltspath.content.application.result.CodeSuggestion;
import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.CourseImportModel;
import com.ieltspath.content.domain.vo.CourseImportTemplate;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The template file that ships with the service must match the template definition. When the definition changes, run
 * this class once with {@code -DwriteImportTemplate=true} to write the file again.
 */
class ImportTemplateFileTest {

    private static final Path TEMPLATE = Path.of("src/main/resources/import/course-import-template-v1.xlsx");

    @Test
    void generatedDownloadAddsCatalogsAndReadOnlyCodeSuggestions() throws Exception {
        KnowledgePoint point = mock(KnowledgePoint.class);
        when(point.getCode()).thenReturn("KP_ACTIVE");
        when(point.getName()).thenReturn("Active point");
        when(point.getStatus()).thenReturn(ContentStatus.ACTIVE);
        ErrorType error = mock(ErrorType.class);
        when(error.getCode()).thenReturn("ERR_ACTIVE");
        when(error.getName()).thenReturn("Active error");
        when(error.getCategory()).thenReturn(com.ieltspath.content.domain.vo.ErrorCategory.GRAMMAR);
        KnowledgePointRepository points = mock(KnowledgePointRepository.class);
        ErrorTypeRepository errors = mock(ErrorTypeRepository.class);
        CodeSettingsUseCases settings = mock(CodeSettingsUseCases.class);
        when(points.findAll()).thenReturn(List.of(point));
        when(errors.findApprovedActive()).thenReturn(List.of(error));
        when(settings.suggestForImport(anyString(), anyString(), nullable(String.class), nullable(Skill.class),
                nullable(String.class), nullable(PackageType.class))).thenAnswer(call ->
                new CodeSuggestion(call.getArgument(0) + "_NEXT", true, false));

        byte[] file = new DownloadImportTemplateUseCase(points, errors, settings).execute();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            assertEquals("KP_ACTIVE", workbook.getSheet("KnowledgePoints").getRow(1).getCell(0).getStringCellValue());
            assertEquals("ERR_ACTIVE", workbook.getSheet("ErrorTypes").getRow(1).getCell(0).getStringCellValue());
            assertEquals("COURSE_NEXT", workbook.getSheet("Course").getRow(2).getCell(0).getStringCellValue());
            assertTrue(workbook.getSheet("Course").getRow(1).getCell(0).getStringCellValue().contains("COURSE_NEXT"));
        }
        CourseWorkbookReader.ReadResult read = new PoiCourseWorkbookReader().read(new ByteArrayInputStream(file));
        assertTrue(read.issues().isEmpty());
        assertEquals(Set.of("Course", "Topics", "Lessons", "Blocks", "Questions", "PracticeSets", "Tests"),
                read.model().sheets().keySet());
    }

    @Test
    @EnabledIfSystemProperty(named = "writeImportTemplate", matches = "true")
    void writeTheTemplateFile() throws Exception {
        Files.createDirectories(TEMPLATE.getParent());
        Files.write(TEMPLATE, TestWorkbooks.template());
    }

    @Test
    void theShippedFileHasEverySheetAndColumnOfTheDefinition() throws Exception {
        byte[] file = new DownloadImportTemplateUseCase().execute();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            assertEquals(CourseImportTemplate.VERSION, workbook.getSheet(CourseImportModel.INFO).getRow(1)
                    .getCell(1).getStringCellValue());
            for (var definition : CourseImportTemplate.SHEETS.entrySet()) {
                Sheet sheet = workbook.getSheet(definition.getKey());
                assertTrue(sheet != null, definition.getKey());
                List<String> titles = new java.util.ArrayList<>();
                sheet.getRow(0).forEach(cell -> titles.add(cell.getStringCellValue()));
                assertEquals(CourseImportTemplate.columnNames(definition.getKey()), titles, definition.getKey());
                // Row 2 describes each column and the drop-downs and notes are there.
                assertEquals(titles.size(), sheet.getRow(1).getPhysicalNumberOfCells(), definition.getKey());
            }
            Sheet questions = workbook.getSheet(CourseImportModel.QUESTIONS);
            assertTrue(questions.getDataValidations().size() >= 3, "type, skill and difficulty have drop-downs");
            assertTrue(questions.getRow(0).getCell(0).getCellComment() != null, "titles carry notes");
        }
    }

    @Test
    void theShippedFileIsAnEmptyButValidWorkbookForTheReader() throws Exception {
        CourseWorkbookReader.ReadResult read = new PoiCourseWorkbookReader().read(new ByteArrayInputStream(
                new DownloadImportTemplateUseCase().execute()));

        assertEquals(List.of(), read.issues());
        assertEquals(CourseImportTemplate.VERSION, read.model().templateVersion());
        read.model().sheets().values().forEach(rows -> assertTrue(rows.isEmpty(),
                "the description row is skipped, so the template holds no data"));
    }
}
