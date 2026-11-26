package com.ieltspath.learning.domain.exception;

import java.time.Instant;

/** The learner has no hearts, so a heart-costing submission or start is refused until one comes back or is bought. */
public class HeartsDepletedException extends RuntimeException {
    private final Instant nextHeartAt;
    private final int refillCost;

    public HeartsDepletedException(Instant nextHeartAt, int refillCost) {
        super("Bạn đã hết tim. Vui lòng chờ hồi tim hoặc dùng điểm để nạp thêm.");
        this.nextHeartAt = nextHeartAt;
        this.refillCost = refillCost;
    }

    public Instant getNextHeartAt() {
        return nextHeartAt;
    }

    public int getRefillCost() {
        return refillCost;
    }
}
