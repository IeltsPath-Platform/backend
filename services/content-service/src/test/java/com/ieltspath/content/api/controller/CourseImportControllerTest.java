package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.usecase.CourseImportUseCases;
import com.ieltspath.content.application.usecase.DownloadImportTemplateUseCase;
import com.ieltspath.content.domain.exception.CourseImportInvalidException;
import com.ieltspath.content.domain.exception.ImportFileRejectedException;
import com.ieltspath.content.domain.exception.ImportQuotaExceededException;
import com.ieltspath.content.domain.service.CourseImportValidator;
import com.ieltspath.content.domain.vo.CourseImportModel.Issue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The import routes: response shapes and the status each failure maps to. Security is tested separately. */
class CourseImportControllerTest {

    private static final String BASE = "/api/content/admin/course-import";

    private final DownloadImportTemplateUseCase template = mock(DownloadImportTemplateUseCase.class);
    private final CourseImportUseCases imports = mock(CourseImportUseCases.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final UUID author = UUID.randomUUID();
    private final MockMultipartFile workbook = new MockMultipartFile("file", "course.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{'P', 'K', 3, 4, 1});

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(currentUser.requireUserId()).thenReturn(author);
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseImportController(template, imports, currentUser))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void theTemplateIsADownloadableWorkbook() throws Exception {
        when(template.execute()).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get(BASE + "/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString(DownloadImportTemplateUseCase.FILE_NAME)))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void validateReturnsTheReport() throws Exception {
        when(imports.validate(eq("course.xlsx"), any())).thenReturn(new CourseImportUseCases.Report(
                List.of(new Issue("Lessons", 4, "knowledgePointCodes", "UNKNOWN_KNOWLEDGE_POINT", "Not active.")),
                List.of(new Issue("Lessons", 3, "knowledgePointCodes", "FEW_PRACTICE_SETS", "Little practice.")),
                new CourseImportValidator.Summary(2, 3, 4, 5), null));

        mockMvc.perform(multipart(BASE + "/validate").file(workbook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0].sheet").value("Lessons"))
                .andExpect(jsonPath("$.errors[0].row").value(4))
                .andExpect(jsonPath("$.errors[0].column").value("knowledgePointCodes"))
                .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_KNOWLEDGE_POINT"))
                .andExpect(jsonPath("$.warnings[0].code").value("FEW_PRACTICE_SETS"))
                .andExpect(jsonPath("$.summary.topics").value(2))
                .andExpect(jsonPath("$.summary.packages").value(5));
    }

    @Test
    void anUnreadableFileReportsNoSummary() throws Exception {
        when(imports.validate(eq("course.xlsx"), any())).thenReturn(new CourseImportUseCases.Report(
                List.of(new Issue("", 0, "", "UNREADABLE_FILE", "Not a workbook.")), List.of(), null, null));

        mockMvc.perform(multipart(BASE + "/validate").file(workbook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.summary").isEmpty());
    }

    @Test
    void importReturnsWhatItCreatedForTheCurrentAuthor() throws Exception {
        UUID course = UUID.randomUUID();
        UUID topic = UUID.randomUUID();
        UUID lesson = UUID.randomUUID();
        UUID question = UUID.randomUUID();
        UUID pack = UUID.randomUUID();
        when(imports.execute(eq(author), eq("course.xlsx"), any())).thenReturn(new CourseImportUseCases.Imported(
                course, List.of(topic), List.of(lesson), List.of(question), List.of(pack),
                List.of(new Issue("Lessons", 3, "knowledgePointCodes", "NO_PRACTICE_SET", "No practice."))));

        mockMvc.perform(multipart(BASE).file(workbook))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(course.toString()))
                .andExpect(jsonPath("$.topicIds[0]").value(topic.toString()))
                .andExpect(jsonPath("$.lessonIds[0]").value(lesson.toString()))
                .andExpect(jsonPath("$.questionIds[0]").value(question.toString()))
                .andExpect(jsonPath("$.packageIds[0]").value(pack.toString()))
                .andExpect(jsonPath("$.warnings[0].code").value("NO_PRACTICE_SET"));
    }

    @Test
    void anImportWithErrorsIsUnprocessableWithTheListAndNothingIsRead() throws Exception {
        when(imports.execute(eq(author), any(), any())).thenThrow(new CourseImportInvalidException(
                List.of(new Issue("Topics", 3, "code", "CODE_CONFLICT", "Used.")), List.of()));

        mockMvc.perform(multipart(BASE).file(workbook))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.code").value("COURSE_IMPORT_INVALID"))
                .andExpect(jsonPath("$.details.valid").value(false))
                .andExpect(jsonPath("$.details.errors[0].sheet").value("Topics"))
                .andExpect(jsonPath("$.details.errors[0].row").value(3));
    }

    @Test
    void theDailyAllowanceIsTooManyRequests() throws Exception {
        when(imports.execute(eq(author), any(), any())).thenThrow(new ImportQuotaExceededException());

        mockMvc.perform(multipart(BASE).file(workbook))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.details.code").value("IMPORT_QUOTA_EXCEEDED"));
    }

    @Test
    void aMissingOrEmptyFileIsBadRequest() throws Exception {
        mockMvc.perform(multipart(BASE + "/validate").file(new MockMultipartFile("file", "course.xlsx",
                        "application/octet-stream", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value(ImportFileRejectedException.NOT_XLSX));

        mockMvc.perform(multipart(BASE)).andExpect(status().isBadRequest());
        verifyNoInteractions(imports);
    }

    @Test
    void aFileOverTheSizeLimitIsRefusedBeforeItsBytesAreRead() throws Exception {
        MockMultipartFile large = mock(MockMultipartFile.class);
        when(large.getName()).thenReturn("file");
        when(large.getOriginalFilename()).thenReturn("course.xlsx");
        when(large.isEmpty()).thenReturn(false);
        when(large.getSize()).thenReturn(CourseImportUseCases.MAX_FILE_BYTES + 1);

        mockMvc.perform(multipart(BASE + "/validate").file(large))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.details.code").value(ImportFileRejectedException.FILE_TOO_LARGE));

        verifyNoInteractions(imports);
        verify(large, org.mockito.Mockito.never()).getBytes();
    }

    @Test
    void aRejectedFileFromTheUseCaseIsBadRequest() throws Exception {
        when(imports.validate(any(), any())).thenThrow(new ImportFileRejectedException(
                ImportFileRejectedException.NOT_XLSX));

        mockMvc.perform(multipart(BASE + "/validate").file(workbook))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value(ImportFileRejectedException.NOT_XLSX));
    }
}
