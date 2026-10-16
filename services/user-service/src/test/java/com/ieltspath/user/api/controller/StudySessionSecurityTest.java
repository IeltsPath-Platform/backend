package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.user.application.usecase.*;
import com.ieltspath.user.domain.exception.StudyTrackingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.security.SecureRandom;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudySessionController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties={"spring.cloud.config.enabled=false","app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
class StudySessionSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean StartStudySessionUseCase start;
    @MockBean RecordStudyHeartbeatUseCase heartbeat;
    @MockBean GetStudyTimeStatisticsUseCase statistics;
    @DynamicPropertySource static void key(DynamicPropertyRegistry registry) {
        byte[] key=new byte[32]; new SecureRandom().nextBytes(key);
        registry.add("app.auth.internal-jwt-secret",()->Base64.getEncoder().encodeToString(key));
    }
    @Test void anonymousStartIsUnauthorized() throws Exception {
        mvc.perform(post("/api/users/me/study-sessions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(start);
    }
    @Test void startUsesAuthenticatedIdentityAndIgnoresUntrustedIdentityHeaders() throws Exception {
        UUID user=UUID.randomUUID(),client=UUID.randomUUID(),source=UUID.randomUUID();
        mvc.perform(post("/api/users/me/study-sessions").with(jwt().jwt(j->j.subject(user.toString())))
                .header("X-User-Id",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"clientSessionId\":\""+client+"\",\"sourceType\":\"LESSON\",\"sourceId\":\""+source+"\"}"))
                .andExpect(status().isOk());
        verify(start).execute(user,client,"LESSON",source,false);
    }
    @Test void malformedSourceAndIntervalAreRejectedBeforeUseCase() throws Exception {
        mvc.perform(post("/api/users/me/study-sessions").with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))
                .contentType(MediaType.APPLICATION_JSON).content("{\"sourceType\":\"MOCK\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/users/me/study-sessions/"+UUID.randomUUID()+"/heartbeats")
                .with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))
                .contentType(MediaType.APPLICATION_JSON).content("{\"epoch\":1,\"sequence\":1,\"intervals\":[null]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(start,heartbeat);
    }
    @Test void foreignSessionAndLeaseConflictUseExistingErrorEnvelope() throws Exception {
        when(heartbeat.execute(any(),any(),anyLong(),anyLong(),anyList(),anyBoolean()))
                .thenThrow(new StudyTrackingException("SESSION_NOT_FOUND"));
        mvc.perform(post("/api/users/me/study-sessions/"+UUID.randomUUID()+"/close")
                .with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))
                .contentType(MediaType.APPLICATION_JSON).content("{\"epoch\":1,\"sequence\":1,\"intervals\":[]}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.details.code").value("SESSION_NOT_FOUND"));
    }
    @Test void badStatisticsDateIsBadRequest() throws Exception {
        mvc.perform(get("/api/users/me/statistics/study-time?from=invalid&to=2026-10-15")
                .with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isBadRequest());
    }
    @Test void vocabularyReviewIsRejectedBeforeStartingASession() throws Exception {
        mvc.perform(post("/api/users/me/study-sessions")
                .with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"clientSessionId\":\""+UUID.randomUUID()+"\",\"sourceType\":\"VOCABULARY_REVIEW\",\"sourceId\":\""+UUID.randomUUID()+"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(start);
    }
}
