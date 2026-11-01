package com.ieltspath.learning.application.port;

import java.util.UUID;

public interface StudySourceVerifier {
    record Context(UUID sourceId, UUID courseId) {}
    Context verify(UUID userId, String sourceType, UUID sourceId);
}
