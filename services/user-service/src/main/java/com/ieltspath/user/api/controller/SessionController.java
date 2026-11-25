package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.dto.response.SessionResponse;
import com.ieltspath.user.application.usecase.ListMySessionsUseCase;
import com.ieltspath.user.application.usecase.RevokeAllMySessionsUseCase;
import com.ieltspath.user.application.usecase.RevokeMySessionUseCase;
import com.ieltspath.user.domain.aggregate.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final CurrentUserProvider currentUserProvider;
    private final ListMySessionsUseCase listSessions;
    private final RevokeMySessionUseCase revokeSession;
    private final RevokeAllMySessionsUseCase revokeAllSessions;

    @GetMapping
    public List<SessionResponse> list() {
        UUID currentSessionId = currentUserProvider.currentSessionId().orElse(null);
        return listSessions.execute(currentUserProvider.requireUserId()).stream()
                .map(token -> toResponse(token, currentSessionId)).toList();
    }

    @DeleteMapping("/{sessionId}")
    public void revoke(@PathVariable String sessionId) {
        UUID parsedId;
        try {
            parsedId = UUID.fromString(sessionId);
        } catch (IllegalArgumentException malformedId) {
            throw new com.ieltspath.user.domain.exception.SessionNotFoundException();
        }
        revokeSession.execute(currentUserProvider.requireUserId(), parsedId);
    }

    @DeleteMapping
    public void revokeAll(@RequestParam(defaultValue = "false") boolean exceptCurrent) {
        revokeAllSessions.execute(currentUserProvider.requireUserId(),
                currentUserProvider.currentSessionId().orElse(null), exceptCurrent);
    }

    private SessionResponse toResponse(RefreshToken token, UUID currentSessionId) {
        return new SessionResponse(token.getFamilyId(), token.getSessionStartedAt(), token.getLastUsedAt(),
                maskAddress(token.getIpAddress()), token.getUserAgent(), token.getFamilyId() != null
                && token.getFamilyId().equals(currentSessionId));
    }

    private String maskAddress(String address) {
        if (address == null || address.isBlank()) return null;
        int lastDot = address.lastIndexOf('.');
        if (lastDot >= 0) return address.substring(0, lastDot + 1) + "x";
        int lastColon = address.lastIndexOf(':');
        return lastColon >= 0 ? address.substring(0, lastColon + 1) + "x" : "x";
    }
}
