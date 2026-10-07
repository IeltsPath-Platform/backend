package com.ieltspath.learning.application.port;

import java.util.UUID;

/** Serializes one learner's writes for the rest of the current transaction. */
public interface LearnerLock {
    void lock(UUID userId);
}
