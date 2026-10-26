package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.ScoreConfigResult;
import com.ieltspath.learning.application.usecase.ManageScoreConfigUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ScoreConfigAdminController.class, properties = {"spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class ScoreConfigAdminControllerTest {
    static final byte[] KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    private static final String PATH = "/api/learning/admin/score-config";
    private static final String BODY = """
            {"processWeight":0.2,"midWeight":0.3,"finalWeight":0.5,"practiceShare":0.64,"practiceTestShare":0.36,
             "passThreshold":0.7,"midRemediationBelow":0.6}""";
    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean ManageScoreConfigUseCase config;
    final UUID admin = UUID.randomUUID();
    final UUID course = UUID.randomUUID();

    private ScoreConfigResult result(UUID courseId) {
        return new ScoreConfigResult(courseId, false, new BigDecimal("0.2"), new BigDecimal("0.3"),
                new BigDecimal("0.5"), new BigDecimal("0.64"), new BigDecimal("0.36"), new BigDecimal("0.7"),
                new BigDecimal("0.6"));
    }

    @Test
    void anAdminReadsTheConfigurationOfACourseOrTheDefault() throws Exception {
        when(config.get(course)).thenReturn(result(course));
        when(config.get(null)).thenReturn(result(null));

        mvc.perform(get(PATH).param("courseId", course.toString()).with(user("author").roles("CONTENT_AUTHOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passThreshold").value(0.7))
                .andExpect(jsonPath("$.courseId").value(course.toString()));
        mvc.perform(get(PATH).with(user("author").roles("CONTENT_AUTHOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.courseId").doesNotExist());
    }

    @Test
    void anUpdateIsAttributedToTheVerifiedAdmin() throws Exception {
        when(currentUser.requireUserId()).thenReturn(admin);
        when(config.update(eq(admin), eq(course), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(result(course));

        mvc.perform(put(PATH).param("courseId", course.toString()).with(user("author").roles("CONTENT_AUTHOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
        verify(config).update(eq(admin), eq(course), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void weightsThatDoNotAddUpToOneComeBackAs400WithTheirCode() throws Exception {
        when(currentUser.requireUserId()).thenReturn(admin);
        when(config.update(eq(admin), eq(null), any(), any(), any(), any(), any(), any(), any())).thenThrow(
                new LearningRequestException(400, "INVALID_SCORE_CONFIG", "weights must add up to 1"));

        mvc.perform(put(PATH).with(user("author").roles("CONTENT_AUTHOR")).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("0.5,", "0.6,")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_SCORE_CONFIG"));
    }

    @Test
    void aRequestWithAMissingValueIsRejectedBeforeTheUseCase() throws Exception {
        mvc.perform(put(PATH).with(user("author").roles("CONTENT_AUTHOR")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processWeight\":0.2}"))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(config);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "CUSTOMER", "EXAMINER", "SALES_STAFF"})
    void onlyAnAdminCanReadOrChangeTheConfiguration(String role) throws Exception {
        mvc.perform(get(PATH).with(user("other").roles(role))).andExpect(status().isForbidden());
        mvc.perform(put(PATH).with(user("other").roles(role)).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(config);
    }

    @Test
    void unauthenticatedCallersAreRejected() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }
}
