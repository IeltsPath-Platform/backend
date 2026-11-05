package com.ieltspath.assessment.application.event;

/**
 * Something wrote outbox rows that people are waiting on (an examiner's notice, for one). Heard only after the transaction
 * commits, so the relay can publish them at once instead of at its next tick.
 */
public record OutboxEventsRecorded() {}
