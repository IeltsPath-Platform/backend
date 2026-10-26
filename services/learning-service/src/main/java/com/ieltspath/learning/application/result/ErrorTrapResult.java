package com.ieltspath.learning.application.result;

/** Learner-facing label and correction hint for the current main error on a parent knowledge point. */
public record ErrorTrapResult(String code, String name, String fixHint) {}
