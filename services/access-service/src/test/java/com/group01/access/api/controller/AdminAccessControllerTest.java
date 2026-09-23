package com.group01.access.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.access.api.dto.request.CreatePlanRequest;
import com.group01.access.api.dto.request.GenerateKeysRequest;
import com.group01.access.api.exception.GlobalExceptionHandler;
import com.group01.access.application.command.CreatePlanCommand;
import com.group01.access.application.command.GenerateActivationKeysCommand;
import com.group01.access.application.result.GeneratedKeyItem;
import com.group01.access.application.result.PlanResult;
import com.group01.access.application.usecase.*;
import com.group01.access.domain.vo.PlanStatus;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminAccessControllerTest {

    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private GenerateActivationKeysUseCase generateActivationKeysUseCase;
    @Mock
    private RevokeActivationKeyUseCase revokeActivationKeyUseCase;
    @Mock
    private CreatePlanUseCase createPlanUseCase;
    @Mock
    private CreateKeyProductUseCase createKeyProductUseCase;
    @Mock
    private AdjustPointsUseCase adjustPointsUseCase;
    @Mock
    private GrantSubscriptionUseCase grantSubscriptionUseCase;

    @InjectMocks
    private AdminAccessController adminAccessController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAccessController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/access/admin/keys/generate should generate keys and return list")
    void shouldGenerateKeys() throws Exception {
        UUID productId = UUID.randomUUID();
        GenerateKeysRequest request = new GenerateKeysRequest(productId, 2, null);

        GeneratedKeyItem item1 = new GeneratedKeyItem(UUID.randomUUID(), "IP-AAAA-BBBB-CCCC", "CCCC", null);
        GeneratedKeyItem item2 = new GeneratedKeyItem(UUID.randomUUID(), "IP-DDDD-EEEE-FFFF", "FFFF", null);

        when(generateActivationKeysUseCase.execute(any(GenerateActivationKeysCommand.class))).thenReturn(List.of(item1, item2));

        mockMvc.perform(post("/api/access/admin/keys/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rawKey").value("IP-AAAA-BBBB-CCCC"))
                .andExpect(jsonPath("$[1].codeHint").value("FFFF"));
    }

    @Test
    @DisplayName("POST /api/access/admin/keys/{id}/revoke should return 204")
    void shouldRevokeKey() throws Exception {
        UUID keyId = UUID.randomUUID();
        doNothing().when(revokeActivationKeyUseCase).execute(keyId);

        mockMvc.perform(post("/api/access/admin/keys/" + keyId + "/revoke"))
                .andExpect(status().isNoContent());

        verify(revokeActivationKeyUseCase).execute(keyId);
    }

    @Test
    @DisplayName("POST /api/access/admin/plans should create plan and return 201")
    void shouldCreatePlan() throws Exception {
        CreatePlanRequest request = new CreatePlanRequest("PRO", "Pro Plan");
        PlanResult result = new PlanResult(UUID.randomUUID(), "PRO", "Pro Plan", PlanStatus.ACTIVE, Collections.emptyList(), Instant.now(), Instant.now());

        when(createPlanUseCase.execute(any(CreatePlanCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/access/admin/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PRO"))
                .andExpect(jsonPath("$.name").value("Pro Plan"));
    }
}
