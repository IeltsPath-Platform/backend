package com.ieltspath.user.application.port;

import com.ieltspath.user.domain.aggregate.AuthAuditEvent;

public interface AuthAuditRecorder {
    void record(AuthAuditEvent event);
}
