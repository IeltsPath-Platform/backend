package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.usecase.AddSectionQuestionUseCase;
import com.ieltspath.content.application.usecase.ArchiveContentPackageUseCase;
import com.ieltspath.content.application.usecase.CopyPackageVersionToDraftUseCase;
import com.ieltspath.content.application.usecase.UpdateContentPackageUseCase;
import com.ieltspath.content.application.usecase.RemoveAndReorderSectionQuestionsUseCase;
import com.ieltspath.content.application.usecase.CreateContentPackageUseCase;
import com.ieltspath.content.application.usecase.CreateContentSectionUseCase;
import com.ieltspath.content.application.usecase.ListAuthorPackagesUseCase;
import com.ieltspath.content.domain.exception.InvalidPackageCourseException;
import com.ieltspath.content.domain.exception.InvalidPackageTopicException;
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
    private final CreateContentPackageUseCase createPackage = mock(CreateContentPackageUseCase.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ContentPackageAuthoringController(
                        mock(GetAuthorPackageUseCase.class), createDraft, mock(CreateContentSectionUseCase.class),
                        updateSection, mock(DeleteContentSectionUseCase.class), reorderSections,
                        mock(AddSectionQuestionUseCase.class), mock(RemoveAndReorderSectionQuestionsUseCase.class),
                        mock(ListAuthorPackagesUseCase.class), createPackage,
                        mock(UpdateContentPackageUseCase.class), mock(ArchiveContentPackageUseCase.class),
                        mock(CopyPackageVersionToDraftUseCase.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void finalTestWithoutItsTopicIsBadRequestWithItsCode() throws Exception {
        when(createPackage.executeAsAuthor(any())).thenThrow(new InvalidPackageTopicException("topic missing"));

        mockMvc.perform(post("/api/content/admin/packages").contentType("application/json")
                        .content("{\"code\":\"X\",\"title\":\"T\",\"packageType\":\"TOPIC_TEST\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value("INVALID_PACKAGE_TOPIC"));
    }

    @Test
    void courseTestWithoutItsCourseIsBadRequestWithItsCode() throws Exception {
        when(createPackage.executeAsAuthor(any())).thenThrow(new InvalidPackageCourseException("course missing"));

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
