package com.ieltspath.access.domain.aggregate;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyProductTest {

    @Test
    void aPointsProductNeedsAtLeastOnePoint() {
        assertThatThrownBy(() -> KeyProduct.createPointsProduct("P0", "Zero", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Points amount must be greater than 0");
    }

    @Test
    void aPremiumProductNeedsDaysAndCredits() {
        assertThatThrownBy(() -> KeyProduct.createPremiumProduct("PR0", "No days", UUID.randomUUID(), 0, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Premium days must be greater than 0");
        assertThatThrownBy(() -> KeyProduct.createPremiumProduct("PR1", "No credits", UUID.randomUUID(), 30, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Human grading credits must be greater than 0");
    }
}
