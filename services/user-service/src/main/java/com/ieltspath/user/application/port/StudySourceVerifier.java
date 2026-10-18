package com.ieltspath.user.application.port;

import java.util.UUID;

public interface StudySourceVerifier {
    record Context(UUID sourceId, UUID courseId) {}
    Context verify(String sourceType, UUID sourceId);
}
