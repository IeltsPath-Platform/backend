package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.WritingResultView.ErrorGroup;
import com.ieltspath.learning.domain.service.WritingResultView.Span;
import com.ieltspath.learning.domain.vo.WritingGrade;
import com.ieltspath.learning.domain.vo.WritingGrade.Correction;
import com.ieltspath.learning.domain.vo.WritingGrade.ErrorExample;
import com.ieltspath.learning.domain.vo.WritingGrade.Priority;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WritingResultViewTest {
    private static Correction correction(String excerpt, String code, int severity, String certainty, UUID practiceId) {
        return new Correction(excerpt, "fix", "GRAMMAR", code, code + " name", code + " hint", severity, practiceId,
                practiceId == null ? null : "KP", "because", certainty);
    }

    private static Correction error(String excerpt, String code, int severity) {
        return correction(excerpt, code, severity, "ERROR", null);
    }

    private static WritingGrade grade(List<Correction> corrections, List<Priority> priorities,
                                      List<ErrorExample> examples) {
        return new WritingGrade(List.of(), corrections, "s", new BigDecimal("6.0"), List.of(), null, null, List.of(),
                priorities, examples);
    }

    @Test
    void aRepeatedPhraseGetsItsOccurrencesInTheOrderOfTheCorrections() {
        String essay = "He go. She is fine. He go.";

        List<Span> spans = WritingResultView.locate(essay, List.of(error("He go", "A", 1), error("He go", "A", 1),
                error("He go", "A", 1)));

        assertEquals(new Span(0, 5), spans.get(0));
        assertEquals(new Span(20, 25), spans.get(1));
        assertNull(spans.get(2), "a third mention with no third occurrence has no position");
    }

    @Test
    void aQuotationThatIsNotInTheEssayHasNoPositionAndTheOthersAreUnaffected() {
        List<Span> spans = WritingResultView.locate("He go to school.",
                List.of(error("missing", "A", 1), error("go to", "A", 1)));

        assertNull(spans.get(0));
        assertEquals(new Span(3, 8), spans.get(1));
    }

    @Test
    void overlappingQuotationsEachKeepTheirOwnPosition() {
        List<Span> spans = WritingResultView.locate("Sales fell to 10 in 2021.",
                List.of(error("Sales fell to 10 in 2021.", "A", 1), error("fell to 10", "B", 1)));

        assertEquals(new Span(0, 25), spans.get(0));
        assertEquals(new Span(6, 16), spans.get(1));
    }

    @Test
    void groupsCountOnlyErrorsAndSortByCountTimesSeverity() {
        List<Correction> corrections = List.of(error("a", "PLURAL", 1), error("b", "PLURAL", 1),
                error("c", "PLURAL", 1), error("d", "TENSE", 3), error("e", "TENSE", 3),
                correction("f", "WORD", 3, "SUGGESTION", null));

        List<ErrorGroup> groups = WritingResultView.errorGroups(corrections, List.of());

        assertEquals(List.of("TENSE", "PLURAL"), groups.stream().map(ErrorGroup::errorCode).toList());
        assertEquals(2, groups.get(0).count());
        assertEquals(3, groups.get(0).severity());
        assertEquals(3, groups.get(1).count());
    }

    @Test
    void aSuggestionNeverStartsAGroup() {
        assertTrue(WritingResultView.errorGroups(List.of(correction("a", "WORD", 2, "SUGGESTION", null)), List.of())
                .isEmpty());
    }

    @Test
    void fallbackCodesComeLastAndCorrectionsWithoutACodeAreSkipped() {
        List<Correction> corrections = List.of(error("a", "GRAMMAR_OTHER", 3), error("b", "GRAMMAR_OTHER", 3),
                error("c", "PLURAL", 1), error("d", null, 3));

        List<ErrorGroup> groups = WritingResultView.errorGroups(corrections, List.of());

        assertEquals(List.of("PLURAL", "GRAMMAR_OTHER"), groups.stream().map(ErrorGroup::errorCode).toList());
    }

    @Test
    void aGroupCarriesItsExampleAndTheFirstPracticeSetOfItsCorrections() {
        UUID practice = UUID.randomUUID();
        List<Correction> corrections = List.of(error("a", "PLURAL", 1),
                correction("b", "PLURAL", 1, "ERROR", practice));

        ErrorGroup group = WritingResultView.errorGroups(corrections,
                List.of(new ErrorExample("PLURAL", "two dog", "two dogs"), new ErrorExample("TENSE", "x", "y"))).getFirst();

        assertEquals("two dog", group.example().wrong());
        assertEquals(practice, group.practiceKnowledgePointId());
        assertEquals("PLURAL name", group.errorName());
        assertEquals("PLURAL hint", group.fixHint());
    }

    @Test
    void aGroupWithoutAnExampleOrPracticeSetCarriesNeither() {
        ErrorGroup group = WritingResultView.errorGroups(List.of(error("a", "PLURAL", 1)), List.of()).getFirst();

        assertNull(group.example());
        assertNull(group.practiceKnowledgePointId());
    }

    @Test
    void prioritiesFromTheModelComeFirstAtMostThreeWithThePracticeSetOfTheirCode() {
        UUID practice = UUID.randomUUID();
        List<Correction> corrections = List.of(error("a", "X", 1), correction("b", "PLURAL", 1, "ERROR", practice));
        List<Priority> ai = List.of(
                new Priority("t1", "e1", List.of("a"), "act1", List.of("PLURAL")),
                new Priority("t2", "e2", List.of(), "act2", List.of("UNKNOWN")),
                new Priority("t3", "e3", List.of(), "act3", List.of()),
                new Priority("t4", "e4", List.of(), "act4", List.of()));

        var priorities = WritingResultView.priorities(grade(corrections, ai, List.of()));

        assertEquals(3, priorities.size());
        assertEquals("AI", priorities.getFirst().source());
        assertEquals(practice, priorities.get(0).practiceKnowledgePointId());
        assertNull(priorities.get(1).practiceKnowledgePointId());
    }

    @Test
    void withoutModelPrioritiesTheTopGroupsBecomeThemFromTheRules() {
        List<Correction> corrections = Arrays.asList(error("a b", "PLURAL", 2), error("c", "PLURAL", 2),
                error("d", "TENSE", 1), error("e", "WORD", 1), error("f", "ORDER", 1));

        var priorities = WritingResultView.priorities(grade(corrections, List.of(), List.of()));

        assertEquals(3, priorities.size());
        var first = priorities.getFirst();
        assertEquals("RULE", first.source());
        assertEquals("PLURAL name", first.title());
        assertEquals("PLURAL hint", first.action());
        assertEquals("because", first.explanation());
        assertEquals(List.of("a b"), first.excerpts());
        assertEquals(List.of("PLURAL"), first.errorCodes());
    }

    @Test
    void anEssayWithoutErrorsHasNoPriorities() {
        assertTrue(WritingResultView.priorities(grade(List.of(), List.of(), List.of())).isEmpty());
    }
}
