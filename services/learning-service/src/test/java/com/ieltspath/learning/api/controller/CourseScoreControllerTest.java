package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.result.CourseScoreResult;
import com.ieltspath.learning.application.usecase.GetCourseScoreUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseScoreController.class, properties = {
        "spring.cloud.config.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class CourseScoreControllerTest {
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean GetCourseScoreUseCase score;

    @Test
    void learnerCanReadOwnScoreAndIdentityComesFromAuthentication() throws Exception {
        UUID learner = UUID.randomUUID();
        UUID course = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(learner);
        when(score.execute(learner, course)).thenReturn(new CourseScoreResult(course, "IN_PROGRESS",
                new CourseScoreResult.Components(null, null, null, null, null),
                new CourseScoreResult.Weights(0.2, 0.3, 0.5, 1, 0), 0, 0.7, 0.6,
                List.of(), List.of(), null));

        mvc.perform(get("/api/learning/courses/" + course + "/score")
                        .param("userId", UUID.randomUUID().toString()).with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(course.toString()));
        verify(score).execute(learner, course);
    }

    @Test
    void examinerLearnerScoreRouteHasBeenRemoved() throws Exception {
        mvc.perform(get("/api/learning/admin/learners/" + UUID.randomUUID() + "/courses/"
                        + UUID.randomUUID() + "/score").with(user("examiner").roles("EXAMINER")))
                .andExpect(status().isNotFound());
        verifyNoInteractions(score);
    }
}
