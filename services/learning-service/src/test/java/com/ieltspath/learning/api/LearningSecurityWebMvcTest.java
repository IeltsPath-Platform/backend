package com.ieltspath.learning.api;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.controller.CourseController;
import com.ieltspath.learning.api.controller.PlacementController;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.PlacementAssignmentResult;
import com.ieltspath.learning.application.result.PlacementTestOptionsResult;
import com.ieltspath.learning.application.usecase.AssignCourseTestUseCase;
import com.ieltspath.learning.application.usecase.AssignPlacementTestUseCase;
import com.ieltspath.learning.application.usecase.GetPlacementTestOptionsUseCase;
import com.ieltspath.learning.application.usecase.ReleasePlacementAssignmentUseCase;
import com.ieltspath.learning.domain.vo.PlacementPool;
import com.ieltspath.learning.application.usecase.ListCoursesUseCase;
import com.ieltspath.learning.application.usecase.UnlockCourseUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {LearningSecurityWebMvcTest.ProtectedController.class, CourseController.class,
        PlacementController.class, com.ieltspath.learning.api.controller.LearningStatisticsController.class}, properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@Import(LearningSecurityWebMvcTest.ProtectedController.class)
class LearningSecurityWebMvcTest {
    private static final byte[] INTERNAL_KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/api/learning/test-protected")
        String protectedRoute() {
            return "ok";
        }
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean ListCoursesUseCase listCourses;
    @MockitoBean UnlockCourseUseCase unlockCourse;
    @MockitoBean AssignCourseTestUseCase assignCourseTest;
    @MockitoBean GetPlacementTestOptionsUseCase placementOptions;
    @MockitoBean com.ieltspath.learning.application.usecase.GetLegacyPlacementTestUseCase legacyPlacement;
    @MockitoBean AssignPlacementTestUseCase assignPlacement;
    @MockitoBean ReleasePlacementAssignmentUseCase releasePlacement;

    @MockitoBean com.ieltspath.learning.application.usecase.GetLearningProgressStatisticsUseCase statistics;
    @MockitoBean com.ieltspath.learning.application.usecase.GetStudyContextUseCase studyContext;
    @MockitoBean com.ieltspath.learning.application.usecase.GetLearningOverviewUseCase overview;

