package com.ieltspath.access.domain.vo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyStatusTest {
    @Test void allowedMovesAreTheLifecycleSpecification() {
        for (var from : KeyStatus.values()) for (var to : KeyStatus.values()) {
            boolean expected = from == KeyStatus.ACTIVE
                    && (to == KeyStatus.REDEEMED || to == KeyStatus.REVOKED || to == KeyStatus.EXPIRED);
            assertEquals(expected, from.canMoveTo(to), from + " -> " + to);
        }
    }
}
