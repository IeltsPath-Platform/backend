package com.group01.assessment.application.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.assessment.domain.service.AnswerSpecGrader;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * The server-side answer an item was frozen with ({@code {answerSpec, explanation, maxScore}}, written by
 * {@link AttemptCreator}). Items without one, such as those of attempts started before Content supplied answers,
 * read as ungradable and stay on the human grading path.
 */
record AnswerSnapshot(Map<String, Object> answerSpec, String explanation, Double maxScore) {
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    static Optional<AnswerSnapshot> parse(ObjectMapper json, String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            Map<String, Object> snapshot = json.readValue(value, MAP);
            Object spec = snapshot.get("answerSpec");
            Object maxScore = snapshot.get("maxScore");
            Object explanation = snapshot.get("explanation");
            @SuppressWarnings("unchecked")
            Map<String, Object> answerSpec = spec instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
            return Optional.of(new AnswerSnapshot(answerSpec,
                    explanation instanceof String text ? text : null,
                    maxScore instanceof Number number ? number.doubleValue() : null));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    boolean autoGradable(AnswerSpecGrader grader) {
        return maxScore != null && maxScore > 0 && grader.grade(answerSpec, null).gradable();
    }

    /** A Writing essay with a numeric pass band, graded by band rather than by answer matching. */
    boolean gradableEssay() {
        return answerSpec != null && "ESSAY".equals(answerSpec.get("type")) && passBand() != null
                && maxScore != null && maxScore > 0;
    }

    BigDecimal passBand() {
        Object band = answerSpec == null ? null : answerSpec.get("passBand");
        return band instanceof Number number ? new BigDecimal(number.toString()) : null;
    }
}
