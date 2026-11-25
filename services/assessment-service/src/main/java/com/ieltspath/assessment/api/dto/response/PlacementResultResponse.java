package com.ieltspath.assessment.api.dto.response;

import com.ieltspath.assessment.application.result.PlacementResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.ieltspath.assessment.domain.vo.PlacementPool;

/**
 * The graded placement. {@code sections} is the report detail in test order: Listening and Reading list their
 * questions with the learner's and the expected answer, Writing its essays with the examiner comments.
 */
public record PlacementResultResponse(UUID attemptId, Double overallBand, Instant completedAt, List<SkillBand> skills,
                                      List<Section> sections, PlacementPool pool, Double ceiling,
                                      boolean reachedCeiling, boolean partial, SpeakingFeedback speakingFeedback) {
    public record SkillBand(String skill, double band) {}

    public record Section(String skill, String title, List<Question> questions, List<Essay> essays) {}

    public record Question(int number, String prompt, String learnerAnswer, String correctAnswer, boolean correct,
                           String explanation) {}

    public record Essay(String task, Double band, boolean submitted, Feedback feedback) {}

    public record Feedback(String summary, List<Criterion> criteria, List<String> focus) {}

    public record SpeakingFeedback(String summary, List<Criterion> criteria) {}

    public PlacementResultResponse(UUID attemptId, Double overallBand, Instant completedAt, List<SkillBand> skills,
                                    List<Section> sections, PlacementPool pool, Double ceiling,
                                    boolean reachedCeiling, boolean partial) {
        this(attemptId, overallBand, completedAt, skills, sections, pool, ceiling, reachedCeiling, partial, null);
    }

    public record Criterion(String code, Double band, String comment) {}

    public static PlacementResultResponse from(PlacementResult r) {
        return new PlacementResultResponse(r.attemptId(), r.overallBand(), r.completedAt(),
                r.skills().stream().map(s -> new SkillBand(s.skill().name(), s.band())).toList(),
                r.sections().stream().map(PlacementResultResponse::section).toList(), r.pool(), r.ceiling(),
                r.reachedCeiling(), r.partial(), r.speakingFeedback() == null ? null : new SpeakingFeedback(
                        r.speakingFeedback().summary(), r.speakingFeedback().criteria().stream()
                        .map(c -> new Criterion(c.code(), c.band(), c.comment())).toList()));
    }

    private static Section section(PlacementResult.Section s) {
        return new Section(s.skill().name(), s.title(),
                s.questions().stream().map(q -> new Question(q.number(), q.prompt(), q.learnerAnswer(),
                        q.correctAnswer(), q.correct(), q.explanation())).toList(),
                s.essays().stream().map(e -> new Essay(e.task(), e.band(), e.submitted(), feedback(e.feedback())))
                        .toList());
    }

    private static Feedback feedback(PlacementResult.Feedback f) {
        if (f == null) return null;
        return new Feedback(f.summary(),
                f.criteria().stream().map(c -> new Criterion(c.code(), c.band(), c.comment())).toList(), f.focus());
    }
}
