package com.ieltspath.access.api.controller;

import com.ieltspath.access.api.exception.GlobalExceptionHandler;
import com.ieltspath.access.application.command.AdjustPointsCommand;
import com.ieltspath.access.application.result.PointLedgerResult;
import com.ieltspath.access.application.usecase.AdjustPointsUseCase;
import com.ieltspath.access.domain.vo.PointTransactionType;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminAccessControllerTest {

    @Mock
    private AdjustPointsUseCase adjustPointsUseCase;

    @InjectMocks
    private AdminAccessController adminAccessController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAccessController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/access/admin/points/adjust should return the ledger entry")
    void shouldAdjustPoints() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adjustPointsUseCase.execute(any(AdjustPointsCommand.class))).thenReturn(new PointLedgerResult(
                UUID.randomUUID(), userId, 5, 25, PointTransactionType.ADMIN_ADJUSTMENT, "ADMIN", (String) null,
                "Bù point", Instant.now()));

        mockMvc.perform(post("/api/access/admin/points/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\",\"delta\":5,\"reason\":\"Bù point\","
                                + "\"idempotencyKey\":\"adjust-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delta").value(5));
    }
}
