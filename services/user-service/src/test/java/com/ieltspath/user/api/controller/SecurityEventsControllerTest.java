package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.exception.GlobalExceptionHandler;
import com.ieltspath.user.application.result.AuthAuditEventResult;
import com.ieltspath.user.application.result.PageResult;
import com.ieltspath.user.application.usecase.GetAuthAuditEventsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
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
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "app.auth.external-jwt-issuer=urn:code-base:auth",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "spring.cloud.config.enabled=false"
})
class SecurityEventsControllerTest {
    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    @Autowired MockMvc mvc;
    @MockBean CurrentUserProvider currentUser;
    @MockBean GetAuthAuditEventsUseCase getEvents;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này."));
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
