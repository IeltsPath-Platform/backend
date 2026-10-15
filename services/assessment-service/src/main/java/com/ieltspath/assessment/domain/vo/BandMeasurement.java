package com.ieltspath.assessment.domain.vo;

/** Immutable evidence captured with a completed result; it never depends on current scoring settings. */
public record BandMeasurement(String skill, Double band, String gradingSource, String measurementQuality,
                              String scoringVersion, String eligibilityReason) {
    public boolean eligible() {
        return "MEASURED".equals(measurementQuality) && eligibilityReason == null
                && band != null && Double.isFinite(band) && band >= 0 && band <= 9;
    }
}
