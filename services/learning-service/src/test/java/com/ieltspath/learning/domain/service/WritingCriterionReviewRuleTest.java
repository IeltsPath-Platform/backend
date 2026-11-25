package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.FailedWritingCriterion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WritingCriterionReviewRuleTest {
    private final WritingCriterionReviewRule rule = new WritingCriterionReviewRule();
    private final FailedWritingCriterion lr = failed("LR", "5.0");
    private final FailedWritingCriterion gra = failed("GRA", "4.5");
    private final FailedWritingCriterion cc = failed("CC", "5.0");

    @Test void selectsTheWeakestTwoAndBreaksBandTiesByCode() {
        assertEquals(List.of(gra, cc), rule.select(List.of(lr, gra, cc), Set.of(), available(), 2));
    }

    @Test void skipsPendingAndExhaustedPoints() {
        assertEquals(List.of(lr), rule.select(List.of(lr, gra, cc), Set.of(gra.knowledgePointId()),
                Map.of(lr.knowledgePointId(), 1), 2));
    }

    @Test void deduplicatesAPointKeepingItsLowestBand() {
        var lower = new FailedWritingCriterion(lr.knowledgePointId(), "LR", new BigDecimal("4.0"));
        assertEquals(List.of(lower), rule.select(List.of(lr, lower), Set.of(), available(), 2));
    }

    @Test void noFailuresOrZeroLimitCreatesNothing() {
        assertTrue(rule.select(List.of(), Set.of(), available(), 2).isEmpty());
        assertTrue(rule.select(List.of(lr), Set.of(), available(), 0).isEmpty());
    }

    private Map<UUID, Integer> available() {
        return Map.of(lr.knowledgePointId(), 2, gra.knowledgePointId(), 1, cc.knowledgePointId(), 1);
    }
    private static FailedWritingCriterion failed(String code, String band) {
        return new FailedWritingCriterion(UUID.randomUUID(), code, new BigDecimal(band));
    }

    @Test void aWeakCriterionReviewsThePointOfItsMostFrequentError() {
        UUID errorPoint = UUID.randomUUID();
        var weak = lr.withReviewPoints(List.of(errorPoint));

        var selected = rule.select(List.of(weak), Set.of(), Map.of(errorPoint, 1, lr.knowledgePointId(), 1), 2);

        assertEquals(errorPoint, selected.getFirst().practisedPoint());
        assertEquals(lr.knowledgePointId(), selected.getFirst().knowledgePointId(), "the criterion's own point is kept");
    }

    @Test void theNextErrorPointIsTriedWhenTheBestHasNoUnseenSetOrIsWaiting() {
        UUID best = UUID.randomUUID(), second = UUID.randomUUID(), third = UUID.randomUUID();
        var weak = lr.withReviewPoints(List.of(best, second, third));

        assertEquals(second, rule.select(List.of(weak), Set.of(), Map.of(second, 2, third, 2), 2).getFirst().practisedPoint());
        assertEquals(third, rule.select(List.of(weak), Set.of(second), Map.of(second, 2, third, 2), 2).getFirst().practisedPoint());
    }

    @Test void withNoErrorPointToPractiseTheCriterionReviewsItsOwnPoint() {
        UUID errorPoint = UUID.randomUUID();
        var weak = lr.withReviewPoints(List.of(errorPoint));

        var selected = rule.select(List.of(weak), Set.of(), Map.of(lr.knowledgePointId(), 1), 2);

        assertEquals(lr.knowledgePointId(), selected.getFirst().practisedPoint());
        assertNull(selected.getFirst().reviewKnowledgePointId());
    }

    @Test void aCriterionWithNothingToPractiseIsLeftOut() {
        UUID errorPoint = UUID.randomUUID();

        assertTrue(rule.select(List.of(lr.withReviewPoints(List.of(errorPoint))), Set.of(), Map.of(), 2).isEmpty());
    }

    @Test void aWaitingReviewOfTheCriterionPointStopsTheCriterion() {
        UUID errorPoint = UUID.randomUUID();

        assertTrue(rule.select(List.of(lr.withReviewPoints(List.of(errorPoint))), Set.of(lr.knowledgePointId()),
                Map.of(errorPoint, 3, lr.knowledgePointId(), 3), 2).isEmpty());
    }

    @Test void theErrorPointDoesNotChangeWhichCriteriaAreWeakest() {
        UUID errorPoint = UUID.randomUUID();
        var weakGra = gra.withReviewPoints(List.of(errorPoint));

        var selected = rule.select(List.of(lr, weakGra, cc), Set.of(), Map.of(errorPoint, 1, lr.knowledgePointId(), 1,
                cc.knowledgePointId(), 1), 2);

        assertEquals(List.of("GRA", "CC"), selected.stream().map(FailedWritingCriterion::code).toList());
    }
}
