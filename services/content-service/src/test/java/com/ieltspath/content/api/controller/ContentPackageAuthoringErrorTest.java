package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.usecase.AddSectionQuestionUseCase;
import com.ieltspath.content.application.usecase.RemoveAndReorderSectionQuestionsUseCase;
import com.ieltspath.content.application.usecase.CodeSettingsUseCases;
import com.ieltspath.content.application.usecase.CreateContentPackageUseCase;
import com.ieltspath.content.application.usecase.CreateContentSectionUseCase;
import com.ieltspath.content.application.usecase.ListAuthorPackagesUseCase;
import com.ieltspath.content.domain.exception.InvalidPackageCourseException;
import com.ieltspath.content.domain.exception.InvalidPackageTopicException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.application.usecase.CreateDraftPackageVersionUseCase;
import com.ieltspath.content.application.usecase.DeleteContentSectionUseCase;
import com.ieltspath.content.application.usecase.GetAuthorPackageUseCase;
import com.ieltspath.content.application.usecase.ReorderContentSectionsUseCase;
import com.ieltspath.content.application.usecase.UpdateContentSectionUseCase;
import com.ieltspath.content.domain.exception.ContentSectionNotFoundException;
import com.ieltspath.content.domain.exception.DraftVersionExistsException;
import com.ieltspath.content.domain.exception.VersionNotDraftException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Error mapping for the author routes. Security is covered by the web authorization test. */
class ContentPackageAuthoringErrorTest {

    private final CreateDraftPackageVersionUseCase createDraft = mock(CreateDraftPackageVersionUseCase.class);
    private final UpdateContentSectionUseCase updateSection = mock(UpdateContentSectionUseCase.class);
    private final ReorderContentSectionsUseCase reorderSections = mock(ReorderContentSectionsUseCase.class);
    private final CodeSettingsUseCases codeSettings = mock(CodeSettingsUseCases.class);

