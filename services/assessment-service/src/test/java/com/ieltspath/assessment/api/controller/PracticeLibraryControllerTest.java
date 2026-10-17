package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.port.PracticeAttemptReader;
import com.ieltspath.assessment.application.usecase.GetPracticeHistoryUseCase;
import com.ieltspath.assessment.application.usecase.GetPracticeProgressUseCase;
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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PracticeLibraryController.class, properties = "spring.cloud.config.enabled=false")
@Import({PracticeLibraryControllerTest.TestSecurityConfig.class, GetPracticeHistoryUseCase.class,
        GetPracticeProgressUseCase.class})
class PracticeLibraryControllerTest {
    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                    .build();
        }
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean PracticeAttemptReader reader;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void rejectsInvalidHistoryPageAndSize() throws Exception {
        mockMvc.perform(get("/api/assessments/practice/history").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/assessments/practice/history").param("size", "51"))
                .andExpect(status().isBadRequest());

        verify(reader, never()).findHistory(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @WithMockUser(roles = "CONTENT_AUTHOR")
    void practiceLibraryIsRestrictedToLearnersAndAdmins() throws Exception {
        mockMvc.perform(get("/api/assessments/practice/history"))
                .andExpect(status().isForbidden());

        verify(reader, never()).findHistory(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }
}
