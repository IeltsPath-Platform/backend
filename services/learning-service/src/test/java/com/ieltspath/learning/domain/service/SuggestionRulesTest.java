package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.SuggestionRules.Candidate;
import com.ieltspath.learning.domain.service.SuggestionRules.Reason;
import com.ieltspath.learning.domain.service.SuggestionRules.Target;
import com.ieltspath.learning.domain.service.SuggestionRules.Type;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuggestionRulesTest {
    private final SuggestionRules rules = new SuggestionRules();
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final BigDecimal band = new BigDecimal("6.0");

    private Candidate candidate(Type type, UUID kp, String bandValue, boolean seen) {
        return new Candidate(type, UUID.randomUUID(), UUID.randomUUID(), "t", List.of(kp), new BigDecimal(bandValue),
                "READING", seen);
    }

    @Test
    void targetsAreServedInPriorityOrderOneSuggestionEach() {
        var setA = candidate(Type.PRACTICE_SET, a, "5.5", false);
        var setB = candidate(Type.PRACTICE_SET, b, "5.5", false);
        var result = rules.choose(List.of(new Target(b, Reason.DUE), new Target(a, Reason.CONSOLIDATE)),
                List.of(setA, setB), band);
        assertEquals(List.of(b, a), result.stream().map(SuggestionRules.Suggestion::knowledgePointId).toList());
        assertEquals(Reason.DUE, result.getFirst().reason());
    }

    @Test
    void contentTheLearnerAlreadyDidIsNeverSuggested() {
        var done = candidate(Type.PRACTICE_SET, a, "5.5", true);
        var fresh = candidate(Type.PRACTICE_SET, a, "5.5", false);
        var result = rules.choose(List.of(new Target(a, Reason.CONSOLIDATE)), List.of(done, fresh), band);
        assertEquals(fresh, result.getFirst().candidate());
        assertTrue(rules.choose(List.of(new Target(a, Reason.CONSOLIDATE)), List.of(done), band).isEmpty());
    }

    @Test
    void nothingAboveTheLearnersBandIsSuggested() {
        var tooHigh = candidate(Type.PRACTICE_SET, a, "6.5", false);
        var atBand = candidate(Type.LESSON, a, "6.0", false);
        var result = rules.choose(List.of(new Target(a, Reason.CONSOLIDATE)), List.of(tooHigh, atBand), band);
        assertEquals(Type.LESSON, result.getFirst().type());
    }

    @Test
    void weakPointsPreferPracticeAndNextPointsPreferALesson() {
        var set = candidate(Type.PRACTICE_SET, a, "5.5", false);
        var lesson = candidate(Type.LESSON, a, "5.5", false);
        assertEquals(Type.PRACTICE_SET,
                rules.choose(List.of(new Target(a, Reason.DUE)), List.of(lesson, set), band).getFirst().type());
        assertEquals(Type.LESSON,
                rules.choose(List.of(new Target(a, Reason.NEXT_IN_TREE)), List.of(set, lesson), band).getFirst().type());
    }

    @Test
    void anItemServingSeveralTargetsIsSuggestedOnce() {
        var shared = new Candidate(Type.PRACTICE_SET, UUID.randomUUID(), UUID.randomUUID(), "t", List.of(a, b), band,
                "READING", false);
        var result = rules.choose(List.of(new Target(a, Reason.DUE), new Target(b, Reason.DUE)), List.of(shared), band);
        assertEquals(1, result.size());
    }

    @Test
    void atMostFiveAreSuggested() {
        List<Target> targets = IntStream.range(0, 8).mapToObj(i -> new Target(UUID.randomUUID(), Reason.DUE)).toList();
        List<Candidate> candidates = targets.stream()
                .map(target -> candidate(Type.PRACTICE_SET, target.knowledgePointId(), "5.0", false)).toList();
        assertEquals(SuggestionRules.MAX_SUGGESTIONS, rules.choose(targets, candidates, band).size());
    }
}
