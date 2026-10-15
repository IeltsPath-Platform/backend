package com.ieltspath.library.domain.aggregate;

import com.ieltspath.library.domain.exception.ConflictException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One learner's recall state for a canonical sense, independent of flashcard decks. */
public record VocabularyProgress(UUID userId, UUID vocabularySenseId, State state, long stateVersion,
        boolean active, Instant nextReviewAt, Instant lastReviewedAt, Instant createdAt) {
    public enum State { NEW, L1, L2, L3, RETAINED }
    public enum Outcome { REMEMBERED, FORGOT }
    public VocabularyProgress {
        Objects.requireNonNull(userId); Objects.requireNonNull(vocabularySenseId); Objects.requireNonNull(state);
        Objects.requireNonNull(nextReviewAt); Objects.requireNonNull(createdAt);
        if (stateVersion < 0) throw new IllegalArgumentException("Invalid state version");
    }
    public static VocabularyProgress start(UUID user, UUID sense, Instant now) {
        return new VocabularyProgress(user, sense, State.NEW, 0, true, now, null, now);
    }
    public VocabularyProgress activate(boolean value) {
        return active == value ? this : new VocabularyProgress(userId, vocabularySenseId, state,
                stateVersion + 1, value, nextReviewAt, lastReviewedAt, createdAt);
    }
    public VocabularyProgress answer(long expectedVersion, Outcome outcome, Instant now) {
        Objects.requireNonNull(outcome); Objects.requireNonNull(now);
        if (!active || expectedVersion != stateVersion || now.isBefore(nextReviewAt)) throw new ConflictException();
        State next = outcome == Outcome.FORGOT ? State.L1 : switch(state) {
            case NEW -> State.L1; case L1 -> State.L2; case L2 -> State.L3; case L3, RETAINED -> State.RETAINED;
        };
        Duration delay = outcome == Outcome.FORGOT ? Duration.ofMinutes(10) : Duration.ofDays(switch(next) {
            case L1 -> 1; case L2 -> 3; case L3 -> 7; case RETAINED -> 14;
            default -> throw new IllegalStateException("Invalid recall transition");
        });
        return new VocabularyProgress(userId, vocabularySenseId, next, stateVersion + 1, true,
                now.plus(delay), now, createdAt);
    }
}
