package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.exception.GlobalExceptionHandler;
import com.ieltspath.user.application.result.ReauthResult;
import com.ieltspath.user.application.usecase.ReauthUseCase;
import com.ieltspath.user.application.usecase.SetInitialPasswordUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ReauthController.class, InitialPasswordController.class})
@Import(GlobalExceptionHandler.class)
class ReauthControllerTest {
    @Autowired MockMvc mvc;
    @MockBean CurrentUserProvider currentUser;
    @MockBean ReauthUseCase reauth;
    @MockBean SetInitialPasswordUseCase setInitialPassword;

    @Test
    @WithMockUser
    void reauthenticatesTheCurrentUserAndReturnsTheShortLivedMethodBoundToken() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        when(currentUser.currentSessionId()).thenReturn(Optional.of(sessionId));
        when(reauth.execute(userId, sessionId, "current-password", null)).thenReturn(new ReauthResult("reauth-token", "PASSWORD"));

        mvc.perform(post("/api/users/me/reauth").with(csrf())
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"current-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reauthToken").value("reauth-token"))
                .andExpect(jsonPath("$.expiresInSeconds").value(300))
                .andExpect(jsonPath("$.method").value("PASSWORD"));
    }

    @Test
    @WithMockUser
    void initialPasswordRequiresAndForwardsTheReauthenticationHeader() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        when(currentUser.currentSessionId()).thenReturn(Optional.of(sessionId));

        mvc.perform(post("/api/users/me/password/initial").with(csrf())
                        .header("X-Reauth-Token", "one-time-token")
                        .contentType("application/json")
                        .content("{\"newPassword\":\"new-password-123\"}"))
                .andExpect(status().isOk());

        verify(setInitialPassword).execute(userId, sessionId, "one-time-token", "new-password-123");
    }
}
