package com.ieltspath.library.domain.vo;

public enum CefrLevel {
    A1(1.0, 2.5), A2(3.0, 3.5), B1(4.0, 5.0),
    B2(5.5, 6.5), C1(7.0, 8.0), C2(8.5, 9.0);

    private final double minBand;
    private final double maxBand;

    CefrLevel(double minBand, double maxBand) {
        this.minBand = minBand;
        this.maxBand = maxBand;
    }

    public IeltsBandRange bandRange() {
        return new IeltsBandRange(minBand, maxBand);
    }
}
