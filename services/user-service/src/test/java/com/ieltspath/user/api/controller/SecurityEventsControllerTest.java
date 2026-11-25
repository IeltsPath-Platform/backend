package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.exception.GlobalExceptionHandler;
import com.ieltspath.user.application.result.AuthAuditEventResult;
import com.ieltspath.user.application.result.PageResult;
import com.ieltspath.user.application.usecase.GetAuthAuditEventsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SecurityEventsController.class)
@Import(GlobalExceptionHandler.class)
class SecurityEventsControllerTest {
    @Autowired MockMvc mvc;
    @MockBean CurrentUserProvider currentUser;
    @MockBean GetAuthAuditEventsUseCase getEvents;

    @Test
    @WithMockUser
    void returnsOnlyTheAuthenticatedUsersSecurityEvents() throws Exception {
        UUID userId = UUID.randomUUID();
        when(currentUser.requireUserId()).thenReturn(userId);
        when(getEvents.execute(eq(userId), any(), isNull())).thenReturn(new PageResult<>(List.of(
                new AuthAuditEventResult(UUID.randomUUID(), userId, "SESSION_REVOKED", "203.0.113.*", "Browser",
                        "request-1", Map.of(), LocalDateTime.now())), 0, 50, 1));

        mvc.perform(get("/api/users/me/security-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].eventType").value("SESSION_REVOKED"))
                .andExpect(jsonPath("$.items[0].ipAddress").value("203.0.113.*"));
        verify(getEvents).execute(eq(userId), any(), isNull());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deniesSecurityEventsForAnotherUserToNonAdministrators() throws Exception {
        mvc.perform(get("/api/users/{id}/auth-events", UUID.randomUUID()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(getEvents);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorsCanFilterAnotherUsersEvents() throws Exception {
        UUID userId = UUID.randomUUID();
        when(getEvents.execute(eq(userId), any(), eq("PASSWORD_CHANGED")))
                .thenReturn(new PageResult<>(List.of(), 1, 40, 0));
        mvc.perform(get("/api/users/{id}/auth-events", userId).param("page", "1").param("size", "40")
                        .param("eventType", "PASSWORD_CHANGED"))
                .andExpect(status().isOk());
        verify(getEvents).execute(eq(userId), any(), eq("PASSWORD_CHANGED"));
    }
}
