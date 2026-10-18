package com.ieltspath.assessment.domain.service;

import com.ieltspath.assessment.domain.vo.AttemptScope;
import com.ieltspath.assessment.domain.vo.Skill;

import java.util.Optional;

/**
 * Estimates the IELTS band of a full Reading or Listening practice test from its raw score. The public conversion
 * tables are written for 40 questions, so a shorter test is scaled to 40 first. A single part has no band.
 */
public final class PracticeBandEstimator {
    private static final int QUESTIONS = 40;

    /** {minimum raw score out of 40, band times ten}, highest first. */
    private static final int[][] READING = {{39, 90}, {37, 85}, {35, 80}, {33, 75}, {30, 70}, {27, 65}, {23, 60},
            {19, 55}, {15, 50}, {13, 45}, {10, 40}, {8, 35}, {6, 30}, {4, 25}};
    private static final int[][] LISTENING = {{39, 90}, {37, 85}, {35, 80}, {32, 75}, {30, 70}, {26, 65}, {23, 60},
            {18, 55}, {16, 50}, {13, 45}, {11, 40}, {8, 35}, {6, 30}, {4, 25}};

    private PracticeBandEstimator() {}

    public static Optional<Double> estimate(Skill skill, AttemptScope scope, double rawScore, double maxScore) {
        if (scope != AttemptScope.FULL || maxScore <= 0 || rawScore < 0) return Optional.empty();
        int[][] table = switch (skill) {
            case READING -> READING;
            case LISTENING -> LISTENING;
            default -> null;
        };
        if (table == null) return Optional.empty();
        long scaled = Math.round(Math.min(rawScore, maxScore) / maxScore * QUESTIONS);
        for (int[] row : table) {
            if (scaled >= row[0]) return Optional.of(row[1] / 10.0);
        }
        return Optional.of(scaled >= 2 ? 2.0 : scaled >= 1 ? 1.0 : 0.0);
    }
}
