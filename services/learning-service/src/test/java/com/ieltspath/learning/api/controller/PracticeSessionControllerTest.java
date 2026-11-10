package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.learning.application.result.PracticeSessionResult;
import com.ieltspath.learning.application.usecase.OpenPracticeSessionUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PracticeSessionController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties={"spring.cloud.config.enabled=false","app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
class PracticeSessionControllerTest {
    @Autowired MockMvc mvc;
    @MockBean OpenPracticeSessionUseCase open;

    @DynamicPropertySource static void key(DynamicPropertyRegistry registry) {
        byte[] key = new byte[32]; new SecureRandom().nextBytes(key);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(key));
    }

    @Test
    void requiresCustomerRole() throws Exception {
        mvc.perform(post("/api/learning/practice-sessions")
                .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\":\"" + UUID.randomUUID() + "\",\"kind\":\"WRITING\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(open);
    }

    @Test
    void validatesKindAndReturnsOnlyPracticeSessionFields() throws Exception {
        UUID user = UUID.randomUUID(), requestId = UUID.randomUUID(), id = UUID.randomUUID();
        when(open.execute(user, "VOCABULARY", requestId))
                .thenReturn(new PracticeSessionResult(id, "VOCABULARY", Instant.parse("2026-11-09T16:00:00Z")));
        mvc.perform(post("/api/learning/practice-sessions")
                .with(jwt().jwt(jwt -> jwt.subject(user.toString())).authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\":\"" + requestId + "\",\"kind\":\"VOCABULARY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.kind").value("VOCABULARY"))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-09T16:00:00Z"))
                .andExpect(jsonPath("$.userId").doesNotExist());
        mvc.perform(post("/api/learning/practice-sessions")
                .with(jwt().jwt(jwt -> jwt.subject(user.toString())).authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\":\"" + UUID.randomUUID() + "\",\"kind\":\"OTHER\"}"))
                .andExpect(status().isBadRequest());
        verify(open).execute(user, "VOCABULARY", requestId);
    }
}
