package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.user.application.command.UpdateExaminerProfileCommand;
import com.ieltspath.user.application.result.ExaminerResult;
import com.ieltspath.user.application.usecase.ExaminerProfilesUseCase;
import com.ieltspath.user.domain.exception.ResourceNotFoundException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ExaminerController.class, properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
class ExaminerControllerSecurityTest {
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }
    @Autowired MockMvc mvc;
    @MockBean ExaminerProfilesUseCase useCase;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Test
    void nobodyWithoutAVerifiedTokenReachesAnyEndpoint() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(get("/api/users/examiners").header("X-User-Id", id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/examiners/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/users/examiners/{id}", id).header("X-User-Id", id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/examiners").header(HttpHeaders.AUTHORIZATION, token(id, "CUSTOMER", "urn:code-base:auth")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }

    @Test
    void aLearnerSeesTheExaminersWhoTakeSubmissionsWithoutTheWorkloadLimit() throws Exception {
        UUID learner = UUID.randomUUID(), examiner = UUID.randomUUID();
        when(useCase.accepting()).thenReturn(List.of(result(examiner, true)));
        when(useCase.get(examiner)).thenReturn(result(examiner, false));

        mvc.perform(get("/api/users/examiners").header(HttpHeaders.AUTHORIZATION, token(learner, "CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(examiner.toString()))
                .andExpect(jsonPath("$[0].displayName").value("Ms Lan"))
                .andExpect(jsonPath("$[0].ieltsBand").value(8.0))
                .andExpect(jsonPath("$[0].certificates[0].name").value("CELTA"))
                .andExpect(jsonPath("$[0].acceptingSubmissions").value(true))
                .andExpect(jsonPath("$[0].dailyLimit").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist());
        mvc.perform(get("/api/users/examiners/{id}", examiner).header(HttpHeaders.AUTHORIZATION, token(learner, "CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.acceptingSubmissions").value(false));
    }

    @Test
    void anUnknownExaminerIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(useCase.get(id)).thenThrow(new ResourceNotFoundException());
        mvc.perform(get("/api/users/examiners/{id}", id).header(HttpHeaders.AUTHORIZATION, token(UUID.randomUUID(), "CUSTOMER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyAnExaminerEditsTheirProfileAndTheVerifiedUserIsTheOneEdited() throws Exception {
        UUID examiner = UUID.randomUUID();
        String body = "{\"ieltsBand\":8.5,\"acceptingSubmissions\":true,"
                + "\"certificates\":[{\"name\":\"CELTA\",\"issuer\":\"Cambridge\",\"year\":2018,\"score\":\"A\"}]}";
        mvc.perform(put("/api/users/me/examiner-profile").header(HttpHeaders.AUTHORIZATION, token(examiner, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        verify(useCase, never()).updateOwn(any(), any());

        when(useCase.updateOwn(eq(examiner), any(UpdateExaminerProfileCommand.class))).thenReturn(result(examiner, true));
        mvc.perform(put("/api/users/me/examiner-profile").header(HttpHeaders.AUTHORIZATION, token(examiner, "EXAMINER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(examiner.toString()));
        var command = org.mockito.ArgumentCaptor.forClass(UpdateExaminerProfileCommand.class);
        verify(useCase).updateOwn(eq(examiner), command.capture());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("8.5"), command.getValue().ieltsBand());
        org.junit.jupiter.api.Assertions.assertEquals("CELTA", command.getValue().certificates().getFirst().name());
    }

    @Test
    void theProfileRequestIsValidated() throws Exception {
        String token = token(UUID.randomUUID(), "EXAMINER");
        for (String body : List.of("{\"ieltsBand\":9.5,\"acceptingSubmissions\":true}",
                "{\"acceptingSubmissions\":null}", "{\"ieltsBand\":7.0}",
                "{\"acceptingSubmissions\":true,\"certificates\":[{\"name\":\"\"}]}")) {
            mvc.perform(put("/api/users/me/examiner-profile").header(HttpHeaders.AUTHORIZATION, token)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void onlyAnAdministratorSetsTheCapacity() throws Exception {
        UUID examiner = UUID.randomUUID();
        mvc.perform(put("/api/users/{id}/examiner-profile/capacity", examiner)
                        .header(HttpHeaders.AUTHORIZATION, token(examiner, "EXAMINER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dailyLimit\":20}"))
                .andExpect(status().isForbidden());
        when(useCase.setCapacity(examiner, 20)).thenReturn(result(examiner, true));
        mvc.perform(put("/api/users/{id}/examiner-profile/capacity", examiner)
                        .header(HttpHeaders.AUTHORIZATION, token(UUID.randomUUID(), "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dailyLimit\":20}"))
                .andExpect(status().isOk());
        for (String body : List.of("{\"dailyLimit\":0}", "{\"dailyLimit\":51}", "{}")) {
            mvc.perform(put("/api/users/{id}/examiner-profile/capacity", examiner)
                    .header(HttpHeaders.AUTHORIZATION, token(UUID.randomUUID(), "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void assessmentAsksWhetherAnExaminerAcceptsAndHowManyMayBeOpen() throws Exception {
        UUID examiner = UUID.randomUUID();
        when(useCase.get(examiner)).thenReturn(result(examiner, true));
        mvc.perform(get("/internal/users/examiners/{id}", examiner)
                        .header(HttpHeaders.AUTHORIZATION, token(UUID.randomUUID(), "CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(examiner.toString()))
                .andExpect(jsonPath("$.acceptingSubmissions").value(true)).andExpect(jsonPath("$.dailyLimit").value(10))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void assessmentAsksAboutSeveralExaminersInOneCallAndEachAnswerHoldsOnlyWhatItNeeds() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(useCase.getAll(List.of(first, second))).thenReturn(List.of(result(first, true), result(second, false)));

        mvc.perform(get("/internal/users/examiners").param("ids", first + "," + second + "," + first)
                        .header(HttpHeaders.AUTHORIZATION, token(UUID.randomUUID(), "CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(first.toString())).andExpect(jsonPath("$[0].acceptingSubmissions").value(true))
                .andExpect(jsonPath("$[1].acceptingSubmissions").value(false)).andExpect(jsonPath("$[0].dailyLimit").value(10))
                .andExpect(jsonPath("$[0].length()").value(3));
        verify(useCase).getAll(List.of(first, second));
    }

    @Test
    void theBatchLookupNeedsAVerifiedTokenAndBetweenOneAndFiftyIds() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(get("/internal/users/examiners").param("ids", id.toString())).andExpect(status().isUnauthorized());

        String tooMany = String.join(",", java.util.Collections.nCopies(51, "x").stream().map(x -> UUID.randomUUID().toString()).toList());
        mvc.perform(get("/internal/users/examiners").param("ids", tooMany)
                        .header(HttpHeaders.AUTHORIZATION, token(UUID.randomUUID(), "CUSTOMER")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/internal/users/examiners").param("ids", "")
                        .header(HttpHeaders.AUTHORIZATION, token(UUID.randomUUID(), "CUSTOMER")))
                .andExpect(status().isBadRequest());
        verify(useCase, never()).getAll(any());
    }

    private static ExaminerResult result(UUID id, boolean accepting) {
        return new ExaminerResult(id, "Ms Lan", "https://images.test/lan.png", "Bio", new BigDecimal("8.0"),
                List.of(new ExaminerResult.Certificate("CELTA", "Cambridge", 2018, "A")), accepting, 10);
    }

    private String token(UUID subject, String role) {
        return token(subject, role, "urn:code-base:api-gateway");
    }

    private String token(UUID subject, String role, String issuer) {
        var key = new OctetSequenceKey.Builder(KEY).build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(subject.toString())
                .expiresAt(Instant.now().plusSeconds(60)).claim("roles", List.of(role)).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
