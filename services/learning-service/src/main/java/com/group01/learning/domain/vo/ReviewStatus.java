package com.group01.learning.domain.vo;

/** A review blocks the learner while PENDING; DONE after a passed set, SKIPPED when it gives up on the learner. */
public enum ReviewStatus {
    PENDING,
    DONE,
    SKIPPED
}
