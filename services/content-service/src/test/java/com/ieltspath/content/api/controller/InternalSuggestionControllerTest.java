package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.content.application.result.SuggestionCandidatesResult;
import com.ieltspath.content.application.usecase.GetSuggestionCandidatesUseCase;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalSuggestionController.class, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class InternalSuggestionControllerTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean GetSuggestionCandidatesUseCase candidates;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/internal/learning-content/suggestions/candidates")
                        .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID(), "5.5")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(candidates);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void returnsPackageAndLessonCandidatesForOneBatch() throws Exception {
        UUID kp = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        BigDecimal maxBand = new BigDecimal("5.5");
        when(candidates.execute(List.of(kp), maxBand)).thenReturn(new SuggestionCandidatesResult(
                List.of(new SuggestionCandidatesResult.PackageCandidate(packageId, versionId,
                        PackageType.PRACTICE_TEST, "Practice test", List.of(kp), maxBand, Skill.READING)),
                List.of(new SuggestionCandidatesResult.LessonCandidate(lessonId, topicId,
                        "Reading lesson", List.of(kp), maxBand, Skill.READING, 2))));

        mockMvc.perform(post("/internal/learning-content/suggestions/candidates")
                        .contentType(MediaType.APPLICATION_JSON).content(body(kp, "5.5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packages[0].packageId").value(packageId.toString()))
                .andExpect(jsonPath("$.packages[0].packageVersionId").value(versionId.toString()))
                .andExpect(jsonPath("$.packages[0].type").value("PRACTICE_TEST"))
                .andExpect(jsonPath("$.packages[0].knowledgePointIds[0]").value(kp.toString()))
                .andExpect(jsonPath("$.packages[0].band").value(5.5))
                .andExpect(jsonPath("$.packages[0].skill").value("READING"))
                .andExpect(jsonPath("$.lessons[0].lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$.lessons[0].topicId").value(topicId.toString()))
                .andExpect(jsonPath("$.lessons[0].knowledgePointIds[0]").value(kp.toString()))
                .andExpect(jsonPath("$.lessons[0].sortOrder").value(2));
        verify(candidates).execute(List.of(kp), maxBand);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void rejectsMissingOversizedAndOutOfRangeBatch() throws Exception {
        UUID kp = UUID.randomUUID();
        String fiftyOne = String.join(",", java.util.Collections.nCopies(51, "\"" + kp + "\""));
        for (String body : List.of("{\"knowledgePointIds\":[],\"maxBand\":5.5}",
                "{\"knowledgePointIds\":[" + fiftyOne + "],\"maxBand\":5.5}",
                body(kp, "9.5"), "{\"knowledgePointIds\":[\"" + kp + "\"]}")) {
            mockMvc.perform(post("/internal/learning-content/suggestions/candidates")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(candidates);
    }

    private static String body(UUID kp, String band) {
        return "{\"knowledgePointIds\":[\"" + kp + "\"],\"maxBand\":" + band + "}";
    }
}
