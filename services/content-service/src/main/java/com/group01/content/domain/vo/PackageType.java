package com.group01.content.domain.vo;

public enum PackageType {
    MOCK_TEST,
    PLACEMENT_TEST,
    PRACTICE_SET,
    QUIZ,
    LESSON,
    /** A topic's final test; created only by seed, always attached to a topic. */
    TOPIC_TEST
}

