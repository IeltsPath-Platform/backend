package com.ieltspath.learning.domain.vo;

/** Why a review sends the learner back to the theory. */
public enum TheoryReason {
    SECOND_FAIL, LOW_SCORE,
    /** Only on reviews created while lessons still had exercises; kept so those rows still load. */
    WRONG_IN_LESSON
}
