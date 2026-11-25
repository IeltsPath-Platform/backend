package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.exception.GlobalExceptionHandler;
import com.ieltspath.user.application.usecase.ListMySessionsUseCase;
import com.ieltspath.user.application.usecase.RevokeAllMySessionsUseCase;
import com.ieltspath.user.application.usecase.RevokeMySessionUseCase;
import com.ieltspath.user.domain.aggregate.RefreshToken;
import com.ieltspath.user.domain.exception.SessionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
@Import(GlobalExceptionHandler.class)
class SessionControllerTest {
    @Autowired MockMvc mvc;
    @MockBean CurrentUserProvider currentUser;
    @MockBean ListMySessionsUseCase listSessions;
    @MockBean RevokeMySessionUseCase revokeSession;
    @MockBean RevokeAllMySessionsUseCase revokeAllSessions;

    @Test
    void requiresAuthenticationToListSessions() throws Exception {
        mvc.perform(get("/api/users/me/sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void listsSessionsWithTheFinalAddressOctetMasked() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        when(currentUser.currentSessionId()).thenReturn(Optional.of(familyId));
        when(listSessions.execute(userId)).thenReturn(List.of(RefreshToken.builder().familyId(familyId)
                .sessionStartedAt(LocalDateTime.of(2026, 11, 25, 10, 0)).lastUsedAt(LocalDateTime.of(2026, 11, 25, 10, 5))
                .ipAddress("203.0.113.45").userAgent("Browser").build()));

        mvc.perform(get("/api/users/me/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value(familyId.toString()))
                .andExpect(jsonPath("$[0].ipAddress").value("203.0.113.x"))
                .andExpect(jsonPath("$[0].current").value(true));
    }

    @Test
    @WithMockUser
    void doesNotRevealWhetherAnotherUsersSessionExists() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        doThrow(new SessionNotFoundException()).when(revokeSession).execute(userId, sessionId);

        mvc.perform(delete("/api/users/me/sessions/{sessionId}", sessionId).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy phiên đăng nhập."));
    }

    @Test
    @WithMockUser
    void treatsMalformedSessionIdsAsNotFound() throws Exception {
        when(currentUser.requireUserId()).thenReturn(UUID.randomUUID());

        mvc.perform(delete("/api/users/me/sessions/abc").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy phiên đăng nhập."));
        verifyNoInteractions(revokeSession);
    }

    @Test
    @WithMockUser
    void returnsNotFoundWhenSessionDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        doThrow(new SessionNotFoundException()).when(revokeSession).execute(userId, sessionId);

        mvc.perform(delete("/api/users/me/sessions/{sessionId}", sessionId).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy phiên đăng nhập."));
    }
}
