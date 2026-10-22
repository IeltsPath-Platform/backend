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
}
