package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.dto.response.AuthAuditEventResponse;
import com.ieltspath.user.api.dto.response.PageResponse;
import com.ieltspath.user.application.query.PageQuery;
import com.ieltspath.user.application.result.AuthAuditEventResult;
import com.ieltspath.user.application.usecase.GetAuthAuditEventsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SecurityEventsController {
    private final GetAuthAuditEventsUseCase getAuthAuditEvents;
    private final CurrentUserProvider currentUser;

    @GetMapping("/me/security-events")
    @PreAuthorize("isAuthenticated()")
    public PageResponse<AuthAuditEventResponse> ownEvents(@RequestParam(name = "page", defaultValue = "0") int page) {
        return PageResponse.from(getAuthAuditEvents.execute(currentUser.requireUserId(), new PageQuery(page, 50), null),
                SecurityEventsController::forOwner);
    }

    /**
     * An event with an {@code actorId} was done by someone else, such as an administrator resetting the password. Its
     * address, browser and actor belong to that person, so the account owner sees only that it happened.
     */
    private static AuthAuditEventResponse forOwner(AuthAuditEventResult event) {
        if (event.detail() == null || !event.detail().containsKey("actorId")) return AuthAuditEventResponse.from(event);
        Map<String, Object> detail = new HashMap<>(event.detail());
        detail.remove("actorId");
        return new AuthAuditEventResponse(event.id(), event.eventType(), null, null, event.correlationId(), detail,
                event.createdAt());
    }

    @GetMapping("/{id}/auth-events")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AuthAuditEventResponse> userEvents(@PathVariable UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "eventType", required = false) String eventType) {
        return PageResponse.from(getAuthAuditEvents.execute(id, new PageQuery(page, size), eventType), AuthAuditEventResponse::from);
    }
}
