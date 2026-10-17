package com.ieltspath.assessment.application.port;

import java.util.Optional;
import java.util.UUID;

public interface HumanGradingRequestStore {
    Optional<UUID> submittedTopicEssay(UUID userId, UUID attemptId, UUID itemId);
}
