package com.ieltspath.community.application.port;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The current streak of learners, owned by the Learning Service. */
public interface StreakLookup {
    /**
     * The streak of each listed learner who has one; the others are absent. Throws
     * {@link com.ieltspath.community.domain.exception.CommunityUnavailableException} when Learning cannot answer.
     */
    Map<UUID, Integer> streaks(Set<UUID> userIds);
}
