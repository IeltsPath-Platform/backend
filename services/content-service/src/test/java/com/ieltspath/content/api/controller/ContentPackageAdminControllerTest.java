package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.result.PackageVersionPreviewResult;
import com.ieltspath.content.application.usecase.CancelPackagePublicationScheduleUseCase;
import com.ieltspath.content.application.usecase.GetPackageVersionPreviewUseCase;
import com.ieltspath.content.application.usecase.ListPackagePublicationSchedulesUseCase;
import com.ieltspath.content.application.usecase.SchedulePackagePublicationUseCase;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContentPackageAdminControllerTest {
    @Mock GetPackageVersionPreviewUseCase preview;
    @Mock SchedulePackagePublicationUseCase schedule;
    @Mock CancelPackagePublicationScheduleUseCase cancel;
    @Mock ListPackagePublicationSchedulesUseCase listSchedules;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ContentPackageAdminController(preview, schedule, cancel,
                        listSchedules))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void previewKeepsAnswersAndChartFactsOutOfTheLearnerPayload() throws Exception {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID questionVersionId = UUID.randomUUID();
        when(preview.execute(packageId, versionId)).thenReturn(new PackageVersionPreviewResult(versionId, packageId,
                PackageType.PRACTICE_SET, null, "{}", List.of(new PackageVersionPreviewResult.Section(sectionId,
                "Task 1", Skill.WRITING, null, 1, null, null, List.of(new PackageVersionPreviewResult.Item(
                questionVersionId, 1, "Describe the chart", null, null, 5.0, List.of())))),
                List.of(new PackageVersionPreviewResult.SectionAnswers(sectionId, null, List.of(
                        new PackageVersionPreviewResult.ItemAnswer(questionVersionId,
                                "{\"type\":\"ESSAY\",\"chartFacts\":\"private chart facts\"}",
                                "Expected comparison", null))))));

        mockMvc.perform(get("/api/content/admin/packages/{id}/versions/{versionId}/preview", packageId, versionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[0].items[0].answerSpec").doesNotExist())
                .andExpect(jsonPath("$.sections[0].items[0].chartFacts").doesNotExist())
                .andExpect(jsonPath("$.answers[0].items[0].answerSpec.type").value("ESSAY"))
                .andExpect(jsonPath("$.answers[0].items[0].answerSpec.chartFacts").value("private chart facts"));
    }

    @Test
    void scheduleRejectsPastDateWithBadRequest() throws Exception {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        when(schedule.execute(eq(packageId), eq(versionId), any()))
                .thenThrow(new IllegalArgumentException("publishAt must be in the future"));

        mockMvc.perform(put("/api/content/admin/packages/{id}/versions/{versionId}/schedule", packageId, versionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishAt\":\"2000-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
        verify(schedule).execute(eq(packageId), eq(versionId), any(Instant.class));
    }
}
