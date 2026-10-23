package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.content.application.result.ContentPackageResult;
import com.ieltspath.content.application.result.ContentPackageDetailResult;
import com.ieltspath.content.application.result.ContentAssetResult;
import com.ieltspath.content.application.result.KnowledgePointResult;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.application.result.QuestionDetailResult;
import com.ieltspath.content.application.result.ReadingPassageResult;
import com.ieltspath.content.application.usecase.*;
import com.ieltspath.content.domain.vo.*;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        QuestionController.class, ContentPackageController.class, ContentPackageAdminController.class, ContentAssetController.class,
        QuestionPublicationAdminController.class, LessonAuthoringAdminController.class,
        KnowledgePointController.class, TopicController.class, ReadingController.class, CourseController.class
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
    @MockitoBean ListCoursesUseCase listCourses;
    @MockitoBean CreateCourseUseCase createCourse;
    @MockitoBean UpdateCourseUseCase updateCourse;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCanReadCoursesButCannotCreateOrUpdateThem() throws Exception {
        when(listCourses.execute()).thenReturn(List.of());
        mockMvc.perform(get("/api/content/courses")).andExpect(status().isOk());
        mockMvc.perform(post("/api/content/admin/courses").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"IELTS\",\"name\":\"IELTS\",\"bandLevel\":5.5}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/content/admin/courses/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"IELTS\",\"bandLevel\":5.5}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(createCourse, updateCourse);
    }

    @Test
    @WithMockUser(roles = "SALES_STAFF")
    void salesStaffCannotReadCourses() throws Exception {
        mockMvc.perform(get("/api/content/courses")).andExpect(status().isForbidden());
        verifyNoInteractions(listCourses);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void authorCanCreateAndUpdateCourses() throws Exception {
        var course = com.ieltspath.content.application.result.CourseResult.from(
                com.ieltspath.content.domain.aggregate.Course.create("IELTS", "IELTS", new java.math.BigDecimal("5.5")));
        when(createCourse.execute(any())).thenReturn(course);
        when(updateCourse.execute(any())).thenReturn(course);
        mockMvc.perform(post("/api/content/admin/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"IELTS\",\"name\":\"IELTS\",\"bandLevel\":5.5}"))
                .andExpect(status().isCreated());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/content/admin/courses/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"IELTS\",\"bandLevel\":5.5}"))
                .andExpect(status().isOk());
    }
    @MockitoBean ListQuestionsUseCase listQuestions;
    @MockitoBean GetQuestionDetailUseCase getQuestionDetail;
    @MockitoBean CreateQuestionUseCase createQuestion;
    @MockitoBean AddQuestionVersionUseCase addQuestionVersion;
    @MockitoBean ArchiveQuestionUseCase archiveQuestion;
    @MockitoBean PublishQuestionVersionUseCase publishQuestionVersion;
    @MockitoBean ArchiveQuestionVersionUseCase archiveQuestionVersion;
    @MockitoBean LessonAuthoringUseCase lessonAuthoring;
    @MockitoBean ListContentPackagesUseCase listPackages;
    @MockitoBean GetContentPackageDetailUseCase getPackageDetail;
    @MockitoBean CreateContentPackageUseCase createPackage;
    @MockitoBean AddPackageVersionUseCase addPackageVersion;
    @MockitoBean AddContentSectionUseCase addSection;
    @MockitoBean PublishContentPackageUseCase publishPackage;
    @MockitoBean GetPackageVersionPreviewUseCase previewPackageVersion;
    @MockitoBean SchedulePackagePublicationUseCase schedulePackagePublication;
    @MockitoBean CancelPackagePublicationScheduleUseCase cancelPackagePublicationSchedule;
    @MockitoBean ListPackagePublicationSchedulesUseCase listPackagePublicationSchedules;
    @MockitoBean GetContentAssetUseCase getAsset;
    @MockitoBean CreateContentAssetUseCase createAsset;
    @MockitoBean LinkAssetUseCase linkAsset;
    @MockitoBean GetKnowledgePointsUseCase getKnowledgePoints;
    @MockitoBean CreateKnowledgePointUseCase createKnowledgePoint;
    @MockitoBean UpdateKnowledgePointUseCase updateKnowledgePoint;
    @MockitoBean DeactivateKnowledgePointUseCase deactivateKnowledgePoint;
    @MockitoBean ActivateKnowledgePointUseCase activateKnowledgePoint;
    @MockitoBean GetKnowledgePointPrerequisitesUseCase getKnowledgePointPrerequisites;
    @MockitoBean ReplaceKnowledgePointPrerequisitesUseCase replaceKnowledgePointPrerequisites;
    @MockitoBean GetTopicTreeUseCase getTopicTree;
    @MockitoBean GetTopicUseCase getTopic;
    @MockitoBean CreateTopicUseCase createTopic;
    @MockitoBean UpdateTopicUseCase updateTopic;
    @MockitoBean ReorderTopicsUseCase reorderTopics;
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
        mockMvc.perform(get("/api/content/admin/packages/{id}/versions/{versionId}/preview", id, UUID.randomUUID()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(listQuestions, getQuestionDetail, listPackages, getPackageDetail, previewPackageVersion);
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
                .content("{\"topicId\":\"" + id + "\",\"code\":\"R55_EXAMPLE\",\"name\":\"KP1\",\"kind\":\"GRAMMAR\",\"learningType\":\"CONCEPT\",\"cefrLevel\":\"B1\",\"canDo\":\"Form simple sentences.\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(createQuestion, createPackage, createAsset, linkAsset, createKnowledgePoint);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCannotChangePackagePublicationSchedules() throws Exception {
        UUID versionId = UUID.randomUUID();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/content/admin/packages/{id}/versions/{versionId}/schedule", id, versionId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"publishAt\":\"2030-01-01T00:00:00Z\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/content/admin/packages/{id}/versions/{versionId}/schedule", id, versionId))
                .andExpect(status().isForbidden());
        verifyNoInteractions(schedulePackagePublication, cancelPackagePublicationSchedule);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCannotPublishOrArchiveQuestionVersions() throws Exception {
        UUID versionId = UUID.randomUUID();
        mockMvc.perform(post("/api/content/admin/questions/{questionId}/versions/{versionId}/publish", id, versionId))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/content/admin/questions/{questionId}/versions/{versionId}/archive", id, versionId))
                .andExpect(status().isForbidden());
        verifyNoInteractions(publishQuestionVersion, archiveQuestionVersion);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCannotAuthorLessons() throws Exception {
        mockMvc.perform(post("/api/content/admin/topics/{topicId}/lessons", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"AUTHOR_TEST\",\"title\":\"Lesson\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(lessonAuthoring);
    }

    @Test
    @WithMockUser(roles = {"CUSTOMER", "EXAMINER", "ADMIN"})
    void onlyAContentAuthorCanReorderTopics() throws Exception {
        mockMvc.perform(put("/api/content/admin/topics/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicIds\":[\"" + id + "\"]}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reorderTopics);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void contentAuthorCanReorderTopics() throws Exception {
        mockMvc.perform(put("/api/content/admin/topics/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicIds\":[\"" + id + "\"]}"))
                .andExpect(status().isNoContent());
        verify(reorderTopics).execute(null, List.of(id));
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void contentAuthorCanPublishQuestionVersion() throws Exception {
        UUID versionId = UUID.randomUUID();
        when(publishQuestionVersion.execute(id, versionId)).thenReturn(new QuestionDetailResult(id,
                QuestionType.MULTIPLE_CHOICE, Skill.READING, null, PublicationStatus.PUBLISHED, versionId,
                Instant.now(), Instant.now(), List.of(), QuestionPurpose.LEARNING));
        mockMvc.perform(post("/api/content/admin/questions/{questionId}/versions/{versionId}/publish", id, versionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPublishedVersionId").value(versionId.toString()));
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
    void adminCannotWriteContent() throws Exception {
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
                .content("{\"topicId\":\"" + id + "\",\"code\":\"R55_EXAMPLE\",\"name\":\"KP1\",\"kind\":\"GRAMMAR\",\"learningType\":\"CONCEPT\",\"cefrLevel\":\"B1\",\"canDo\":\"Form simple sentences.\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(createQuestion, createPackage, createKnowledgePoint, createAsset);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCannotReadAnAssetDirectly() throws Exception {
        mockMvc.perform(get("/api/content/assets/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
        verifyNoInteractions(getAsset);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void contentAuthorCanReadAnAsset() throws Exception {
        UUID id = UUID.randomUUID();
        when(getAsset.execute(id)).thenReturn(new ContentAssetResult(id, AssetType.AUDIO, "Librarian: Good morning.",
                "listening/demo/ls1.mp3", 45, null, AssetValidationStatus.VALID, Instant.now()));

        mockMvc.perform(get("/api/content/assets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetType").value("AUDIO"));
    }

    @Test
    void anonymousCannotReadAnAsset() throws Exception {
        mockMvc.perform(get("/api/content/assets/{id}", UUID.randomUUID())).andExpect(status().isUnauthorized());
    }}
