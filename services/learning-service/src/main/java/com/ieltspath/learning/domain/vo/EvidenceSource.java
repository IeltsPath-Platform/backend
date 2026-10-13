package com.ieltspath.learning.domain.vo;

/** Where a mastery evidence row came from; the stored value is the {@code kp_evidence.source} column. */
public enum EvidenceSource {
    /** Written while lessons still had exercises and essays; kept so those evidence rows still load. */
    LESSON_EXERCISE("lesson_exercise"),
    REVIEW_SET("review_set"),
    PRACTICE_SET("practice_set"),
    /** See {@link #LESSON_EXERCISE}. */
    LESSON_WRITING("lesson_writing"),
    ASSESSMENT("assessment");

    private final String value;

    EvidenceSource(String value) { this.value = value; }

    public String value() { return value; }
}
