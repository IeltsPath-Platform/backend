package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.port.AuthAuditRecorder;
import com.ieltspath.user.domain.aggregate.AuthAuditEvent;
import com.ieltspath.user.domain.repository.AuthAuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JpaAuthAuditRecorder implements AuthAuditRecorder {
    private final AuthAuditEventRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuthAuditEvent event) {
        try {
            repository.save(event);
        } catch (RuntimeException failure) {
            log.warn("Không ghi được sự kiện kiểm toán xác thực eventType={} errorType={}",
                    event.eventType(), failure.getClass().getSimpleName());
        }
    }
}
