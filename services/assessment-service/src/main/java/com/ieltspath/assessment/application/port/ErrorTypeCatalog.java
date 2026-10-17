package com.ieltspath.assessment.application.port;

import java.util.Set;
import java.util.List;

/**
 * The active error-type codes Content Service maintains. A grader may only record a code from this set, so every
 * error analysis row names a code that R&amp;D can explain.
 */
public interface ErrorTypeCatalog {
    /**
     * @return the codes currently {@code ACTIVE}; an empty set when Content has none. Any failure to read them is
     * thrown as {@link com.ieltspath.assessment.application.exception.ContentUnavailableException}.
     */
    Set<String> activeCodes();

    default List<ErrorType> activeErrorTypes() {
        return activeCodes().stream().sorted().map(code -> new ErrorType(code, code, "")).toList();
    }

    record ErrorType(String code, String name, String fixHint) {}
}
