package com.group01.access.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.access.api.dto.request.DebitPointsRequest;
import com.group01.access.api.dto.request.RefundPointsRequest;
import com.group01.access.api.exception.GlobalExceptionHandler;
import com.group01.access.application.command.ConsumeHumanGradingCreditCommand;
import com.group01.access.application.command.DebitPointsCommand;
import com.group01.access.application.command.RefundPointsCommand;
import com.group01.access.application.result.PointLedgerResult;
import com.group01.access.application.result.UserEntitlementResult;
import com.group01.access.application.usecase.ConsumeHumanGradingCreditUseCase;
import com.group01.access.application.usecase.DebitPointsUseCase;
import com.group01.access.application.usecase.GetUserEntitlementUseCase;
import com.group01.access.application.usecase.RefundPointsUseCase;
import com.group01.access.domain.exception.InsufficientPointsException;
import com.group01.access.domain.vo.PointTransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalAccessControllerTest {

    @Mock
    private GetUserEntitlementUseCase getUserEntitlementUseCase;
    @Mock
    private DebitPointsUseCase debitPointsUseCase;
    @Mock
    private RefundPointsUseCase refundPointsUseCase;
    @Mock
    private ConsumeHumanGradingCreditUseCase consumeHumanGradingCreditUseCase;

    @InjectMocks
    private InternalAccessController internalAccessController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(internalAccessController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/access/users/{userId}/entitlement should return entitlement")
    void shouldReturnEntitlement() throws Exception {
        UUID userId = UUID.randomUUID();
        when(getUserEntitlementUseCase.execute(userId)).thenReturn(
                new UserEntitlementResult(userId, true, Instant.now().plusSeconds(3600), 5, 20L, List.of("PREMIUM_CONTENT"))
        );

        mockMvc.perform(get("/api/access/users/" + userId + "/entitlement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.isPremium").value(true))
                .andExpect(jsonPath("$.remainingHumanGradingCredits").value(5))
                .andExpect(jsonPath("$.pointBalance").value(20));
    }

    @Test
    @DisplayName("POST /api/access/points/debit should debit points and return 200")
    void shouldDebitPoints() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        DebitPointsRequest request = new DebitPointsRequest(userId, 3, "GRADING_JOB", refId, "idem-1", "AI grading");

        when(debitPointsUseCase.execute(any(DebitPointsCommand.class))).thenReturn(
                new PointLedgerResult(UUID.randomUUID(), userId, -3, 17, PointTransactionType.AI_GRADING_DEBIT, "GRADING_JOB", refId, "AI grading", Instant.now())
        );

        mockMvc.perform(post("/api/access/points/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delta").value(-3))
                .andExpect(jsonPath("$.balanceAfter").value(17));
    }

    @Test
    @DisplayName("POST /api/access/points/debit when insufficient points should return 402 Payment Required")
    void shouldReturn402WhenInsufficientPoints() throws Exception {
        UUID userId = UUID.randomUUID();
        DebitPointsRequest request = new DebitPointsRequest(userId, 50, "GRADING_JOB", UUID.randomUUID(), "idem-2", "AI grading");

        when(debitPointsUseCase.execute(any(DebitPointsCommand.class)))
                .thenThrow(new InsufficientPointsException(userId, 10, 50));

        mockMvc.perform(post("/api/access/points/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402));
    }

    @Test
    @DisplayName("POST /api/access/points/refund should refund points and return 200")
    void shouldRefundPoints() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        RefundPointsRequest request = new RefundPointsRequest(userId, 3, "GRADING_JOB", refId, "idem-3", "Refund failed");

        when(refundPointsUseCase.execute(any(RefundPointsCommand.class))).thenReturn(
                new PointLedgerResult(UUID.randomUUID(), userId, 3, 20, PointTransactionType.AI_GRADING_REFUND, "GRADING_JOB", refId, "Refund failed", Instant.now())
        );

        mockMvc.perform(post("/api/access/points/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delta").value(3))
                .andExpect(jsonPath("$.balanceAfter").value(20));
    }

    @Test
    @DisplayName("POST /api/access/users/{userId}/consume-human-grading should consume credit and return remaining")
    void shouldConsumeHumanGrading() throws Exception {
        UUID userId = UUID.randomUUID();
        when(consumeHumanGradingCreditUseCase.execute(any(ConsumeHumanGradingCreditCommand.class))).thenReturn(3);

        mockMvc.perform(post("/api/access/users/" + userId + "/consume-human-grading"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.remainingCredits").value(3));
    }
}
