package com.ieltspath.assessment.application.result;

import com.ieltspath.assessment.domain.vo.Skill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.ieltspath.assessment.domain.vo.PlacementPool;

/**
 * A learner's completed placement: the overall band, the skill bands it was averaged from and, per section in test
 * order, what the report shows (each objective question with its answers, or each essay with the grader's comments).
 */
public record PlacementResult(UUID attemptId, Double overallBand, Instant completedAt, List<SkillBand> skills,
                              List<Section> sections, PlacementPool pool, Double ceiling,
                              boolean reachedCeiling, boolean partial, SpeakingFeedback speakingFeedback) {
    public PlacementResult(UUID attemptId, Double overallBand, Instant completedAt, List<SkillBand> skills,
                           List<Section> sections) {
        this(attemptId, overallBand, completedAt, skills, sections, null, null, false, false, null);
    }
    public PlacementResult(UUID attemptId, Double overallBand, Instant completedAt, List<SkillBand> skills) {
        this(attemptId, overallBand, completedAt, skills, List.of(), null, null, false, false, null);
    }

    public PlacementResult(UUID attemptId, Double overallBand, Instant completedAt, List<SkillBand> skills,
                           List<Section> sections, PlacementPool pool, Double ceiling,
                           boolean reachedCeiling, boolean partial) {
        this(attemptId, overallBand, completedAt, skills, sections, pool, ceiling, reachedCeiling, partial, null);
    }

    public record SkillBand(Skill skill, double band) {}

    public record Section(Skill skill, String title, List<Question> questions, List<Essay> essays) {}

    /** {@code learnerAnswer} is null when the question was left blank. */
    public record Question(int number, String prompt, String learnerAnswer, String correctAnswer, boolean correct,
                           String explanation) {}

    /**
     * {@code band} is null while the essay is not graded; {@code feedback} is null when it was graded without comments
     * (no LLM reply, the default band, or no essay sent).
     */
    public record Essay(String task, Double band, boolean submitted, Feedback feedback) {}

    public record Feedback(String summary, List<Criterion> criteria, List<String> focus) {}

    public record Criterion(String code, Double band, String comment) {}

    public record SpeakingFeedback(String summary, List<Criterion> criteria) {
        public SpeakingFeedback { criteria = criteria == null ? List.of() : List.copyOf(criteria); }
    }
}
