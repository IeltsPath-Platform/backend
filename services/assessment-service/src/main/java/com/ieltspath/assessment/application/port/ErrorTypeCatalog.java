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

    /**
     * The codes an examiner may record: every {@code ACTIVE} code, including those whose review is not finished.
     * {@link #activeCodes()} stays the set a learner may be shown. Failure to read is thrown like {@link #activeCodes()}.
     */
    default Set<String> staffCodes() {
        return activeCodes();
    }

    default List<ErrorType> activeErrorTypes() {
        return activeCodes().stream().sorted().map(code -> new ErrorType(code, code, "")).toList();
    }

    /** The approved codes that apply to {@code skill}; the essay grader asks for WRITING. */
    default List<ErrorType> activeErrorTypes(String skill) {
        return activeErrorTypes();
    }

    /**
     * {@code modelDefinition} is the English definition the model reads, null when Content has none.
     * {@code writingTasks} lists TASK_1 or TASK_2; an empty list means both tasks.
     */
    record ErrorType(String code, String name, String fixHint, String modelDefinition, List<String> writingTasks) {
        public ErrorType {
            writingTasks = writingTasks == null ? List.of() : List.copyOf(writingTasks);
        }

        public ErrorType(String code, String name, String fixHint) {
            this(code, name, fixHint, null, List.of());
        }

        /** Whether a Writing task (TASK_1 or TASK_2) may use this code. */
        public boolean appliesToWritingTask(String task) {
            return writingTasks.isEmpty() || writingTasks.contains(task);
        }
    }
}
