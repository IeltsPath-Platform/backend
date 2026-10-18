package com.ieltspath.learning.domain.service;

/**
 * Whether a question is too easy or too hard for what learners actually do. The difficulty tag of Content only moves the
 * limits (a question tagged HARD that nearly everyone answers is more suspect than an untagged one); it never changes
 * what a learner is given.
 */
public final class QuestionDifficultyRules {
    public enum Verdict { TOO_EASY, TOO_HARD, OK }

    /** Correct-rate limits: any question; a question tagged HARD (too easy from here); one tagged EASY (too hard below). */
    public record Limits(double tooEasyAbove, double tooHardBelow, double hardTaggedEasyAt, double easyTaggedHardAt) {}

    /** {@code difficulty} is EASY, MEDIUM, HARD or null (untagged); {@code correctRate} is of first submissions, 0 to 1. */
    public Verdict judge(double correctRate, String difficulty, Limits limits) {
        double easyAt = "HARD".equals(difficulty) ? Math.min(limits.tooEasyAbove(), limits.hardTaggedEasyAt())
                : limits.tooEasyAbove();
        double hardAt = "EASY".equals(difficulty) ? Math.max(limits.tooHardBelow(), limits.easyTaggedHardAt())
                : limits.tooHardBelow();
        if ("HARD".equals(difficulty) ? correctRate >= easyAt : correctRate > easyAt) return Verdict.TOO_EASY;
        if ("EASY".equals(difficulty) ? correctRate <= hardAt : correctRate < hardAt) return Verdict.TOO_HARD;
        return Verdict.OK;
    }
}
