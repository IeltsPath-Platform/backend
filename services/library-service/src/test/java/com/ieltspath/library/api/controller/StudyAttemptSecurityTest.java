package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.library.application.exception.StudyRequestException;
import com.ieltspath.library.application.result.StudyAttemptResult;
import com.ieltspath.library.application.usecase.*;
import com.ieltspath.library.domain.study.StudyAnswer;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VocabularyStudyController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class StudyAttemptSecurityTest {
    private static final byte[] KEY = new byte[32];
    private static final UUID USER = UUID.randomUUID(), UNIT = UUID.randomUUID(), ATTEMPT = UUID.randomUUID();
    private static final UUID STEP = UUID.randomUUID(), REQUEST = UUID.randomUUID();
    private static final String ROOT = "/api/learning-support/vocabulary-study";
    static { new SecureRandom().nextBytes(KEY); }

    @Autowired MockMvc mvc;
    @MockitoBean StartStudyAttemptUseCase start;
    @MockitoBean GetStudyAttemptUseCase get;
    @MockitoBean SubmitStudyAnswerUseCase answer;
    @MockitoBean CompleteStudyAttemptUseCase complete;
    @MockitoBean GetStudyProgressUseCase progress;

    @DynamicPropertySource static void key(DynamicPropertyRegistry properties) {
        properties.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Test
    void everyAttemptAndProgressEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get(ROOT + "/progress").param("topicIds", UNIT.toString())).andExpect(status().isUnauthorized());
        mvc.perform(get(ROOT + "/attempts/{id}", ATTEMPT)).andExpect(status().isUnauthorized());
        mvc.perform(post(ROOT + "/units/{id}/attempts", UNIT).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(ROOT + "/attempts/{id}/steps/{step}/answers", ATTEMPT, STEP)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(ROOT + "/attempts/{id}/complete", ATTEMPT).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(start, get, answer, complete, progress);
    }

    @Test
    void newlyCreatedAttemptReturnsCreatedAndResumedAttemptReturnsOk() throws Exception {
        var attempt = mock(StudyAttemptResult.Attempt.class);
        when(start.execute(USER, UNIT, REQUEST, "RESUME_OR_START"))
                .thenReturn(new StudyAttemptResult.Start(attempt, true), new StudyAttemptResult.Start(attempt, false));
        String body = "{\"requestId\":\"" + REQUEST + "\",\"mode\":\"RESUME_OR_START\",\"userId\":\"" + UUID.randomUUID() + "\"}";
        mvc.perform(post(ROOT + "/units/{id}/attempts", UNIT).header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post(ROOT + "/units/{id}/attempts", UNIT).header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        verify(start, times(2)).execute(USER, UNIT, REQUEST, "RESUME_OR_START");
    }

    @Test
    void rejectsMalformedStartWithoutCallingTheUseCase() throws Exception {
        for (String body : List.of("{}", "{\"requestId\":\"bad\",\"mode\":\"REPLAY\"}",
                "{\"requestId\":\"" + REQUEST + "\",\"mode\":\"ADMIN\"}")) {
            mvc.perform(post(ROOT + "/units/{id}/attempts", UNIT).header(HttpHeaders.AUTHORIZATION, token())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(start);
    }

    @Test
    void missingOrForeignAttemptUsesTheSameCodedNotFoundResponse() throws Exception {
        when(get.execute(USER, ATTEMPT)).thenThrow(StudyRequestException.missing("ATTEMPT_NOT_FOUND"));
        mvc.perform(get(ROOT + "/attempts/{id}", ATTEMPT).header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ATTEMPT_NOT_FOUND"));
        verify(get).execute(USER, ATTEMPT);
    }

    @Test
    void answerUsesOnlyTheAuthenticatedUserAndForwardsTheSelectedOption() throws Exception {
        UUID option = UUID.randomUUID();
        var selected = new StudyAnswer("CHOICE", null, option, null, null);
        mvc.perform(post(ROOT + "/attempts/{id}/steps/{step}/answers", ATTEMPT, STEP)
                        .header(HttpHeaders.AUTHORIZATION, token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + REQUEST + "\",\"userId\":\"" + UUID.randomUUID()
                                + "\",\"answer\":{\"type\":\"CHOICE\",\"optionId\":\"" + option + "\"}}"))
                .andExpect(status().isOk());
        verify(answer).execute(USER, ATTEMPT, STEP, REQUEST, selected);
    }

    @Test
    void businessAnswerErrorsIncludeTheirCodeAndStringDetails() throws Exception {
        when(answer.execute(eq(USER), eq(ATTEMPT), eq(STEP), eq(REQUEST), any()))
                .thenThrow(new StudyRequestException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ANSWER",
                        "Phương án không hợp lệ.", Map.of("field", "answer.optionId")));
        mvc.perform(post(ROOT + "/attempts/{id}/steps/{step}/answers", ATTEMPT, STEP)
                        .header(HttpHeaders.AUTHORIZATION, token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + REQUEST + "\",\"answer\":{\"type\":\"CHOICE\"}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_ANSWER"))
                .andExpect(jsonPath("$.details.field").value("answer.optionId"));
    }

    @Test
    void rejectsMissingAnswerOrCompletionRequestId() throws Exception {
        mvc.perform(post(ROOT + "/attempts/{id}/steps/{step}/answers", ATTEMPT, STEP)
                        .header(HttpHeaders.AUTHORIZATION, token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + REQUEST + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(ROOT + "/attempts/{id}/complete", ATTEMPT).header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(answer, complete);
    }

    @Test
    void completionForwardsTheLogicalRequestIdAndTokenUser() throws Exception {
        mvc.perform(post(ROOT + "/attempts/{id}/complete", ATTEMPT).header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"requestId\":\"" + REQUEST + "\"}"))
                .andExpect(status().isOk());
        verify(complete).execute(USER, ATTEMPT, REQUEST);
    }

    @Test
    void progressBatchesTopicIdsAndRejectsMalformedOrOversizedBatches() throws Exception {
        UUID other = UUID.randomUUID();
        when(progress.execute(USER, List.of(UNIT, other))).thenReturn(List.of());
        mvc.perform(get(ROOT + "/progress").param("topicIds", UNIT + "," + other)
                        .header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(progress).execute(USER, List.of(UNIT, other));
        clearInvocations(progress);
        for (String ids : List.of("invalid", IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.joining(",")))) {
            mvc.perform(get(ROOT + "/progress").param("topicIds", ids).header(HttpHeaders.AUTHORIZATION, token()))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(progress);
    }

    private static String token() {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("urn:code-base:api-gateway").subject(USER.toString())
                .issuedAt(now).expiresAt(now.plusSeconds(120)).claim("roles", List.of("CUSTOMER")).build();
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(KEY, "HmacSHA256")));
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
