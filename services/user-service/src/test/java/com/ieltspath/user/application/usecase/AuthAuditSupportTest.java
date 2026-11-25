package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.application.port.AuthAuditRecorder;
import com.ieltspath.user.application.port.AuthAuditContextProvider;
import com.ieltspath.user.domain.aggregate.AuthAuditEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthAuditSupportTest {
    private final AuthAuditRecorder recorder = mock(AuthAuditRecorder.class);
    private final AuthAuditContextProvider contextProvider = () -> LoginContext.UNKNOWN;
    private final AuthAuditSupport support = new AuthAuditSupport(recorder, contextProvider);

    @Test
    void recordsOnlyAllowlistedIdentifiersAndCodes() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        support.record("SESSION_REVOKED", userId, new LoginContext("192.0.2.17", "Browser"),
                Map.of("sessionId", sessionId, "reason", "USER_REQUEST", "token", "must-not-be-stored", "password", "secret"));

        ArgumentCaptor<AuthAuditEvent> event = ArgumentCaptor.forClass(AuthAuditEvent.class);
        verify(recorder).record(event.capture());
        assertThat(event.getValue().userId()).isEqualTo(userId);
        assertThat(event.getValue().ipAddress()).isEqualTo("192.0.2.17");
        assertThat(event.getValue().detail()).containsEntry("sessionId", sessionId).containsEntry("reason", "USER_REQUEST")
                .doesNotContainKeys("token", "password");
    }

    @Test
    void auditStorageFailureDoesNotFailTheAccountAction() {
        doThrow(new IllegalStateException("database unavailable")).when(recorder).record(any());
        support.record("PASSWORD_CHANGED", UUID.randomUUID(), null, Map.of());
    }
}
