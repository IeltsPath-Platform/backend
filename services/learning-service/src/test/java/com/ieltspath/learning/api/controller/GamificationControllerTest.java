package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.service.GamificationRecorder;
import com.ieltspath.learning.application.service.LearnerTimezones;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GamificationController.class, properties = {"spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class GamificationControllerTest {
    static final byte[] KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean GamificationRecorder gamification;
    @MockitoBean LearnerTimezones timezones;
    final UUID learner = UUID.randomUUID();

    @Test
    void progressReturnsXpLevelAndTodaysCountedXp() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(gamification.progress(learner)).thenReturn(
                new GamificationRecorder.Progress(1200, 2, 2000, 800, 60L, 100));

        mvc.perform(get("/api/learning/me/progress").with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(1200))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.xpToNextLevel").value(800))
                .andExpect(jsonPath("$.xpToday").value(60))
                .andExpect(jsonPath("$.dailyCap").value(100));
    }

    @Test
    void theTimezoneIsSavedAndTheSavedValueComesBack() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(timezones.timezoneOf(learner)).thenReturn("Europe/Paris");

        mvc.perform(put("/api/learning/me/settings").with(user("learner").roles("CUSTOMER"))
                        .contentType("application/json").content("{\"timezone\":\"Europe/Paris\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Europe/Paris"));

        verify(timezones).save(eq(learner), eq("Europe/Paris"));
    }

    @Test
    void aBlankTimezoneIsRefusedBeforeItReachesTheService() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);

        mvc.perform(put("/api/learning/me/settings").with(user("learner").roles("CUSTOMER"))
                        .contentType("application/json").content("{\"timezone\":\" \"}"))
                .andExpect(status().is4xxClientError());

        verify(timezones, org.mockito.Mockito.never()).save(any(), any());
    }
}
