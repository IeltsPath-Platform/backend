package com.ieltspath.user.application.usecase;

import com.ieltspath.commonobservability.Correlation;
import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.application.port.AuthAuditRecorder;
import com.ieltspath.user.application.port.AuthAuditContextProvider;
import com.ieltspath.user.domain.aggregate.AuthAuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthAuditSupport {
    private static final Set<String> SAFE_DETAIL_KEYS = Set.of("sessionId", "reason", "method", "code", "provider");
    private final AuthAuditRecorder recorder;
    private final AuthAuditContextProvider contextProvider;

    public void record(String eventType, UUID userId, LoginContext context, Map<String, ?> detail) {
        try {
            Map<String, Object> safeDetail = detail == null ? Map.of() : detail.entrySet().stream()
                    .filter(entry -> SAFE_DETAIL_KEYS.contains(entry.getKey()))
                    .filter(entry -> entry.getValue() instanceof UUID || entry.getValue() instanceof Enum<?>
                            || entry.getValue() instanceof String value && value.matches("[A-Z0-9_-]{1,64}"))
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,
                            entry -> entry.getValue() instanceof Enum<?> value ? value.name() : entry.getValue()));
            LoginContext source = context == null ? contextProvider.current() : context;
            recorder.record(new AuthAuditEvent(UUID.randomUUID(), userId, eventType, source.ipAddress(), source.userAgent(),
                    Correlation.current(), safeDetail, LocalDateTime.now()));
        } catch (RuntimeException failure) {
            log.warn("Không ghi được sự kiện kiểm toán xác thực eventType={} errorType={}",
                    eventType, failure.getClass().getSimpleName());
        }
    }
}
