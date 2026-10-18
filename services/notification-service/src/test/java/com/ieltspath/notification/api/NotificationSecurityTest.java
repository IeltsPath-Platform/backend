package com.ieltspath.notification.api;

import com.ieltspath.commonsecurity.currentuser.CurrentUser;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.commonsecurity.role.CanonicalRoles;
import com.ieltspath.notification.application.NotificationPreference;
import com.ieltspath.notification.application.NotificationPreferenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(NotificationController.class)
@TestPropertySource(properties={"app.auth.internal-jwt-issuer=urn:code-base:api-gateway","app.auth.internal-jwt-secret=SGs1cjhXY1Y4OFluY2JHdG9hR2dpcWZtRmtKRWprQk5vYUtTd25xV0h0TT0="})
class NotificationSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider users;
    @MockitoBean JdbcTemplate jdbc;
    @MockitoBean NotificationPreferenceService preferences;

    @Test void unauthenticatedListAndPreferencesAreRejected() throws Exception {
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications/preferences")).andExpect(status().isUnauthorized());
    }

    @Test void preferencesRequireAuthenticationAndUpdateForVerifiedOwner() throws Exception {
        UUID owner=UUID.randomUUID();
        when(users.requireCurrentUser()).thenReturn(new CurrentUser(owner, Set.of(CanonicalRoles.CUSTOMER)));
        when(preferences.list(owner)).thenReturn(List.of(new NotificationPreference("community.post-commented.v1",true)));
        mvc.perform(get("/api/notifications/preferences").with(jwt().jwt(token -> token.subject(owner.toString()).claim("roles",List.of("CUSTOMER")))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].enabled").value(true));
        when(preferences.update(owner,"community.post-commented.v1",false))
                .thenReturn(new NotificationPreference("community.post-commented.v1",false));
        mvc.perform(put("/api/notifications/preferences/community.post-commented.v1")
                        .with(jwt().jwt(token -> token.subject(owner.toString()).claim("roles",List.of("CUSTOMER"))))
                        .contentType("application/json").content("{\"enabled\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
        verify(preferences).update(owner,"community.post-commented.v1",false);
    }

    @Test void unsupportedPreferenceTypeReturnsBadRequest() throws Exception {
        UUID owner=UUID.randomUUID();
        when(users.requireCurrentUser()).thenReturn(new CurrentUser(owner, Set.of(CanonicalRoles.CUSTOMER)));
        when(preferences.update(owner,"community.unknown.v1",false)).thenThrow(new IllegalArgumentException("Unsupported notification event type"));
        mvc.perform(put("/api/notifications/preferences/community.unknown.v1")
                        .with(jwt().jwt(token -> token.subject(owner.toString()).claim("roles",List.of("CUSTOMER"))))
                        .contentType("application/json").content("{\"enabled\":false}"))
                .andExpect(status().isBadRequest());
        verify(preferences).update(owner,"community.unknown.v1",false);
    }

    @Test void readMutationIsScopedToCurrentOwner() throws Exception {
        UUID owner=UUID.randomUUID(), notification=UUID.randomUUID();
        when(users.requireCurrentUser()).thenReturn(new CurrentUser(owner, Set.of(CanonicalRoles.CUSTOMER)));
        when(jdbc.update(anyString(), any(), eq(notification), eq(owner))).thenReturn(1);
        mvc.perform(put("/api/notifications/{id}/read",notification).with(jwt().jwt(token->token.subject(owner.toString()).claim("roles",List.of("CUSTOMER")))))
                .andExpect(status().isOk());
        verify(jdbc).update(contains("WHERE id=? AND recipient_id=?"), any(), eq(notification), eq(owner));
    }

    @Test void readMutationReturnsNotFoundWhenNotificationMissingOrBelongsToAnother() throws Exception {
        UUID owner=UUID.randomUUID(), notification=UUID.randomUUID();
        when(users.requireCurrentUser()).thenReturn(new CurrentUser(owner, Set.of(CanonicalRoles.CUSTOMER)));
        when(jdbc.update(anyString(), any(), eq(notification), eq(owner))).thenReturn(0);
        mvc.perform(put("/api/notifications/{id}/read",notification).with(jwt().jwt(token->token.subject(owner.toString()).claim("roles",List.of("CUSTOMER")))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test void nonExistentPathReturnsNotFoundWithStandardErrorResponse() throws Exception {
        UUID owner=UUID.randomUUID();
        mvc.perform(post("/api/notifications/unknown-endpoint")
                        .with(jwt().jwt(token -> token.subject(owner.toString()).claim("roles",List.of("CUSTOMER")))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/notifications/unknown-endpoint"));
    }

    @Test void readMutationWithMalformedIdReturnsBadRequest() throws Exception {
        UUID owner = UUID.randomUUID();
        mvc.perform(put("/api/notifications/abc/read")
                        .with(jwt().jwt(token -> token.subject(owner.toString()).claim("roles", List.of("CUSTOMER")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid value for id"));
    }

    @Test void listWithMalformedPageReturnsBadRequest() throws Exception {
        UUID owner = UUID.randomUUID();
        mvc.perform(get("/api/notifications?page=abc")
                        .with(jwt().jwt(token -> token.subject(owner.toString()).claim("roles", List.of("CUSTOMER")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid value for page"));
    }

    @Test void unsupportedMediaTypeReturnsUnsupportedMediaType() throws Exception {
        UUID owner = UUID.randomUUID();
        mvc.perform(put("/api/notifications/preferences/community.post-commented.v1")
                        .with(jwt().jwt(token -> token.subject(owner.toString()).claim("roles", List.of("CUSTOMER"))))
                        .contentType("text/plain")
                        .content("enabled=true"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }

    @Test void unacceptableMediaTypeReturnsNotAcceptable() throws Exception {
        UUID owner = UUID.randomUUID();
        when(users.requireCurrentUser()).thenReturn(new CurrentUser(owner, Set.of(CanonicalRoles.CUSTOMER)));
        when(preferences.list(owner)).thenReturn(List.of());
        mvc.perform(get("/api/notifications/preferences")
                        .with(jwt().jwt(token -> token.subject(owner.toString()).claim("roles", List.of("CUSTOMER"))))
                        .accept("application/xml"))
                .andExpect(status().isNotAcceptable());
    }
}
