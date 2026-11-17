package com.ieltspath.content.domain.vo;

public enum PlacementPool {
    BAND_0_3(3.0), BAND_3_5(5.0), BAND_5_9(9.0);

    private final double ceiling;
    PlacementPool(double ceiling) { this.ceiling = ceiling; }
    public double ceiling() { return ceiling; }
}