    private final ListAuthorPackagesUseCase listPackages = mock(ListAuthorPackagesUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ContentPackageAuthoringController(
                        mock(GetAuthorPackageUseCase.class), createDraft, mock(CreateContentSectionUseCase.class),
                        updateSection, mock(DeleteContentSectionUseCase.class), reorderSections,
                        mock(AddSectionQuestionUseCase.class), mock(RemoveAndReorderSectionQuestionsUseCase.class),
                        listPackages, codeSettings))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void authorListSerializesPoolInEachSummary() throws Exception {
        var summary = new com.ieltspath.content.application.result.AuthorPackageSummary(UUID.randomUUID(),
                "POOL", "Placement", com.ieltspath.content.domain.vo.PackageType.PLACEMENT_TEST, null,
                com.ieltspath.content.domain.vo.PublicationStatus.DRAFT, null, null, null, null, null,
                java.time.Instant.now(), java.time.Instant.now(), null, false,
                com.ieltspath.content.domain.vo.PlacementPool.BAND_3_5);
        when(listPackages.execute(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new com.ieltspath.content.application.usecase.ListAuthorPackagesUseCase.Page(1, 0, 25, java.util.List.of(summary)));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/content/admin/packages"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].placementPool").value("BAND_3_5"));
    }

    @Test
    void placementRequiresPoolAndOtherTypesRejectPool() throws Exception {
        for (String body : java.util.List.of(
                "{\"code\":\"P\",\"title\":\"Placement\",\"packageType\":\"PLACEMENT_TEST\"}",
                "{\"code\":\"P\",\"title\":\"Practice\",\"packageType\":\"PRACTICE_SET\",\"placementPool\":\"BAND_0_3\"}")) {
            mockMvc.perform(post("/api/content/admin/packages").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        org.mockito.Mockito.verifyNoInteractions(codeSettings);
    }

    @Test
    void blankCodeWithoutAppliedSettingReturnsVietnameseValidationError() throws Exception {
        when(codeSettings.createPackage(any()))
                .thenThrow(new IllegalArgumentException("Mã gói đề không được để trống."));

        mockMvc.perform(post("/api/content/admin/packages").contentType("application/json")
                        .content("{\"code\":\"\",\"title\":\"Practice\",\"packageType\":\"PRACTICE_SET\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mã gói đề không được để trống."));
        org.mockito.Mockito.verify(codeSettings).createPackage(any());
    }

    @Test
    void lockedPackageCodeReturnsConflict() throws Exception {
        when(codeSettings.createPackage(any())).thenThrow(InvalidContentStateException.codeLocked());

        mockMvc.perform(post("/api/content/admin/packages").contentType("application/json")
                        .content("{\"code\":\"MANUAL\",\"title\":\"Practice\",\"packageType\":\"PRACTICE_SET\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.code").value("CODE_LOCKED"));
    }

    @Test
    void placementPoolIsStoredAndReturned() throws Exception {
        var repository = mock(com.ieltspath.content.domain.repository.ContentPackageRepository.class);
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        var useCase = new CreateContentPackageUseCase(repository,
                mock(com.ieltspath.content.application.port.LearningContentReader.class));
        when(codeSettings.createPackage(any())).thenAnswer(call -> useCase.executeAsAuthor(call.getArgument(0)));
        mockMvc.perform(post("/api/content/admin/packages").contentType("application/json")
                        .content("{\"code\":\"P\",\"title\":\"Placement\",\"packageType\":\"PLACEMENT_TEST\",\"placementPool\":\"BAND_3_5\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.placementPool").value("BAND_3_5"));
        var saved = org.mockito.ArgumentCaptor.forClass(com.ieltspath.content.domain.aggregate.ContentPackage.class);
        org.mockito.Mockito.verify(repository).save(saved.capture());
        org.assertj.core.api.Assertions.assertThat(saved.getValue().getPlacementPool().name()).isEqualTo("BAND_3_5");
    }

    @Test
    void authorListForwardsPoolAndRejectsUnknownPool() throws Exception {
        when(listPackages.execute(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new com.ieltspath.content.application.usecase.ListAuthorPackagesUseCase.Page(0, 0, 25, java.util.List.of()));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/content/admin/packages")
                        .param("placementPool", "BAND_5_9")).andExpect(status().isOk());
        var filter = org.mockito.ArgumentCaptor.forClass(com.ieltspath.content.application.port.AuthorPackageReader.Filter.class);
        org.mockito.Mockito.verify(listPackages).execute(filter.capture(), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(25));
        org.assertj.core.api.Assertions.assertThat(filter.getValue().placementPool().name()).isEqualTo("BAND_5_9");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/content/admin/packages")
                        .param("placementPool", "UNKNOWN")).andExpect(status().isBadRequest());
    }

    @Test
    void finalTestWithoutItsTopicIsBadRequestWithItsCode() throws Exception {
        when(codeSettings.createPackage(any())).thenThrow(new InvalidPackageTopicException("topic missing"));

        mockMvc.perform(post("/api/content/admin/packages").contentType("application/json")
                        .content("{\"code\":\"X\",\"title\":\"T\",\"packageType\":\"TOPIC_TEST\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value("INVALID_PACKAGE_TOPIC"));
    }

    @Test
    void courseTestWithoutItsCourseIsBadRequestWithItsCode() throws Exception {
        when(codeSettings.createPackage(any())).thenThrow(new InvalidPackageCourseException("course missing"));

        mockMvc.perform(post("/api/content/admin/packages").contentType("application/json")
                        .content("{\"code\":\"X\",\"title\":\"T\",\"packageType\":\"COURSE_TEST\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value("INVALID_PACKAGE_COURSE"));
    }

    @Test
    void secondDraftReturnsConflictWithItsCode() throws Exception {
        when(createDraft.execute(any(), any())).thenThrow(new DraftVersionExistsException());

        mockMvc.perform(post("/api/content/admin/packages/{id}/versions", UUID.randomUUID())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.code").value("DRAFT_EXISTS"));
    }

    @Test
    void editingAPublishedVersionReturnsBadRequestWithItsCode() throws Exception {
        when(updateSection.execute(any())).thenThrow(new VersionNotDraftException());

        mockMvc.perform(put("/api/content/admin/packages/{p}/versions/{v}/sections/{s}",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
                        .contentType("application/json").content("{\"title\":\"Passage\",\"sortOrder\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value("VERSION_NOT_DRAFT"));
    }

    @Test
    void unknownSectionReturnsNotFound() throws Exception {
        when(updateSection.execute(any())).thenThrow(new ContentSectionNotFoundException(UUID.randomUUID()));

        mockMvc.perform(put("/api/content/admin/packages/{p}/versions/{v}/sections/{s}",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
                        .contentType("application/json").content("{\"title\":\"Passage\",\"sortOrder\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void incompleteReorderListReturnsBadRequest() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("sectionIds must list every section of the version exactly once"))
                .when(reorderSections).execute(any(), any(), any());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/content/admin/packages/{p}/versions/{v}/sections/order",
                                UUID.randomUUID(), UUID.randomUUID())
                        .contentType("application/json")
                        .content("{\"sectionIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isBadRequest());
    }
}
