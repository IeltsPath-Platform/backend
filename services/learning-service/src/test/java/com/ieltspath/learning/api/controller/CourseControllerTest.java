package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.usecase.AssignCourseTestUseCase;
import com.ieltspath.learning.application.usecase.ListCoursesUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseController.class, properties = {"spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class CourseControllerTest {
    static final byte[] KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean AssignCourseTestUseCase assignCourseTest;
    @MockitoBean ListCoursesUseCase listCourses;
    final UUID learner = UUID.randomUUID();
    final UUID course = UUID.randomUUID();

    @Test
    void theCourseIdOfThePathReachesTheUseCaseAndItsRefusalKeepsItsCode() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(assignCourseTest.execute(learner, course)).thenThrow(
                new LearningRequestException(403, "COURSE_TEST_LOCKED", "Complete all course topics first"));

        mvc.perform(post("/api/learning/courses/" + course + "/test-assignments")
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COURSE_TEST_LOCKED"));

        verify(assignCourseTest).execute(learner, course);
    }

    @Test
    void aPathThatIsNotAUuidIsRejectedAsInvalid() throws Exception {
        mvc.perform(post("/api/learning/courses/not-a-uuid/test-assignments")
                        .with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isUnprocessableEntity());
    }
}
