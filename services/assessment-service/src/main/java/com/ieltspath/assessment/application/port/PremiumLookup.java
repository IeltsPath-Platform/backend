package com.ieltspath.assessment.application.port;

import java.util.UUID;

/** Reads whether a learner has an active Premium subscription for background quota decisions. */
public interface PremiumLookup {
    boolean isPremium(UUID userId);
}
