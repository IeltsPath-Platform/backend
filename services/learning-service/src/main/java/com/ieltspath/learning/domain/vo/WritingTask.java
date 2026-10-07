package com.ieltspath.learning.domain.vo;

import java.util.List;
import java.util.Optional;

/** IELTS Writing tasks graded in lessons, each with its own four criteria in response order. */
public enum WritingTask {
    /** Academic Task 1: Task Achievement is judged against the question's chart facts. */
    TASK_1(List.of("TA", "CC", "LR", "GRA")),
    TASK_2(List.of("TR", "CC", "LR", "GRA"));

    private final List<String> criteria;

    WritingTask(List<String> criteria) {
        this.criteria = criteria;
    }

    public List<String> criteria() {
        return criteria;
    }

    public static Optional<WritingTask> parse(String value) {
        for (WritingTask task : values()) {
            if (task.name().equals(value)) return Optional.of(task);
        }
        return Optional.empty();
    }
}
