package com.ieltspath.access.domain.vo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscriptionStatusTest {
    @Test void allowedMovesAreTheLifecycleSpecification() {
        for (var from : SubscriptionStatus.values()) for (var to : SubscriptionStatus.values()) {
            boolean expected = from == SubscriptionStatus.ACTIVE
                    && (to == SubscriptionStatus.EXPIRED || to == SubscriptionStatus.CANCELLED);
            assertEquals(expected, from.canMoveTo(to), from + " -> " + to);
        }
    }
}
