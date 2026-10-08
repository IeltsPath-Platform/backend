package com.ieltspath.learning.api;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonsecurity.currentuser.CurrentUser;
import com.ieltspath.learning.api.controller.CourseController;
import com.ieltspath.learning.api.controller.PlacementController;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.PlacementTestResult;
import com.ieltspath.learning.application.usecase.AssignCourseTestUseCase;
import com.ieltspath.learning.application.usecase.GetPlacementTestUseCase;
import com.ieltspath.learning.application.usecase.ListCoursesUseCase;
import com.ieltspath.learning.application.usecase.RequirePlacementUseCase;
import com.ieltspath.learning.infrastructure.config.PlacementGateConfig;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {LearningSecurityWebMvcTest.ProtectedController.class, CourseController.class,
        PlacementController.class}, properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@Import({LearningSecurityWebMvcTest.ProtectedController.class, PlacementGateConfig.class})
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
    @MockitoBean AssignCourseTestUseCase assignCourseTest;
    @MockitoBean RequirePlacementUseCase requirePlacement;
    @MockitoBean GetPlacementTestUseCase getPlacementTest;

    private final UUID userId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void learnerWithoutPlacementIsBlockedOnEveryLearningRoute() throws Exception {
        when(currentUser.currentUser()).thenReturn(Optional.of(new CurrentUser(userId, Set.of("CUSTOMER"))));
        doThrow(new LearningRequestException(403, "PLACEMENT_REQUIRED", "Take the placement test first"))
                .when(requirePlacement).execute(userId);

        mockMvc.perform(get("/api/learning/test-protected"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PLACEMENT_REQUIRED"));
        mockMvc.perform(get("/api/learning/courses"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PLACEMENT_REQUIRED"));
        verifyNoInteractions(listCourses);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void placementTestStaysReachableBeforeAnyPlacementExists() throws Exception {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        when(currentUser.currentUser()).thenReturn(Optional.of(new CurrentUser(userId, Set.of("CUSTOMER"))));
        when(currentUser.requireUserId()).thenReturn(userId);
        doThrow(new LearningRequestException(403, "PLACEMENT_REQUIRED", "Take the placement test first"))
                .when(requirePlacement).execute(userId);
        when(getPlacementTest.execute(userId)).thenReturn(new PlacementTestResult(packageId, versionId));

        mockMvc.perform(get("/api/learning/placement-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageId").value(packageId.toString()))
                .andExpect(jsonPath("$.packageVersionId").value(versionId.toString()));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void placementTestReportsThatItWasAlreadyTaken() throws Exception {
        when(currentUser.requireUserId()).thenReturn(userId);
        when(getPlacementTest.execute(userId)).thenThrow(
                new LearningRequestException(409, "PLACEMENT_ALREADY_DONE", "The placement test was already taken"));

        mockMvc.perform(get("/api/learning/placement-test"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PLACEMENT_ALREADY_DONE"));
    }

    @Test
    @WithMockUser(roles = "EXAMINER")
    void otherRolesAreNotAskedForAPlacement() throws Exception {
        when(currentUser.currentUser()).thenReturn(Optional.of(new CurrentUser(userId, Set.of("EXAMINER"))));

        mockMvc.perform(get("/api/learning/courses")).andExpect(status().isForbidden());
        verifyNoInteractions(requirePlacement);
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
