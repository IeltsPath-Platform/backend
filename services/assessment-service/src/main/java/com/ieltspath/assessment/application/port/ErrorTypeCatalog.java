package com.ieltspath.assessment.application.port;

import java.util.Set;

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
}
