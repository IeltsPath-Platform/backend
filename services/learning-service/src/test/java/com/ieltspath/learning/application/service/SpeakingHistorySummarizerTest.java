package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.QuestionEventLog.RecentEvent;
import com.ieltspath.learning.application.service.SpeakingHistorySummarizer.ErrorCount;
import com.ieltspath.learning.application.service.SpeakingHistorySummarizer.HistoryLine;
import com.ieltspath.learning.domain.vo.MasteryTier;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SpeakingHistorySummarizerTest {

    @Test
    void trendNeedsAtLeastFourJudgedAnswers() {
        assertThat(SpeakingHistorySummarizer.trend(events(true, true, false))).isNull();
    }

    @Test
    void newerAnswersRightWhereOlderWereWrongIsImproving() {
        assertThat(SpeakingHistorySummarizer.trend(events(true, true, false, false))).isEqualTo("improving");
    }

    @Test
    void newerAnswersWrongWhereOlderWereRightIsDeclining() {
        assertThat(SpeakingHistorySummarizer.trend(events(false, false, true, true))).isEqualTo("declining");
    }

    @Test
    void anUnclearShiftIsSteady() {
        assertThat(SpeakingHistorySummarizer.trend(events(true, false, true, false))).isEqualTo("steady");
    }

    @Test
    void rendersWordsOnlyWithNoNumberForMasteryOrBands() {
        String line = SpeakingHistorySummarizer.render(new HistoryLine(UUID.randomUUID(), "S60_FLUENCY", "from writing",
                MasteryTier.CONSOLIDATE, null, List.of(new ErrorCount("SPK_NO_LINKER", 3)), false));

        assertThat(line).isEqualTo("S60_FLUENCY [from writing] needs consolidation; recent errors: SPK_NO_LINKER x3");
    }

    /** Newest first, as the event log returns them. */
    private static List<RecentEvent> events(boolean... correct) {
        List<RecentEvent> events = new ArrayList<>();
        for (boolean value : correct) events.add(new RecentEvent(UUID.randomUUID(), value, 1000L, null, null, Instant.now()));
        return events;
    }
}
