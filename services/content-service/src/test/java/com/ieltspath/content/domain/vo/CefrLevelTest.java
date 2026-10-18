package com.ieltspath.content.domain.vo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CefrLevelTest {
    @Test
    void mapsCourseBandRangesToSuggestedCefrLevels() {
        assertThat(CefrLevel.forBand(new BigDecimal("0.0"))).isEqualTo(CefrLevel.A1);
        assertThat(CefrLevel.forBand(new BigDecimal("2.0"))).isEqualTo(CefrLevel.A1);
        assertThat(CefrLevel.forBand(new BigDecimal("2.5"))).isEqualTo(CefrLevel.A2);
        assertThat(CefrLevel.forBand(new BigDecimal("3.5"))).isEqualTo(CefrLevel.A2);
        assertThat(CefrLevel.forBand(new BigDecimal("4.0"))).isEqualTo(CefrLevel.B1);
        assertThat(CefrLevel.forBand(new BigDecimal("4.5"))).isEqualTo(CefrLevel.B1);
        assertThat(CefrLevel.forBand(new BigDecimal("5.0"))).isEqualTo(CefrLevel.B2);
        assertThat(CefrLevel.forBand(new BigDecimal("6.0"))).isEqualTo(CefrLevel.B2);
        assertThat(CefrLevel.forBand(new BigDecimal("6.5"))).isEqualTo(CefrLevel.C1);
        assertThat(CefrLevel.forBand(new BigDecimal("7.5"))).isEqualTo(CefrLevel.C1);
        assertThat(CefrLevel.forBand(new BigDecimal("8.0"))).isEqualTo(CefrLevel.C2);
        assertThat(CefrLevel.forBand(new BigDecimal("9.0"))).isEqualTo(CefrLevel.C2);
    }

    @Test
    void returnsNoDefaultForMissingOrOutOfRangeBand() {
        assertThat(CefrLevel.forBand(null)).isNull();
        assertThat(CefrLevel.forBand(new BigDecimal("-0.5"))).isNull();
        assertThat(CefrLevel.forBand(new BigDecimal("9.5"))).isNull();
    }
}
