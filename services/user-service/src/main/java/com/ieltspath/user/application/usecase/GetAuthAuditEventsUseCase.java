package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.query.PageQuery;
import com.ieltspath.user.application.result.AuthAuditEventResult;
import com.ieltspath.user.application.result.PageResult;
import com.ieltspath.user.domain.aggregate.AuthAuditEvent;
import com.ieltspath.user.domain.repository.AuthAuditEventRepository;
import com.ieltspath.user.domain.vo.OwnedPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAuthAuditEventsUseCase {
    private static final Set<String> EVENT_TYPES = Set.of("PASSWORD_CHANGED", "PASSWORD_RESET", "PASSWORD_SET",
            "GOOGLE_LINKED", "GOOGLE_UNLINKED", "GOOGLE_PASSWORD_CLEARED", "SESSION_REVOKED",
            "SESSIONS_REVOKED_ALL", "REFRESH_REUSE_DETECTED", "REAUTH_SUCCEEDED", "REAUTH_FAILED",
            "ACCOUNT_STATUS_CHANGED", "ROLE_CHANGED");
    private final AuthAuditEventRepository repository;

    @Transactional(readOnly = true)
    public PageResult<AuthAuditEventResult> execute(UUID userId, PageQuery query, String eventType) {
        if (eventType != null && !EVENT_TYPES.contains(eventType)) {
            throw new IllegalArgumentException("Loại sự kiện bảo mật không hợp lệ.");
        }
        OwnedPage<AuthAuditEvent> page = repository.findByUserId(userId, eventType, query.page(), query.size());
        return new PageResult<>(page.items().stream().map(GetAuthAuditEventsUseCase::toResult).toList(),
                query.page(), query.size(), page.total());
    }

    private static AuthAuditEventResult toResult(AuthAuditEvent event) {
        return new AuthAuditEventResult(event.id(), event.userId(), event.eventType(),
                maskIp(event.ipAddress()), event.userAgent(), event.correlationId(), event.detail(), event.createdAt());
    }

    static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) return null;
        if (ip.contains(":")) {
            int separator = ip.lastIndexOf(':');
            return separator < 0 ? "*" : ip.substring(0, separator + 1) + "*";
        }
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) return "*";
        return parts[0] + "." + parts[1] + "." + parts[2] + ".*";
    }
}
