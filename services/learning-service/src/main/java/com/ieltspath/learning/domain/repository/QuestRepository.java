package com.ieltspath.learning.domain.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The daily quests of learners. */
public interface QuestRepository {
    record Quest(UUID id, UUID userId, LocalDate localDate, String type, UUID targetKnowledgePointId, int required,
                 int progress, Instant completedAt, int xpReward, int pointsReward) {
        public boolean completed() {
            return completedAt != null;
        }
    }

    record NewQuest(String type, UUID targetKnowledgePointId, int required, int xpReward, int pointsReward) {}

    List<Quest> find(UUID userId, LocalDate date);

    /** Adds the quests of a day; one already there for the same type is kept. */
    void insertIfAbsent(UUID userId, LocalDate date, List<NewQuest> quests);

    /**
     * Adds progress to an unfinished quest, capped at what it requires. Returns the quest only when this call completed
     * it, so the reward is paid once.
     */
    Optional<Quest> addProgress(UUID userId, LocalDate date, String type, int amount, Instant now);
}
