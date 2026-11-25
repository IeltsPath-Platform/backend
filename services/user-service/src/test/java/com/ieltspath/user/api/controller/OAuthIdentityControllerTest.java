package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.exception.GlobalExceptionHandler;
import com.ieltspath.user.application.usecase.GetOAuthIdentitiesUseCase;
import com.ieltspath.user.application.usecase.UnlinkOAuthIdentityUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OAuthIdentityController.class)
@Import(GlobalExceptionHandler.class)
class OAuthIdentityControllerTest {
    @Autowired MockMvc mvc;
    @MockBean CurrentUserProvider currentUser;
    @MockBean GetOAuthIdentitiesUseCase getIdentities;
    @MockBean UnlinkOAuthIdentityUseCase unlink;

    @Test
    @WithMockUser
    void forwardsTheReauthenticationHeaderAndCurrentSessionToTheUnlinkUseCase() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        when(currentUser.currentSessionId()).thenReturn(java.util.Optional.of(sessionId));

        mvc.perform(delete("/api/users/me/oauth/google").with(csrf()).header("X-Reauth-Token", "one-time-token"))
                .andExpect(status().isOk());

        verify(unlink).execute(userId, "google", "one-time-token", sessionId);
    }
}
