package com.ieltspath.access.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.access.api.exception.GlobalExceptionHandler;
import com.ieltspath.access.application.command.GrantSubscriptionCommand;
import com.ieltspath.access.application.result.SubscriptionResult;
import com.ieltspath.access.application.usecase.*;
import com.ieltspath.access.domain.vo.SubscriptionStatus;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GrantSubscriptionIdempotencyTest {
    private final GrantSubscriptionUseCase grant = mock(GrantSubscriptionUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var controller = new AdminAccessController(mock(CurrentUserProvider.class), mock(GenerateActivationKeysUseCase.class),
                mock(RevokeActivationKeyUseCase.class), mock(CreatePlanUseCase.class), mock(CreateKeyProductUseCase.class),
                mock(AdjustPointsUseCase.class), grant);
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        when(grant.execute(any())).thenReturn(new SubscriptionResult(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "BASIC", "Basic", SubscriptionStatus.ACTIVE, Instant.now(), Instant.now().plusSeconds(86400),
                0, 0, 0, Instant.now(), Instant.now()));
    }

    @Test
    void missingKeyIsBadRequest() throws Exception {
        mvc.perform(post("/api/access/admin/subscriptions/grant").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forwardsIdempotencyKeyToGrantCommand() throws Exception {
        String key = UUID.randomUUID().toString();
        mvc.perform(post("/api/access/admin/subscriptions/grant").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody()))
                .andExpect(status().isCreated());

        var captor = org.mockito.ArgumentCaptor.forClass(GrantSubscriptionCommand.class);
        verify(grant).execute(captor.capture());
        assertThat(captor.getValue().getClass().getMethod("idempotencyKey").invoke(captor.getValue())).isEqualTo(key);
    }

    private static String requestBody() {
        return """
                {"userId":"%s","planId":"%s","durationDays":30,"humanGradingCredits":2}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
    }
}
