package com.ieltspath.learning.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** The badge catalog, what each learner has earned, and the counts some badges are judged by. */
public interface BadgeRepository {
    record Badge(String code, String name, String description, int rewardPoints, boolean active) {}

    List<Badge> catalog();

    Optional<Badge> find(String code);

    /** Switches a badge on or off; false when there is no such badge. */
    boolean setActive(String code, boolean active);

    /** The badges the learner holds and when each was awarded. */
    Map<String, Instant> awarded(UUID userId);

    /**
     * Awards the active badges among {@code codes} that the learner does not hold yet and returns them. A badge already
     * held, or switched off, is skipped, so repeating a check awards nothing twice.
     */
    List<Badge> award(UUID userId, Collection<String> codes, Instant now);

    /** Knowledge points the learner holds at the maintain tier now. */
    int maintainedPoints(UUID userId);

    /** Daily quests the learner has completed. */
    int completedQuests(UUID userId);
}
