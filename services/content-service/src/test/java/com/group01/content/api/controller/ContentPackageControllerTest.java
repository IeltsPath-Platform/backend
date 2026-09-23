package com.group01.content.api.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.content.api.dto.request.CreateContentPackageRequest;
import com.group01.content.api.dto.request.PublishPackageRequest;
import com.group01.content.api.exception.GlobalExceptionHandler;
import com.group01.content.application.command.CreateContentPackageCommand;
import com.group01.content.application.command.PublishContentPackageCommand;
import com.group01.content.application.result.ContentPackageResult;
import com.group01.content.application.usecase.AddContentSectionUseCase;
import com.group01.content.application.usecase.AddPackageVersionUseCase;
import com.group01.content.application.usecase.CreateContentPackageUseCase;
import com.group01.content.application.usecase.GetContentPackageDetailUseCase;
import com.group01.content.application.usecase.ListContentPackagesUseCase;
import com.group01.content.application.usecase.PublishContentPackageUseCase;
import com.group01.content.domain.exception.ContentPackageNotFoundException;
import com.group01.content.domain.vo.AccessLevel;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.PublicationStatus;

@ExtendWith(MockitoExtension.class)
class ContentPackageControllerTest {

    @Mock
    private ListContentPackagesUseCase listContentPackagesUseCase;

    @Mock
    private GetContentPackageDetailUseCase getContentPackageDetailUseCase;

    @Mock
    private CreateContentPackageUseCase createContentPackageUseCase;

    @Mock
    private AddPackageVersionUseCase addPackageVersionUseCase;

    @Mock
    private AddContentSectionUseCase addContentSectionUseCase;

    @Mock
    private PublishContentPackageUseCase publishContentPackageUseCase;

    @InjectMocks
    private ContentPackageController contentPackageController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(contentPackageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/content/packages should return list of packages")
    void shouldReturnPackages() throws Exception {
        UUID id = UUID.randomUUID();
        ContentPackageResult result = new ContentPackageResult(
                id, "PKG_01", "IELTS Cam 18", PackageType.MOCK_TEST, AccessLevel.FREE,
                PublicationStatus.PUBLISHED, UUID.randomUUID(), Instant.now(), Instant.now()
        );

        when(listContentPackagesUseCase.execute(null, null)).thenReturn(List.of(result));

        mockMvc.perform(get("/api/content/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].code").value("PKG_01"))
                .andExpect(jsonPath("$[0].title").value("IELTS Cam 18"));
    }

    @Test
    @DisplayName("GET /api/content/packages/{id} when not found should return 404")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(getContentPackageDetailUseCase.execute(id))
                .thenThrow(new ContentPackageNotFoundException(id));

        mockMvc.perform(get("/api/content/packages/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/content/packages should create package successfully")
    void shouldCreatePackage() throws Exception {
        UUID id = UUID.randomUUID();
        CreateContentPackageRequest request = new CreateContentPackageRequest(
                "PKG_02", "IELTS Listening Test", PackageType.PRACTICE_SET, AccessLevel.FREE
        );
        ContentPackageResult result = new ContentPackageResult(
                id, "PKG_02", "IELTS Listening Test", PackageType.PRACTICE_SET, AccessLevel.FREE,
                PublicationStatus.DRAFT, null, Instant.now(), Instant.now()
        );

        when(createContentPackageUseCase.execute(any(CreateContentPackageCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/content/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("PKG_02"));
    }

    @Test
    @DisplayName("POST /api/content/packages/{id}/publish should publish package version")
    void shouldPublishPackage() throws Exception {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        PublishPackageRequest request = new PublishPackageRequest(versionId);

        ContentPackageResult result = new ContentPackageResult(
                packageId, "PKG_01", "IELTS Cam 18", PackageType.MOCK_TEST, AccessLevel.FREE,
                PublicationStatus.PUBLISHED, versionId, Instant.now(), Instant.now()
        );
        when(publishContentPackageUseCase.execute(any(PublishContentPackageCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/content/packages/" + packageId + "/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.currentPublishedVersionId").value(versionId.toString()));
    }
}
