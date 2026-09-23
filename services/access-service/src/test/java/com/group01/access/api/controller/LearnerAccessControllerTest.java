package com.group01.access.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.access.api.dto.request.ActivateKeyRequest;
import com.group01.access.api.exception.GlobalExceptionHandler;
import com.group01.access.application.command.ActivateKeyCommand;
import com.group01.access.application.result.ActivationResult;
import com.group01.access.application.result.PointWalletResult;
import com.group01.access.application.result.SubscriptionResult;
import com.group01.access.application.usecase.ActivateKeyUseCase;
import com.group01.access.application.usecase.GetPointHistoryUseCase;
import com.group01.access.application.usecase.GetUserPointWalletUseCase;
import com.group01.access.application.usecase.GetUserSubscriptionUseCase;
import com.group01.access.domain.vo.KeyType;
import com.group01.access.domain.vo.SubscriptionStatus;
import com.group01.commonsecurity.currentuser.CurrentUserProvider;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LearnerAccessControllerTest {

    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private GetUserSubscriptionUseCase getUserSubscriptionUseCase;
    @Mock
    private GetUserPointWalletUseCase getUserPointWalletUseCase;
    @Mock
    private GetPointHistoryUseCase getPointHistoryUseCase;
    @Mock
    private ActivateKeyUseCase activateKeyUseCase;

    @InjectMocks
    private LearnerAccessController learnerAccessController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockMvc = MockMvcBuilders.standaloneSetup(learnerAccessController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/access/me/points should return wallet balance")
    void shouldReturnPoints() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(userId);
        when(getUserPointWalletUseCase.execute(userId)).thenReturn(
                new PointWalletResult(userId, 100L, 100L, 0L, Instant.now())
        );

        mockMvc.perform(get("/api/access/me/points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.balance").value(100));
    }

    @Test
    @DisplayName("GET /api/access/me/subscription should return active subscription")
    void shouldReturnSubscription() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(userId);
        UUID subId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        when(getUserSubscriptionUseCase.execute(userId)).thenReturn(Optional.of(
                new SubscriptionResult(
                        subId, userId, planId, "PREMIUM", "Premium Plan", SubscriptionStatus.ACTIVE,
                        Instant.now(), Instant.now().plusSeconds(86400 * 30), 4, 1, 3,
                        Instant.now(), Instant.now()
                )
        ));

        mockMvc.perform(get("/api/access/me/subscription"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(subId.toString()))
                .andExpect(jsonPath("$.planCode").value("PREMIUM"))
                .andExpect(jsonPath("$.remainingCredits").value(3));
    }

    @Test
    @DisplayName("POST /api/access/me/keys/activate should activate key and return result")
    void shouldActivateKey() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(userId);
        ActivateKeyRequest request = new ActivateKeyRequest("IP-TEST-1234-ABCD-5678", "idem-act-1");

        ActivationResult result = new ActivationResult(
                UUID.randomUUID(), UUID.randomUUID(), userId, KeyType.POINTS, 50, 0, 0,
                Instant.now(), 50L, null
        );

        when(activateKeyUseCase.execute(any(ActivateKeyCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/access/me/keys/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productType").value("POINTS"))
                .andExpect(jsonPath("$.pointsGranted").value(50))
                .andExpect(jsonPath("$.newBalance").value(50));
    }
}
