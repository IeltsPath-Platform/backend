package com.ieltspath.library.domain.study;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A learner's hearts. One heart comes back every {@link #REGEN_INTERVAL} while below the maximum, and the clock stops
 * while the learner is full, so a refill is never wasted on time already passed.
 */
public final class StudyHearts {
    public static final Duration REGEN_INTERVAL = Duration.ofHours(5);

    private final UUID userId;
    private int hearts;
    private int max;
    private Instant lastRegenAt;
    private final Duration interval;

    private StudyHearts(UUID userId, int hearts, int max, Instant lastRegenAt, Duration interval) {
        if (max < 1) throw new IllegalArgumentException("A learner has at least one heart at most");
        if (hearts < 0 || hearts > max) throw new IllegalArgumentException("StudyHearts must be between 0 and the maximum");
        if (interval == null || interval.isNegative() || interval.isZero()) throw new IllegalArgumentException("Regeneration interval must be positive");
        this.interval = interval;
        this.userId = Objects.requireNonNull(userId);
        this.hearts = hearts;
        this.max = max;
        this.lastRegenAt = Objects.requireNonNull(lastRegenAt);
    }

    /** A new learner starts full. */
    public static StudyHearts start(UUID userId, int max, Instant now, Duration interval) {
        return new StudyHearts(userId, max, max, now, interval);
    }

    public static StudyHearts restore(UUID userId, int hearts, int max, Instant lastRegenAt, Duration interval) {
        return new StudyHearts(userId, hearts, max, lastRegenAt, interval);
    }

    /** Adds the hearts whose interval has passed, up to the maximum. Full hearts keep the clock at {@code now}. */
    public void regen(Instant now) {
        if (hearts >= max) {
            lastRegenAt = now;
            return;
        }
        if (!now.isAfter(lastRegenAt)) return;
        long intervals = Duration.between(lastRegenAt, now).dividedBy(interval);
        if (intervals == 0) return;
        hearts = (int) Math.min(max, hearts + intervals);
        lastRegenAt = hearts >= max ? now : lastRegenAt.plus(interval.multipliedBy(intervals));
    }

    /** Takes {@code count} hearts, never below zero. A learner who was full starts the regeneration clock now. */
    public void lose(int count, Instant now) {
        if (count < 0) throw new IllegalArgumentException("StudyHearts lost cannot be negative");
        if (hearts >= max) lastRegenAt = now;
        hearts = Math.max(0, hearts - count);
    }

    /** Fills the hearts (a refill or a level-up). */
    public void refillFull(Instant now) {
        hearts = max;
        lastRegenAt = now;
    }

    /** Changes the maximum, for a change of plan. A lower maximum cuts the hearts to it. */
    public void setMax(int newMax, Instant now) {
        if (newMax < 1) throw new IllegalArgumentException("A learner has at least one heart at most");
        boolean wasFull = hearts >= max;
        max = newMax;
        hearts = Math.min(hearts, max);
        if (wasFull || hearts >= max) lastRegenAt = now;
    }

    public boolean full() {
        return hearts >= max;
    }

    /** When the next heart comes back, or empty when the learner is full. */
    public Optional<Instant> nextHeartAt() {
        return full() ? Optional.empty() : Optional.of(lastRegenAt.plus(interval));
    }

    public UUID userId() { return userId; }
    public int hearts() { return hearts; }
    public int max() { return max; }
    public Instant lastRegenAt() { return lastRegenAt; }
}
