package com.ieltspath.content.domain.vo;

import java.math.BigDecimal;

/** Approximate IELTS band interval for a CEFR level, used only to suggest a default label. */
public enum CefrLevel {
    A1("2.0"),
    A2("3.5"),
    B1("4.5"),
    B2("6.0"),
    C1("7.5"),
    C2("9.0");

    private final BigDecimal upperBand;

    CefrLevel(String upperBand) {
        this.upperBand = new BigDecimal(upperBand);
    }

    public static CefrLevel forBand(BigDecimal band) {
        if (band == null || band.compareTo(BigDecimal.ZERO) < 0 || band.compareTo(new BigDecimal("9.0")) > 0) {
            return null;
        }
        for (CefrLevel level : values()) {
            if (band.compareTo(level.upperBand) <= 0) {
                return level;
            }
        }
        return null;
    }
}
