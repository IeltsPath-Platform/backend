package com.ieltspath.content.domain.vo;

/** Groups error types for reporting. Matches the {@code error_types.category} check constraint. */
public enum ErrorCategory {
    GRAMMAR,
    VOCABULARY,
    COHERENCE,
    TASK,
    READING_STRATEGY,
    LISTENING_STRATEGY,
    SPELLING_NUMBER
}
