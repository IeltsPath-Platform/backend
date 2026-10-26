package com.ieltspath.assessment.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/** The rules of choosing an examiner: when a job counts as late and how often the examiner may be replaced. */
public final class ExaminerAssignmentPolicy {
    /** A job is late once this long has passed since the examiner was chosen. */
    public static final Duration DEADLINE = Duration.ofHours(48);
    /** The examiner is reminded once a job has waited this long without being graded: 12 hours before it is late. */
    public static final Duration REMINDER_AFTER = Duration.ofHours(36);
    /** How many times the learner may replace the examiner of one submission. */
    public static final int MAX_CHANGES = 2;

    private static final Set<String> OPEN = Set.of("QUEUED", "PROCESSING");

    public enum State { AWAITING_CHOICE, WAITING, OVERDUE, COMPLETED, CLOSED }

    private ExaminerAssignmentPolicy() {}

    public static boolean isOpen(String status) {
        return OPEN.contains(status);
    }

    public static boolean isOverdue(Instant requestedAt, Instant now) {
        return requestedAt != null && !requestedAt.plus(DEADLINE).isAfter(now);
    }

    public static State stateOf(String status, boolean examinerChosen, Instant requestedAt, Instant now) {
        if ("COMPLETED".equals(status)) return State.COMPLETED;
        if (!isOpen(status)) return State.CLOSED;
        if (!examinerChosen) return State.AWAITING_CHOICE;
        return isOverdue(requestedAt, now) ? State.OVERDUE : State.WAITING;
    }

    /** The learner may choose when nobody was chosen yet, or replace the examiner once the job is late. */
    public static boolean canChoose(State state, int changesMade) {
        return state == State.AWAITING_CHOICE || (state == State.OVERDUE && changesMade < MAX_CHANGES);
    }
}
