package com.ieltspath.content.domain.vo;

/** One version of a rubric, whatever its status. Authors need every version to pick the next number. */
public record RubricVersionRef(String code, String task, int version, String status) {
}
