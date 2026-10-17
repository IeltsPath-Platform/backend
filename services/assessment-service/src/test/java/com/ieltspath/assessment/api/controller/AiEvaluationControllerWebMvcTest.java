package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.service.AiEvaluationService;
import com.ieltspath.assessment.application.port.PromptActivationProvider;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AiEvaluationController.class, InternalPromptActivationController.class},
        properties = "spring.cloud.config.enabled=false")
@Import(AiEvaluationControllerWebMvcTest.MethodSecurityConfig.class)
class AiEvaluationControllerWebMvcTest {
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                    .build();
        }
    }

    @Autowired MockMvc mvc;
    @MockitoBean AiEvaluationService service;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean PromptActivationProvider activations;

    @Test @WithMockUser(roles = "ADMIN") void adminCanReadEvaluationRun() throws Exception {
        UUID run = UUID.randomUUID();
        when(service.getRun(run)).thenReturn(Map.of("id", run, "status", "DONE"));
        mvc.perform(get("/api/assessments/admin/ai-evaluations/{id}", run))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test @WithMockUser(roles = "CUSTOMER") void nonAdminCannotAccessEvaluationRoutes() throws Exception {
        mvc.perform(get("/api/assessments/admin/ai-evaluations/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "CUSTOMER") void internalLookupDistinguishesBootstrapAndActivatedTasks() throws Exception {
        when(activations.activatedVersion("TASK_2")).thenReturn("IELTS_WRITING_TASK_2@1+tplv1");
        mvc.perform(get("/internal/assessments/ai-prompt-activations").param("task", "TASK_1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.task").value("TASK_1"))
                .andExpect(jsonPath("$.promptVersion").isEmpty());
        mvc.perform(get("/internal/assessments/ai-prompt-activations").param("task", "TASK_2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.promptVersion").value("IELTS_WRITING_TASK_2@1+tplv1"));
    }
}