    @Test
    void anonymousOverviewIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/learning/statistics/overview")).andExpect(status().isUnauthorized());
        verifyNoInteractions(overview);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void staffCannotReadLearnerOverview() throws Exception {
        mockMvc.perform(get("/api/learning/statistics/overview")).andExpect(status().isForbidden());
        verifyNoInteractions(overview);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void overviewIgnoresUntrustedUserAndPagingParameters() throws Exception {
        when(currentUser.requireUserId()).thenReturn(userId);
        var now = java.time.Instant.parse("2026-11-08T05:00:00Z");
        when(overview.execute(userId)).thenReturn(
                new com.ieltspath.learning.application.result.LearningOverviewStatistics(now, now, 120, 7));
        mockMvc.perform(get("/api/learning/statistics/overview").param("userId", UUID.randomUUID().toString())
                        .param("page", "100").header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completedLessons").value(7))
                .andExpect(jsonPath("$.totalStudySeconds").value(120));
        org.mockito.Mockito.verify(overview).execute(userId);
    }

    @Test
    void anonymousStatisticsCannotReadProgressOrStudyContexts() throws Exception {
        mockMvc.perform(get("/api/learning/statistics/progress")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/learning/statistics/study-context").param("sourceType", "LESSON")
                .param("sourceId", UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
        verifyNoInteractions(statistics, studyContext);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void nonLearnerCannotReadStatistics() throws Exception {
        mockMvc.perform(get("/api/learning/statistics/progress")).andExpect(status().isForbidden());
        verifyNoInteractions(statistics);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void studyContextAlwaysUsesAuthenticatedIdentity() throws Exception {
        UUID source = UUID.randomUUID();
        UUID course = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        when(studyContext.execute(userId, "LESSON", source)).thenReturn(
                new com.ieltspath.learning.application.result.StudyContextResult("LESSON", source, course));
        mockMvc.perform(get("/api/learning/statistics/study-context").param("sourceType", "LESSON")
                .param("sourceId", source.toString()).header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.courseId").value(course.toString()))
                .andExpect(jsonPath("$.sourceId").value(source.toString()));
    }

    private final UUID userId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void oversizedProgressScopeReturnsUnprocessableEntityWithClearCode() throws Exception {
        when(currentUser.requireUserId()).thenReturn(userId);
        when(statistics.execute(userId, null, 0, 4)).thenThrow(new LearningRequestException(422,
                "PROGRESS_SCOPE_TOO_LARGE", "Thống kê tiến độ hỗ trợ tối đa 100 khóa học đã bắt đầu."));

        mockMvc.perform(get("/api/learning/statistics/progress"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PROGRESS_SCOPE_TOO_LARGE"))
                .andExpect(jsonPath("$.detail").value("Thống kê tiến độ hỗ trợ tối đa 100 khóa học đã bắt đầu."));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerWithoutPlacementCanStillReadCourses() throws Exception {
        when(currentUser.requireUserId()).thenReturn(userId);
        when(listCourses.execute(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/learning/courses")).andExpect(status().isOk());
        verifyNoInteractions(placementOptions, assignPlacement, releasePlacement);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCanRequestAPlacementAssignment() throws Exception {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        when(assignPlacement.execute(userId, PlacementPool.BAND_3_5)).thenReturn(
                new PlacementAssignmentResult(assignmentId, PlacementPool.BAND_3_5, packageId, versionId, 2, null));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/learning/placement-test/assignments")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"pool\":\"BAND_3_5\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentId").value(assignmentId.toString()))
                .andExpect(jsonPath("$.pool").value("BAND_3_5"))
                .andExpect(jsonPath("$.packageId").value(packageId.toString()))
                .andExpect(jsonPath("$.packageVersionId").value(versionId.toString()))
                .andExpect(jsonPath("$.retakesLeft").value(2));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void placementOptionsIncludePoolsAndCurrentPlacementFields() throws Exception {
        when(currentUser.requireUserId()).thenReturn(userId);
        when(placementOptions.execute(userId)).thenReturn(new PlacementTestOptionsResult(List.of(
                new PlacementTestOptionsResult.PoolOption(PlacementPool.BAND_0_3, 2)), 3, 1, null, true, 5.5, null));

        mockMvc.perform(get("/api/learning/placement-test/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pools[0].pool").value("BAND_0_3"))
                .andExpect(jsonPath("$.pools[0].examCount").value(2))
                .andExpect(jsonPath("$.limitPer24h").value(3))
                .andExpect(jsonPath("$.retakesLeft").value(1))
                .andExpect(jsonPath("$.hasPlacement").value(true))
                .andExpect(jsonPath("$.currentBand").value(5.5));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void theDeprecatedPlacementRouteStillAnswersWithThePackageAndItsVersion() throws Exception {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        when(legacyPlacement.execute(userId)).thenReturn(
                new com.ieltspath.learning.application.result.PlacementTestResult(packageId, versionId));

        mockMvc.perform(get("/api/learning/placement-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageId").value(packageId.toString()))
                .andExpect(jsonPath("$.packageVersionId").value(versionId.toString()));
    }

    @Test
    void theDeprecatedPlacementRouteRequiresAuthenticationAndTheCustomerRole() throws Exception {
        mockMvc.perform(get("/api/learning/placement-test")).andExpect(status().isUnauthorized());
        verifyNoInteractions(legacyPlacement);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void staffCannotUseTheDeprecatedPlacementRoute() throws Exception {
        mockMvc.perform(get("/api/learning/placement-test")).andExpect(status().isForbidden());
        verifyNoInteractions(legacyPlacement);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerCanReleaseAnOwnedFailedStartAssignment() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/learning/placement-test/assignments/{id}/release", assignmentId))
                .andExpect(status().isNoContent());
        org.mockito.Mockito.verify(releasePlacement).execute(userId, assignmentId);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void invalidPlacementPoolIsBadRequest() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/learning/placement-test/assignments")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"pool\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(assignPlacement);
    }

    @Test
    void placementRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/learning/placement-test/options")).andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/learning/placement-test/assignments").content("{\"pool\":\"BAND_5_9\"}")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/learning/placement-test/assignments/{id}/release", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(placementOptions, assignPlacement, releasePlacement);
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void staffCannotUsePlacementAssignmentRoutes() throws Exception {
        mockMvc.perform(get("/api/learning/placement-test/options")).andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/learning/placement-test/assignments").content("{\"pool\":\"BAND_5_9\"}")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
        verifyNoInteractions(placementOptions, assignPlacement);
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/learning/test-protected")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void authenticatedCallerReachesTheTestRoute() throws Exception {
        mockMvc.perform(get("/api/learning/test-protected")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void examinerCannotReadLearningCourses() throws Exception {
        mockMvc.perform(get("/api/learning/courses")).andExpect(status().isForbidden());
    }
}
