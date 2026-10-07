package com.ieltspath.learning.domain.vo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class BandLevelTest {
    @Test
    void acceptsAndNormalizesTheIeltsHalfBandScale() {
        for (String value : new String[]{"0.0", "5.50", "6", "9.0"}) {
            assertThat(new BandLevel(new BigDecimal(value)).value())
                    .isEqualTo(new BigDecimal(value).setScale(1));
        }
    }

    @Test
    void rejectsMissingQuarterBandsAndValuesOutsideTheScale() {
        assertThatThrownBy(() -> new BandLevel(null)).isInstanceOf(IllegalArgumentException.class);
        for (String value : new String[]{"6.25", "9.5", "-0.5"}) {
            assertThatThrownBy(() -> new BandLevel(new BigDecimal(value)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
