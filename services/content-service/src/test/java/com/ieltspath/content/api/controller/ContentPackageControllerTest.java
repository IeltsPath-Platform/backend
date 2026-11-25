package com.ieltspath.content.api.controller;

import org.mockito.ArgumentCaptor;
import com.ieltspath.content.domain.exception.InvalidPackageLessonException;
import com.ieltspath.content.domain.exception.QuestionAlreadyUsedException;
import com.ieltspath.content.domain.exception.QuestionPurposeMismatchException;
import com.ieltspath.content.domain.exception.QuestionNotPublishedException;
import com.ieltspath.content.domain.vo.QuestionUsageConflict;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.UnpublishedPackageQuestion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.api.dto.request.CreateContentPackageRequest;
import com.ieltspath.content.api.dto.request.PublishPackageRequest;
import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.command.CreateContentPackageCommand;
import com.ieltspath.content.application.command.PublishContentPackageCommand;
import com.ieltspath.content.application.result.ContentPackageResult;
import com.ieltspath.content.application.usecase.*;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContentPackageControllerTest {
    @Test
    void placementRequiresPoolAndOtherTypesRejectPool() throws Exception {
        for (String body : List.of(
                "{\"code\":\"P\",\"title\":\"Placement\",\"packageType\":\"PLACEMENT_TEST\"}",
                "{\"code\":\"P\",\"title\":\"Practice\",\"packageType\":\"PRACTICE_SET\",\"placementPool\":\"BAND_0_3\"}")) {
            mockMvc.perform(post("/api/content/packages").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        org.mockito.Mockito.verifyNoInteractions(createContentPackageUseCase);
    }

    @Test
    void placementPoolIsForwardedAndReturned() throws Exception {
        when(createContentPackageUseCase.execute(any())).thenAnswer(call -> {
            var command = call.getArgument(0, CreateContentPackageCommand.class);
            return ContentPackageResult.of(com.ieltspath.content.domain.aggregate.ContentPackage.create(
                    command.code(), command.title(), command.packageType(), null, null, null, command.placementPool()));
        });
        mockMvc.perform(post("/api/content/packages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"P\",\"title\":\"Placement\",\"packageType\":\"PLACEMENT_TEST\",\"placementPool\":\"BAND_0_3\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.placementPool").value("BAND_0_3"));
    }

    @Test
    void creatingACourseTestThroughTheAuthorApiReturns400() throws Exception {
        when(createContentPackageUseCase.execute(any())).thenThrow(
                new IllegalArgumentException("COURSE_TEST packages cannot be created through this API"));
        mockMvc.perform(post("/api/content/packages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"FINAL\",\"title\":\"Final\",\"packageType\":\"COURSE_TEST\"}"))
                .andExpect(status().isBadRequest());
        ArgumentCaptor<CreateContentPackageCommand> command = ArgumentCaptor.forClass(CreateContentPackageCommand.class);
        verify(createContentPackageUseCase).execute(command.capture());
        assertThat(command.getValue().packageType()).isEqualTo(PackageType.COURSE_TEST);
    }

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
                id, "PKG_01", "IELTS Cam 18", PackageType.MOCK_TEST, null,
                PublicationStatus.PUBLISHED, UUID.randomUUID(), Instant.now(), Instant.now(), null
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
        UUID lessonId = UUID.randomUUID();
        CreateContentPackageRequest request = new CreateContentPackageRequest(
                "PKG_02", "IELTS Listening Test", PackageType.PRACTICE_SET, AccessLevel.FREE, lessonId
        );
        ContentPackageResult result = new ContentPackageResult(
                id, "PKG_02", "IELTS Listening Test", PackageType.PRACTICE_SET, null,
                PublicationStatus.DRAFT, null, Instant.now(), Instant.now(), lessonId
        );

        when(createContentPackageUseCase.execute(any(CreateContentPackageCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/content/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("PKG_02"))
                .andExpect(jsonPath("$.lessonId").value(lessonId.toString()));

        ArgumentCaptor<CreateContentPackageCommand> command = ArgumentCaptor.forClass(CreateContentPackageCommand.class);
        verify(createContentPackageUseCase).execute(command.capture());
        assertThat(command.getValue().lessonId()).isEqualTo(lessonId);
    }

    @Test
    void aPackageThatCannotJoinItsLessonReturns422() throws Exception {
        when(createContentPackageUseCase.execute(any(CreateContentPackageCommand.class)))
                .thenThrow(new InvalidPackageLessonException("Only a PRACTICE_SET package can belong to a lesson"));

        mockMvc.perform(post("/api/content/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"Q1\",\"title\":\"Quiz\",\"packageType\":\"QUIZ\",\"lessonId\":\""
                        + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.code").value("INVALID_PACKAGE_LESSON"));
    }

    @Test
    void publishingRejectsOwnershipAndPurposeConflictsWith422() throws Exception {
        UUID questionId = UUID.randomUUID();
        when(publishContentPackageUseCase.execute(any()))
                .thenThrow(new QuestionAlreadyUsedException(List.of(
                        new QuestionUsageConflict(questionId, QuestionUsageConflict.OwnerType.LESSON, "L1"))))
                .thenThrow(new QuestionPurposeMismatchException(QuestionPurpose.EXAM, List.of(questionId)));
        String path = "/api/content/packages/" + UUID.randomUUID() + "/publish";
        String request = "{\"versionId\":\"" + UUID.randomUUID() + "\"}";
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.code").value("QUESTION_ALREADY_USED"));
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.code").value("QUESTION_PURPOSE_MISMATCH"));
    }

    @Test
    void publishingAnUnpublishedQuestionReturnsVietnameseMessageAndQuestionDetails() throws Exception {
        UUID questionId = UUID.randomUUID();
        when(publishContentPackageUseCase.execute(any())).thenThrow(new QuestionNotPublishedException(List.of(
                new UnpublishedPackageQuestion(questionId, "Q-DRAFT", "Reading 1", "DRAFT"))));

        mockMvc.perform(post("/api/content/packages/" + UUID.randomUUID() + "/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Đề còn 1 câu hỏi chưa xuất bản. Xuất bản các câu đó trước."))
                .andExpect(jsonPath("$.details.code").value("QUESTION_NOT_PUBLISHED"))
                .andExpect(jsonPath("$.details.questions[0].id").value(questionId.toString()))
                .andExpect(jsonPath("$.details.questions[0].code").value("Q-DRAFT"))
                .andExpect(jsonPath("$.details.questions[0].section").value("Reading 1"));
    }

    @Test
    @DisplayName("POST /api/content/packages/{id}/publish should publish package version")
    void shouldPublishPackage() throws Exception {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        PublishPackageRequest request = new PublishPackageRequest(versionId);

        ContentPackageResult result = new ContentPackageResult(
                packageId, "PKG_01", "IELTS Cam 18", PackageType.MOCK_TEST, null,
                PublicationStatus.PUBLISHED, versionId, Instant.now(), Instant.now(), null
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
