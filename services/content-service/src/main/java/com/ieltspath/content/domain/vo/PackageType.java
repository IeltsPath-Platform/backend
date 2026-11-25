package com.ieltspath.content.domain.vo;

public enum PackageType {
    MOCK_TEST,
    PLACEMENT_TEST,
    PRACTICE_SET,
    PRACTICE_TEST,
    QUIZ,
    LESSON,
    /** A topic's final test; created only by seed, always attached to a topic. */
    TOPIC_TEST,
    /** A course's final test; created only by seed, always attached to a course. */
    COURSE_TEST;

    public String codeAbbreviation() {
        return switch (this) {
            case PRACTICE_SET -> "PS";
            case TOPIC_TEST -> "TT";
            case COURSE_TEST -> "CT";
            case PRACTICE_TEST -> "PT";
            case MOCK_TEST -> "MT";
            case PLACEMENT_TEST -> "PL";
            case QUIZ -> "QZ";
            case LESSON -> "LS";
        };
    }
}

