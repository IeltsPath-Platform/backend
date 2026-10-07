package com.ieltspath.learning.api;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.controller.CourseController;
import com.ieltspath.learning.application.usecase.AssignCourseTestUseCase;
import com.ieltspath.learning.application.usecase.ListCoursesUseCase;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {LearningSecurityWebMvcTest.ProtectedController.class, CourseController.class}, properties = {
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
    @MockitoBean AssignCourseTestUseCase assignCourseTest;

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
