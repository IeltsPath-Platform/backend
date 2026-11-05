package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.learning.application.command.ChangeLearningGoalStatusCommand;
import com.ieltspath.learning.application.result.LearningGoalResult;
import com.ieltspath.learning.application.usecase.ChangeLearningGoalStatusUseCase;
import com.ieltspath.learning.application.usecase.GetLearningGoalsByUserIdUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Staff can read and change a learner's goals; the same data the learner manages through /api/learning/goals. */
@WebMvcTest(controllers = AdminLearningGoalController.class, properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
class AdminLearningGoalControllerTest {
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean GetLearningGoalsByUserIdUseCase goals;
    @MockitoBean ChangeLearningGoalStatusUseCase changeStatus;

    private final UUID learner = UUID.randomUUID();
    private final UUID goalId = UUID.randomUUID();

    private LearningGoalResult result(String status) {
        return new LearningGoalResult(goalId, learner, new BigDecimal("7.5"), LocalDate.parse("2027-03-01"), 60, status,
                LocalDateTime.parse("2026-10-01T08:00:00"), null, LocalDateTime.parse("2026-10-01T08:00:00"),
                LocalDateTime.parse("2026-10-01T08:00:00"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anAdminListsALearnersGoals() throws Exception {
        when(goals.execute(learner)).thenReturn(List.of(result("ACTIVE")));

        mockMvc.perform(get("/api/learning/admin/users/{id}/goals", learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(goalId.toString()))
                .andExpect(jsonPath("$[0].userId").value(learner.toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anAdminChangesTheStatusOfALearnersGoalWithoutActingAsTheLearner() throws Exception {
        when(changeStatus.execute(any(ChangeLearningGoalStatusCommand.class))).thenReturn(result("PAUSED"));

        mockMvc.perform(put("/api/learning/admin/users/{id}/goals/{goalId}/status", learner, goalId)
                        .contentType(APPLICATION_JSON).content("{\"status\":\"PAUSED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAUSED"));

        var command = org.mockito.ArgumentCaptor.forClass(ChangeLearningGoalStatusCommand.class);
        verify(changeStatus).execute(command.capture());
        org.junit.jupiter.api.Assertions.assertEquals(goalId, command.getValue().goalId());
        org.junit.jupiter.api.Assertions.assertNull(command.getValue().userId());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void aLearnerCannotUseTheStaffRoutes() throws Exception {
        mockMvc.perform(get("/api/learning/admin/users/{id}/goals", learner)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/learning/admin/users/{id}/goals/{goalId}/status", learner, goalId)
                .contentType(APPLICATION_JSON).content("{\"status\":\"PAUSED\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(goals, changeStatus);
    }

    @Test
    void anonymousCallersAreRefused() throws Exception {
        mockMvc.perform(get("/api/learning/admin/users/{id}/goals", learner)).andExpect(status().isUnauthorized());
        verifyNoInteractions(goals, changeStatus);
    }
}
