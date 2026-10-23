package com.ieltspath.content.infrastructure.excel;

import com.ieltspath.content.application.port.CourseWorkbookReader.ReadResult;
import com.ieltspath.content.domain.vo.CourseImportModel;
import com.ieltspath.content.domain.vo.CourseImportModel.Issue;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoiCourseWorkbookReaderTest {

    private final PoiCourseWorkbookReader reader = new PoiCourseWorkbookReader();

    private ReadResult read(byte[] file) throws Exception {
        return reader.read(new ByteArrayInputStream(file));
    }

    private static List<String> codes(ReadResult result) {
        return result.issues().stream().map(Issue::code).toList();
    }

    @Test
    void aFilledWorkbookIsReadRowByRowFromRowThree() throws Exception {
        byte[] file = TestWorkbooks.workbook()
                .row("Course", "code", "C1", "name", "Course one", "bandLevel", 6.5)
                .row("Topics", "code", "T1", "name", "Topic", "skill", "READING", "sortOrder", 970)
                .row("Topics", "code", "T2", "name", "Second", "skill", "LISTENING", "sortOrder", 971)
                .build();

        ReadResult result = read(file);

        assertEquals(List.of(), result.issues());
        assertEquals("1", result.model().templateVersion());
        CourseImportModel.Row course = result.model().rows("Course").get(0);
        assertEquals(3, course.number());
        assertEquals("C1", course.get("code"));
        assertEquals("6.5", course.get("bandLevel"));
        assertEquals(List.of(3, 4), result.model().rows("Topics").stream().map(CourseImportModel.Row::number).toList());
        assertEquals("970", result.model().rows("Topics").get(0).get("sortOrder"));
        assertEquals("", result.model().rows("Topics").get(0).get("missingColumn"));
    }

    @Test
    void wholeNumbersAreReadWithoutADecimalPointAndTextIsTrimmed() throws Exception {
        byte[] file = TestWorkbooks.workbook()
                .row("Topics", "code", "  T1  ", "name", "Topic", "skill", "READING", "sortOrder", 12.0)
                .build();

        CourseImportModel.Row row = read(file).model().rows("Topics").get(0);

        assertEquals("T1", row.get("code"));
        assertEquals("12", row.get("sortOrder"));
    }

    @Test
    void blankRowsAreSkipped() throws Exception {
        byte[] file = TestWorkbooks.workbook()
                .row("Topics", "code", "T1", "name", "A", "skill", "READING", "sortOrder", 1)
                .row("Topics")
                .row("Topics", "code", "T2", "name", "B", "skill", "READING", "sortOrder", 2)
                .build();

        assertEquals(2, read(file).model().rows("Topics").size());
    }

    @Test
    void aMissingSheetIsReported() throws Exception {
        ReadResult result = read(TestWorkbooks.workbook().omitSheet("Tests").build());

        assertEquals(List.of(new Issue("Tests", 0, "", "SHEET_MISSING", "The sheet Tests is missing.")),
                result.issues());
    }

    @Test
    void aMissingColumnIsReportedWithItsSheetAndName() throws Exception {
        ReadResult result = read(TestWorkbooks.workbook().omitColumn("Questions.explanation").build());

        assertEquals(1, result.issues().size());
        assertEquals("Questions", result.issues().get(0).sheet());
        assertEquals("explanation", result.issues().get(0).column());
        assertEquals("COLUMN_MISSING", result.issues().get(0).code());
        assertEquals(1, result.issues().get(0).row());
    }

    @Test
    void anotherTemplateVersionIsReported() throws Exception {
        assertEquals(List.of("TEMPLATE_VERSION_MISMATCH"), codes(read(TestWorkbooks.workbook().version("2").build())));
    }

    @Test
    void aFormulaIsRefusedWithItsPlaceAndNeverEvaluated() throws Exception {
        byte[] file = TestWorkbooks.workbook()
                .row("Topics", "code", "T1", "name", "=1+1", "skill", "READING", "sortOrder", 1)
                .build();

        ReadResult result = read(file);

        assertEquals(List.of(new Issue("Topics", 3, "name", "FORMULA_NOT_ALLOWED", "A cell holds a formula.")),
                result.issues());
        assertEquals("", result.model().rows("Topics").get(0).get("name"));
    }

    @Test
    void aCellOverTheLimitIsReportedAndCut() throws Exception {
        byte[] file = TestWorkbooks.workbook()
                .row("Questions", "importRef", "Q1", "stem", "s".repeat(PoiCourseWorkbookReader.MAX_CELL_CHARACTERS + 1))
                .build();

        ReadResult result = read(file);

        assertEquals(List.of("CELL_TOO_LONG"), codes(result));
        assertEquals(PoiCourseWorkbookReader.MAX_CELL_CHARACTERS,
                result.model().rows("Questions").get(0).get("stem").length());
    }

    @Test
    void moreRowsThanTheLimitStopTheReading() throws Exception {
        TestWorkbooks.Builder builder = TestWorkbooks.workbook();
        for (int i = 0; i < PoiCourseWorkbookReader.MAX_DATA_ROWS + 5; i++) {
            builder.row("Lessons", "code", "L" + i, "topicCode", "T", "title", "t");
        }

        ReadResult result = read(builder.build());

        assertEquals(List.of("TOO_MANY_ROWS"), codes(result));
        assertEquals(PoiCourseWorkbookReader.MAX_DATA_ROWS, result.model().rows("Lessons").size());
    }

    @Test
    void theRowLimitCountsAllSheetsTogether() throws Exception {
        TestWorkbooks.Builder builder = TestWorkbooks.workbook();
        for (int i = 0; i < 1500; i++) builder.row("Lessons", "code", "L" + i, "topicCode", "T", "title", "t");
        for (int i = 0; i < 600; i++) builder.row("Blocks", "lessonCode", "L1", "type", "TEXT");

        assertEquals(List.of("TOO_MANY_ROWS"), codes(read(builder.build())));
    }

    @Test
    void aFileThatIsNotAWorkbookIsUnreadable() throws Exception {
        ReadResult text = read("name,code\nx,y".getBytes());
        ReadResult empty = read(new byte[0]);
        ReadResult truncated = read(Arrays.copyOf(TestWorkbooks.template(), 200));

        for (ReadResult result : List.of(text, empty, truncated)) {
            assertNull(result.model());
            assertEquals(List.of("UNREADABLE_FILE"), codes(result));
        }
    }

    @Test
    void aWorkbookWithMacrosIsRefused() throws Exception {
        byte[] withMacro;
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(TestWorkbooks.template()))) {
            workbook.getPackage().createPart(PackagingURIHelper.createPartName("/xl/vbaProject.bin"),
                    "application/vnd.ms-office.vbaProject").getOutputStream().write(new byte[]{1, 2, 3});
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            withMacro = out.toByteArray();
        }

        ReadResult result = read(withMacro);

        assertNull(result.model());
        assertEquals(List.of("MACRO_NOT_ALLOWED"), codes(result));
    }

    @Test
    void aZipBombIsRefusedInsteadOfExpanded() throws Exception {
        byte[] bomb = withHugeSheet(TestWorkbooks.template());
        assertTrue(bomb.length < 5 * 1024 * 1024, "the bomb is small on disk");

        ReadResult result = read(bomb);

        assertNull(result.model());
        assertEquals(List.of("UNREADABLE_FILE"), codes(result));
    }

    @Test
    void anIssueNeverQuotesACellOrAFile() throws Exception {
        byte[] file = TestWorkbooks.workbook()
                .row("Topics", "code", "SECRET-VALUE", "name", "=SECRET()", "skill", "READING", "sortOrder", 1).build();

        ReadResult result = read(file);

        assertNotNull(result.model());
        result.issues().forEach(issue -> assertTrue(!issue.message().contains("SECRET"), issue.message()));
    }

    /** The template with one worksheet replaced by 60 MB of blanks: tiny when zipped, huge when read. */
    private static byte[] withHugeSheet(byte[] workbook) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(workbook));
             ZipOutputStream zip = new ZipOutputStream(out)) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                zip.putNextEntry(new ZipEntry(entry.getName()));
                if (entry.getName().equals("xl/worksheets/sheet2.xml")) {
                    zip.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"
                            .getBytes());
                    byte[] blanks = new byte[1024 * 1024];
                    Arrays.fill(blanks, (byte) ' ');
                    for (int i = 0; i < 60; i++) zip.write(blanks);
                    zip.write("</sheetData></worksheet>".getBytes());
                } else {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
