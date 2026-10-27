package com.ieltspath.assessment.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** The examiner a learner chooses for a human grading job, or picks instead of one that is late. */
public record ChooseExaminerRequest(@NotNull UUID examinerId) {}
