package com.ieltspath.learning.application.port;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Display names of learners, kept by the User Service. Never returns an email address. */
public interface DisplayNameClient {
    /**
     * The display name of each learner the User Service knows, at most 50 ids at a time. Empty when it cannot answer;
     * the caller shows a placeholder. Never throws.
     */
    Map<UUID, String> names(Collection<UUID> userIds);
}
