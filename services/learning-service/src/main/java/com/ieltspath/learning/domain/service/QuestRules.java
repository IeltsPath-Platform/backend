package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.QuestionEventSource;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The three daily quests and what counts toward them. Every learner gets a practice quest, a streak quest, and either a
 * weakness quest aimed at one knowledge point or, when none suits, a longer practice quest.
 */
public final class QuestRules {
    private QuestRules() {}

    public enum Type { PRACTICE, WEAKNESS, PRACTICE_PLUS, STREAK }

    public record Params(int practiceRequired, int practiceXp, int practicePoints, int weaknessRequired,
                         int weaknessXp, int plusRequired, int plusXp, int streakXp) {}

    public record Spec(Type type, int required, int xp, int points) {}

    /** A knowledge point still being built; {@code practicable} means a practice set or a blocking review exists. */
    public record Candidate(UUID knowledgePointId, double mastery, boolean blockingReview, boolean practicable) {}

    /** The three quests of a day. */
    public static List<Spec> dailySet(Params params, boolean hasWeaknessTarget) {
        Spec second = hasWeaknessTarget
                ? new Spec(Type.WEAKNESS, params.weaknessRequired(), params.weaknessXp(), 0)
                : new Spec(Type.PRACTICE_PLUS, params.plusRequired(), params.plusXp(), 0);
        return List.of(new Spec(Type.PRACTICE, params.practiceRequired(), params.practiceXp(), params.practicePoints()),
                second, new Spec(Type.STREAK, 1, params.streakXp(), 0));
    }

    /** The point to work on: one with a blocking review first, then the lowest mastery. */
    public static Optional<UUID> pickWeakness(List<Candidate> candidates) {
        return candidates.stream().filter(Candidate::practicable)
                .min(Comparator.comparing((Candidate candidate) -> !candidate.blockingReview())
                        .thenComparingDouble(Candidate::mastery).thenComparing(Candidate::knowledgePointId))
                .map(Candidate::knowledgePointId);
    }

    /**
     * Whether an answer counts as a practice answer: a judged answer from a lesson (an exercise or an essay), Practice
     * (an essay is an item of the set), free practice or review, on its first submission. Exams and diagnostic probes
     * do not count.
     */
    public static boolean isPracticeAnswer(QuestionEventSource source, Boolean correct, boolean firstSubmission) {
        return firstSubmission && correct != null && (source == QuestionEventSource.LESSON_EXERCISE
                || source == QuestionEventSource.LESSON_WRITING
                || source == QuestionEventSource.PRACTICE_SET || source == QuestionEventSource.FREE_PRACTICE
                || source == QuestionEventSource.REVIEW_SET);
    }

    /** Whether an answer counts toward the weakness quest: free practice or review, on the target point. */
    public static boolean isWeaknessAnswer(QuestionEventSource source, Boolean correct, boolean firstSubmission,
                                           List<UUID> knowledgePointIds, UUID target) {
        return target != null && firstSubmission && correct != null && knowledgePointIds.contains(target)
                && (source == QuestionEventSource.FREE_PRACTICE || source == QuestionEventSource.REVIEW_SET);
    }
}
