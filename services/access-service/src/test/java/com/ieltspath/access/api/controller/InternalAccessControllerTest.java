package com.ieltspath.access.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.access.api.dto.request.DebitPointsRequest;
import com.ieltspath.access.api.dto.request.RefundPointsRequest;
import com.ieltspath.access.application.command.DebitPointsCommand;
import com.ieltspath.access.application.result.PointLedgerResult;
import com.ieltspath.access.application.result.UserEntitlementResult;
import com.ieltspath.access.application.usecase.ConsumeHumanGradingCreditUseCase;
import com.ieltspath.access.application.usecase.DebitPointsUseCase;
import com.ieltspath.access.application.usecase.GetUserEntitlementUseCase;
import com.ieltspath.access.application.usecase.RefundPointsUseCase;
import com.ieltspath.access.domain.exception.InsufficientPointsException;
import com.ieltspath.access.domain.exception.PointsActorMismatchException;
import com.ieltspath.access.domain.vo.PointTransactionType;
import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {InternalAccessController.class, InternalPointsController.class}, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Import(CommonSecurityAutoConfiguration.class)
class InternalAccessControllerTest {
    private static final byte[] INTERNAL_KEY = new byte[32];

    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean CurrentUserProvider currentUserProvider;
    @MockitoBean GetUserEntitlementUseCase getUserEntitlementUseCase;
    @MockitoBean DebitPointsUseCase debitPointsUseCase;
    @MockitoBean RefundPointsUseCase refundPointsUseCase;
    @MockitoBean ConsumeHumanGradingCreditUseCase consumeHumanGradingCreditUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID userId = UUID.randomUUID();

    private String debitBody() throws Exception {
        return objectMapper.writeValueAsString(
                new DebitPointsRequest(userId, 3, "LESSON_WRITING", UUID.randomUUID(), "idem-1", "Writing grading"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void debitUsesInternalPathAndPassesTokenSubjectAsActor() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(userId);
        when(debitPointsUseCase.execute(any())).thenReturn(new PointLedgerResult(UUID.randomUUID(), userId, -3, 17,
                PointTransactionType.AI_GRADING_DEBIT, "LESSON_WRITING", UUID.randomUUID(), "Writing grading", Instant.now()));

        mockMvc.perform(post("/internal/access/points/debit").contentType(MediaType.APPLICATION_JSON).content(debitBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delta").value(-3));

        ArgumentCaptor<DebitPointsCommand> captor = ArgumentCaptor.forClass(DebitPointsCommand.class);
        verify(debitPointsUseCase).execute(captor.capture());
        assertThat(captor.getValue().actorUserId()).isEqualTo(userId);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void oldPublicDebitPathIsGone() throws Exception {
        mockMvc.perform(post("/api/access/points/debit").contentType(MediaType.APPLICATION_JSON).content(debitBody()))
                .andExpect(status().isNotFound());
        verifyNoInteractions(debitPointsUseCase);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void debitForAnotherUserIs403AndInsufficientIs402() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(UUID.randomUUID());
        when(debitPointsUseCase.execute(any()))
                .thenThrow(new PointsActorMismatchException())
                .thenThrow(new InsufficientPointsException(userId, 1, 3));

        mockMvc.perform(post("/internal/access/points/debit").contentType(MediaType.APPLICATION_JSON).content(debitBody()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/access/points/debit").contentType(MediaType.APPLICATION_JSON).content(debitBody()))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotRefundConsumeOrReadEntitlement() throws Exception {
        String refund = objectMapper.writeValueAsString(
                new RefundPointsRequest(userId, 3, "LESSON_WRITING", UUID.randomUUID(), "idem-2", "Refund"));

        mockMvc.perform(post("/api/access/points/refund").contentType(MediaType.APPLICATION_JSON).content(refund))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/access/users/{id}/consume-human-grading", userId)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/access/users/{id}/entitlement", userId)).andExpect(status().isForbidden());
        verifyNoInteractions(refundPointsUseCase, consumeHumanGradingCreditUseCase, getUserEntitlementUseCase);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminKeepsEntitlementAndConsume() throws Exception {
        when(getUserEntitlementUseCase.execute(userId)).thenReturn(
                new UserEntitlementResult(userId, true, Instant.now().plusSeconds(3600), 5, 20L, List.of("PREMIUM_CONTENT")));
        when(consumeHumanGradingCreditUseCase.execute(any())).thenReturn(4);

        mockMvc.perform(get("/api/access/users/{id}/entitlement", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointBalance").value(20));
        mockMvc.perform(post("/api/access/users/{id}/consume-human-grading", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCredits").value(4));
    }
}
