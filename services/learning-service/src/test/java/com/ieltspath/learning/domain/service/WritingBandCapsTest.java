package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.WritingBandCaps.Caps;
import com.ieltspath.learning.domain.service.WritingBandCaps.Flags;
import com.ieltspath.learning.domain.service.WritingBandCaps.Limit;
import com.ieltspath.learning.domain.service.WritingBandCaps.Reason;
import com.ieltspath.learning.domain.vo.WritingTask;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WritingBandCapsTest {
    // Thresholds here are placeholders for the tests only; the examiner sets the real ones.
    private static final Caps CAPS = new Caps(bd("5.0"), bd("4.5"), bd("5.5"), bd("5.5"), bd("6.0"));

    @Test
    void noRuleFiresWhenTheEssayMeetsEveryCondition() {
        assertTrue(WritingBandCaps.evaluate(WritingTask.TASK_2, 260, 250, Flags.NONE, CAPS).isEmpty());
    }

    @Test
    void anEssayUnderTheMinimumIsCappedOnTheFirstCriterionOfItsTask() {
        List<Limit> onTaskOne = WritingBandCaps.evaluate(WritingTask.TASK_1, 120, 150, Flags.NONE, CAPS);
        List<Limit> onTaskTwo = WritingBandCaps.evaluate(WritingTask.TASK_2, 200, 250, Flags.NONE, CAPS);

        assertEquals(List.of(new Limit(Reason.TOO_SHORT, "TA", bd("5.0"), "Bài chưa đủ 150 từ nên TA bị giới hạn ở 5")), onTaskOne);
        assertEquals("TR", onTaskTwo.getFirst().criterion());
    }

    @Test
    void anEssayExactlyAtTheMinimumIsNotShort() {
        assertTrue(WritingBandCaps.evaluate(WritingTask.TASK_2, 250, 250, Flags.NONE, CAPS).isEmpty());
    }

    @Test
    void offTopicLowersTheFirstCriterionOnBothTasks() {
        Flags offTopic = new Flags(true, false, false, false);

        assertEquals(Reason.OFF_TOPIC, WritingBandCaps.evaluate(WritingTask.TASK_1, 200, 150, offTopic, CAPS).getFirst().reason());
        assertEquals("TR", WritingBandCaps.evaluate(WritingTask.TASK_2, 300, 250, offTopic, CAPS).getFirst().criterion());
    }

    @Test
    void noOverviewIsATaskOneRuleOnly() {
        Flags noOverview = new Flags(false, true, false, false);

        assertEquals(List.of(Reason.T1_NO_OVERVIEW), WritingBandCaps.evaluate(WritingTask.TASK_1, 200, 150, noOverview, CAPS)
                .stream().map(Limit::reason).toList());
        assertTrue(WritingBandCaps.evaluate(WritingTask.TASK_2, 300, 250, noOverview, CAPS).isEmpty());
    }

    @Test
    void partialAnswerAndNoPositionAreTaskTwoRulesOnly() {
        Flags both = new Flags(false, false, true, true);

        List<Reason> reasons = WritingBandCaps.evaluate(WritingTask.TASK_2, 300, 250, both, CAPS).stream()
                .map(Limit::reason).toList();

        assertEquals(List.of(Reason.PARTIAL_ANSWER, Reason.NO_POSITION), reasons);
        assertTrue(WritingBandCaps.evaluate(WritingTask.TASK_1, 200, 150, both, CAPS).isEmpty());
    }

    @Test
    void aNullCapSwitchesItsRuleOff() {
        Caps noShortRule = new Caps(null, bd("4.5"), bd("5.5"), bd("5.5"), bd("6.0"));

        assertTrue(WritingBandCaps.evaluate(WritingTask.TASK_2, 100, 250, Flags.NONE, noShortRule).isEmpty());
        assertTrue(WritingBandCaps.evaluate(WritingTask.TASK_2, 100, 250, Flags.NONE, Caps.NONE).isEmpty());
    }

    @Test
    void aCapOnlyLowersABand() {
        List<Limit> limits = WritingBandCaps.evaluate(WritingTask.TASK_2, 100, 250, Flags.NONE, CAPS);

        assertEquals(Map.of("TR", bd("4.0"), "CC", bd("6.0")),
                WritingBandCaps.apply(Map.of("TR", bd("4.0"), "CC", bd("6.0")), limits));
    }

    @Test
    void theLowestCapOnACriterionWins() {
        Flags many = new Flags(false, false, true, true);
        List<Limit> limits = WritingBandCaps.evaluate(WritingTask.TASK_2, 100, 250, many, CAPS);

        assertEquals(Map.of("TR", bd("5.0")), WritingBandCaps.apply(Map.of("TR", bd("8.0")), limits));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
