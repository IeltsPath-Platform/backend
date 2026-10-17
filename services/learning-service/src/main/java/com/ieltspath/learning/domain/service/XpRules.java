package com.ieltspath.learning.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * XP rules of the gamification layer: what a correct answer is worth by its difficulty, how many answer points a learner
 * can count in a day, and the level a total gives. Pure Java; the numbers come from {@code learning.gamification.*}.
 */
public final class XpRules {
    private XpRules() {}

    /**
     * XP values. {@code unknownXp} applies when Content gives no difficulty for a question. {@code dailyCap} limits the
     * answer XP counted per learner day; the milestone XP for a knowledge point is not limited.
     */
    public record Params(int easyXp, int mediumXp, int hardXp, int unknownXp, int dailyCap, int levelXp,
                         int consolidateXp, int maintainXp) {
        public Params {
            if (easyXp < 1 || mediumXp < 1 || hardXp < 1 || unknownXp < 1 || levelXp < 1 || consolidateXp < 1
                    || maintainXp < 1 || dailyCap < 0) {
                throw new IllegalArgumentException("XP values must be positive and the daily cap not negative");
            }
        }

        public static Params defaults() {
            return new Params(10, 15, 20, 10, 100, 1000, 25, 50);
        }

        public int correctAnswerXp(String difficulty) {
            if (difficulty == null) return unknownXp;
            return switch (difficulty) {
                case "EASY" -> easyXp;
                case "MEDIUM" -> mediumXp;
                case "HARD" -> hardXp;
                default -> unknownXp;
            };
        }

        /** Level 1 starts at zero XP; every {@code levelXp} points adds one level. */
        public int levelOf(int totalXp) {
            return 1 + totalXp / levelXp;
        }
    }

    /** A correct answer and its difficulty tag, if Content has one. */
    public record Candidate(UUID questionVersionId, String difficulty) {}

    /** One award: the XP of a correct answer and whether it counts toward the day's cap and the total. */
    public record Award(UUID questionVersionId, int amount, boolean counted) {}

    /**
     * Awards the correct answers of one submission in order. An answer that would push the day over the cap is kept as
     * shadow XP ({@code counted = false}); a later answer that still fits under the cap counts. {@code alreadyCounted}
     * is the answer XP the learner has already counted today.
     */
    public static List<Award> award(Params params, List<Candidate> correct, long alreadyCounted) {
        List<Award> awards = new ArrayList<>();
        long counted = alreadyCounted;
        for (Candidate candidate : correct) {
            int amount = params.correctAnswerXp(candidate.difficulty());
            boolean fits = counted + amount <= params.dailyCap();
            if (fits) counted += amount;
            awards.add(new Award(candidate.questionVersionId(), amount, fits));
        }
        return List.copyOf(awards);
    }
}
