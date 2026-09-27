package com.group01.content.domain.vo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BandRangeTest {

    private static BigDecimal band(String value) {
        return new BigDecimal(value);
    }

    @Test
    void acceptsHalfBandStepsWithinTheIeltsScale() {
        BandRange range = BandRange.of(band("4.0"), band("6.5"));

        assertThat(range.min()).isEqualByComparingTo("4.0");
        assertThat(range.max()).isEqualByComparingTo("6.5");
        assertThat(range.isUnbounded()).isFalse();
    }

    @Test
    void eitherEndMayBeOpen() {
        assertThat(BandRange.of(null, band("5.0")).min()).isNull();
        assertThat(BandRange.of(band("7.0"), null).max()).isNull();
        assertThat(BandRange.of(null, null)).isEqualTo(BandRange.UNBOUNDED);
    }

    @Test
    void rejectsBandsOutsideTheScaleOrOffTheHalfStep() {
        assertThatThrownBy(() -> BandRange.of(band("9.5"), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BandRange.of(band("-0.5"), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BandRange.of(band("4.3"), null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAMinimumAboveTheMaximum() {
        assertThatThrownBy(() -> BandRange.of(band("7.0"), band("5.0"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anOwnRangeReplacesTheInheritedOneAsAWhole() {
        BandRange topic = BandRange.of(band("4.0"), band("5.0"));

        assertThat(BandRange.UNBOUNDED.orInherit(topic)).isEqualTo(topic);
        // Overriding one end must not combine with the other end of the topic: 8.0-5.0 would be invalid.
        assertThat(BandRange.of(band("8.0"), null).orInherit(topic)).isEqualTo(BandRange.of(band("8.0"), null));
        assertThat(BandRange.UNBOUNDED.orInherit(BandRange.UNBOUNDED)).isEqualTo(BandRange.UNBOUNDED);
    }
}
