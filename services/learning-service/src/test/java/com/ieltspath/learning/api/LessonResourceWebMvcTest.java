package com.ieltspath.learning.api;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.controller.LessonResourceController;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient.LessonResources;
import com.ieltspath.learning.application.port.LearningContentClient.ResourceLink;
import com.ieltspath.learning.application.usecase.GetLessonResourcesUseCase;
import com.ieltspath.learning.domain.exception.LearningGateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LessonResourceController.class, properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "spring.jackson.default-property-inclusion=non_null"
})
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
class LessonResourceWebMvcTest {
    private static final byte[] INTERNAL_KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    private static final UUID USER = UUID.randomUUID();
    private static final UUID LESSON = UUID.randomUUID();

    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean GetLessonResourcesUseCase getLessonResources;

    @BeforeEach
    void verifiedIdentity() {
        when(currentUser.requireUserId()).thenReturn(USER);
    }

    @Test
    void returnsSummarySlidesAndDocumentsForTheCurrentLearner() throws Exception {
        when(getLessonResources.execute(USER, LESSON)).thenReturn(new LessonResources(LESSON, "## Outcomes",
                List.of(new ResourceLink(UUID.randomUUID(), "Slide", "https://docs.google.com/presentation/d/x", 0)),
                List.of(new ResourceLink(UUID.randomUUID(), "Giáo trình", "https://drive.google.com/x", 0))));

        mvc.perform(get("/api/learning/lessons/{id}/resources", LESSON).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(LESSON.toString()))
                .andExpect(jsonPath("$.summaryMarkdown").value("## Outcomes"))
                .andExpect(jsonPath("$.slides[0].title").value("Slide"))
                .andExpect(jsonPath("$.documents[0].url").value("https://drive.google.com/x"));
    }

    @Test
    void aMissingSummaryIsSerializedAsNull() throws Exception {
        when(getLessonResources.execute(USER, LESSON)).thenReturn(new LessonResources(LESSON, null, null, null));

        mvc.perform(get("/api/learning/lessons/{id}/resources", LESSON).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaryMarkdown").value(nullValue()))
                .andExpect(jsonPath("$.slides").isEmpty())
                .andExpect(jsonPath("$.documents").isEmpty());
    }

    @Test
    void aLockedLessonIsForbiddenWithItsGateCode() throws Exception {
        when(getLessonResources.execute(USER, LESSON)).thenThrow(new LearningGateException("LESSON_LOCKED", List.of()));

        mvc.perform(get("/api/learning/lessons/{id}/resources", LESSON).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LESSON_LOCKED"));
    }

    @Test
    void anUnknownLessonIsNotFoundAndAnonymousIsUnauthorized() throws Exception {
        when(getLessonResources.execute(USER, LESSON))
                .thenThrow(new LearningRequestException(404, "NOT_FOUND", "Lesson was not found"));

        mvc.perform(get("/api/learning/lessons/{id}/resources", LESSON).with(jwt())).andExpect(status().isNotFound());
        mvc.perform(get("/api/learning/lessons/{id}/resources", LESSON)).andExpect(status().isUnauthorized());
    }

    private static org.hamcrest.Matcher<Object> nullValue() {
        return org.hamcrest.Matchers.nullValue();
    }
}
