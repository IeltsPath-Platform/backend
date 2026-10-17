package com.ieltspath.learning.domain.vo;

/** Where a mastery evidence row came from; the stored value is the {@code kp_evidence.source} column. */
public enum EvidenceSource {
    LESSON_EXERCISE("lesson_exercise"),
    REVIEW_SET("review_set"),
    PRACTICE_SET("practice_set"),
    LESSON_WRITING("lesson_writing"),
    ASSESSMENT("assessment"),
    DIAGNOSTIC_PROBE("diagnostic_probe");

    private final String value;

    EvidenceSource(String value) { this.value = value; }

    public String value() { return value; }

    public static EvidenceSource fromValue(String stored) {
        for (EvidenceSource source : values()) {
            if (source.value.equals(stored)) return source;
        }
        throw new IllegalArgumentException("Unknown evidence source: " + stored);
    }
}
