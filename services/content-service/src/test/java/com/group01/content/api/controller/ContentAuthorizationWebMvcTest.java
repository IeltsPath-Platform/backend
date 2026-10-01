package com.group01.content.api.controller;

import com.group01.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.group01.content.application.result.ContentPackageResult;
import com.group01.content.application.result.ContentPackageDetailResult;
import com.group01.content.application.result.ContentAssetResult;
import com.group01.content.application.result.KnowledgePointResult;
import com.group01.content.application.result.QuestionResult;
import com.group01.content.application.result.QuestionDetailResult;
import com.group01.content.application.result.ReadingPassageResult;
import com.group01.content.application.usecase.*;
import com.group01.content.domain.vo.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        QuestionController.class, ContentPackageController.class, ContentAssetController.class,
        KnowledgePointController.class, TopicController.class, ReadingController.class
}, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class ContentAuthorizationWebMvcTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean ListQuestionsUseCase listQuestions;
    @MockitoBean GetQuestionDetailUseCase getQuestionDetail;
    @MockitoBean CreateQuestionUseCase createQuestion;
    @MockitoBean AddQuestionVersionUseCase addQuestionVersion;
    @MockitoBean ArchiveQuestionUseCase archiveQuestion;
    @MockitoBean ListContentPackagesUseCase listPackages;
    @MockitoBean GetContentPackageDetailUseCase getPackageDetail;
    @MockitoBean CreateContentPackageUseCase createPackage;
    @MockitoBean AddPackageVersionUseCase addPackageVersion;
    @MockitoBean AddContentSectionUseCase addSection;
    @MockitoBean PublishContentPackageUseCase publishPackage;
    @MockitoBean GetContentAssetUseCase getAsset;
    @MockitoBean CreateContentAssetUseCase createAsset;
    @MockitoBean LinkAssetUseCase linkAsset;
    @MockitoBean GetKnowledgePointsUseCase getKnowledgePoints;
    @MockitoBean CreateKnowledgePointUseCase createKnowledgePoint;
    @MockitoBean GetTopicTreeUseCase getTopicTree;
    @MockitoBean GetTopicUseCase getTopic;
    @MockitoBean CreateTopicUseCase createTopic;
    @MockitoBean UpdateTopicUseCase updateTopic;
    @MockitoBean GetReadingPassageUseCase getReadingPassage;

    private final UUID id = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCanReadTopicsKnowledgePointsAndReading() throws Exception {
        when(getTopicTree.execute()).thenReturn(List.of());
        when(getKnowledgePoints.execute(null)).thenReturn(List.of());
        when(getReadingPassage.execute(id)).thenReturn(new ReadingPassageResult(
                id, "Reading", null, id, "Package", List.of()));

        mockMvc.perform(get("/api/content/topics")).andExpect(status().isOk());
        mockMvc.perform(get("/api/content/knowledge-points")).andExpect(status().isOk());
        mockMvc.perform(get("/api/content/reading/sections/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionId").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCannotReadQuestionOrPackageListsAndDetails() throws Exception {
        mockMvc.perform(get("/api/content/questions")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/content/questions/{id}", id)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/content/admin/questions")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/content/packages")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/content/packages/{id}", id)).andExpect(status().isForbidden());
        verifyNoInteractions(listQuestions, getQuestionDetail, listPackages, getPackageDetail);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void contentAuthorCanReadQuestionAndPackageListsAndDetails() throws Exception {
        when(listQuestions.execute(null)).thenReturn(List.of());
        when(listPackages.execute(null, null)).thenReturn(List.of());
        when(getQuestionDetail.execute(id)).thenReturn(new QuestionDetailResult(
                id, QuestionType.MULTIPLE_CHOICE, Skill.READING, null, PublicationStatus.PUBLISHED,
                id, Instant.now(), Instant.now(), List.of()));
        when(getPackageDetail.execute(id)).thenReturn(new ContentPackageDetailResult(
                id, "PS-KP1-A", "Practice", PackageType.PRACTICE_SET, null,
                PublicationStatus.PUBLISHED, id, 1, "{}", Instant.now(), id,
                Instant.now(), Instant.now(), List.of()));
        mockMvc.perform(get("/api/content/questions")).andExpect(status().isOk());
        mockMvc.perform(get("/api/content/questions/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/content/packages")).andExpect(status().isOk());
        mockMvc.perform(get("/api/content/packages/{id}", id)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCannotWriteContent() throws Exception {
        mockMvc.perform(post("/api/content/questions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionType\":\"MULTIPLE_CHOICE\",\"skill\":\"READING\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/content/packages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"PS-KP1-A\",\"title\":\"Practice\",\"packageType\":\"PRACTICE_SET\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/content/assets/links").contentType(MediaType.APPLICATION_JSON)
                .content("{\"assetId\":\"" + id + "\",\"sortOrder\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/content/assets").contentType(MediaType.APPLICATION_JSON)
                .content("{\"assetType\":\"PASSAGE\",\"textContent\":\"Paragraph\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/content/knowledge-points").contentType(MediaType.APPLICATION_JSON)
                .content("{\"topicId\":\"" + id + "\",\"code\":\"KP1\",\"name\":\"KP1\",\"kind\":\"GRAMMAR\",\"learningType\":\"CONCEPT\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(createQuestion, createPackage, createAsset, linkAsset, createKnowledgePoint);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCannotUseOtherQuestionAndPackageWriteRoutes() throws Exception {
        MockHttpServletRequestBuilder[] requests = {
                post("/api/content/admin/questions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionType\":\"MULTIPLE_CHOICE\"}"),
                post("/api/content/questions/{id}/versions", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionNumber\":1,\"stem\":\"Question\"}"),
                post("/api/content/questions/{id}/archive", id),
                post("/api/content/packages/{id}/versions", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionNumber\":1,\"rulesJson\":\"{}\"}"),
                post("/api/content/packages/versions/{id}/sections", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Reading\",\"skill\":\"READING\",\"sortOrder\":1}"),
                post("/api/content/packages/{id}/publish", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionId\":\"" + id + "\"}")
        };
        for (MockHttpServletRequestBuilder request : requests) {
            mockMvc.perform(request).andExpect(status().isForbidden());
        }
        verifyNoInteractions(createQuestion, addQuestionVersion, archiveQuestion,
                addPackageVersion, addSection, publishPackage);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanWriteContent() throws Exception {
        when(createQuestion.execute(any())).thenReturn(new QuestionResult(id, QuestionType.MULTIPLE_CHOICE,
                Skill.READING, null, PublicationStatus.DRAFT, null, Instant.now(), Instant.now()));
        when(createPackage.execute(any())).thenReturn(new ContentPackageResult(id, "PS-KP1-A", "Practice",
                PackageType.PRACTICE_SET, null, PublicationStatus.DRAFT, null, Instant.now(), Instant.now()));
        when(createKnowledgePoint.execute(any())).thenReturn(new KnowledgePointResult(id, id, "KP1", "KP1",
                KnowledgePointKind.GRAMMAR, LearningType.CONCEPT, null, null, ContentStatus.ACTIVE,
                Instant.now(), Instant.now()));
        when(createAsset.execute(any())).thenReturn(new ContentAssetResult(
                id, AssetType.PASSAGE, "Paragraph", null, null, null, AssetValidationStatus.VALID, Instant.now()));

        mockMvc.perform(post("/api/content/questions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionType\":\"MULTIPLE_CHOICE\",\"skill\":\"READING\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/content/packages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"PS-KP1-A\",\"title\":\"Practice\",\"packageType\":\"PRACTICE_SET\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/content/assets/links").contentType(MediaType.APPLICATION_JSON)
                .content("{\"assetId\":\"" + id + "\",\"sortOrder\":1}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/content/assets").contentType(MediaType.APPLICATION_JSON)
                .content("{\"assetType\":\"PASSAGE\",\"textContent\":\"Paragraph\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/content/knowledge-points").contentType(MediaType.APPLICATION_JSON)
                .content("{\"topicId\":\"" + id + "\",\"code\":\"KP1\",\"name\":\"KP1\",\"kind\":\"GRAMMAR\",\"learningType\":\"CONCEPT\"}"))
                .andExpect(status().isCreated());
    }
}
