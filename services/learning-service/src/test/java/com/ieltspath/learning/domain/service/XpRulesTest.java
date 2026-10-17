package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.XpRules.Candidate;
import com.ieltspath.learning.domain.service.XpRules.Params;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XpRulesTest {
    private static final Params DEFAULTS = Params.defaults();

    @Test
    void aCorrectAnswerIsWorthMoreTheHarderItIs() {
        assertThat(DEFAULTS.correctAnswerXp("EASY")).isEqualTo(10);
        assertThat(DEFAULTS.correctAnswerXp("MEDIUM")).isEqualTo(15);
        assertThat(DEFAULTS.correctAnswerXp("HARD")).isEqualTo(20);
        assertThat(DEFAULTS.correctAnswerXp(null)).as("no difficulty tag").isEqualTo(10);
        assertThat(DEFAULTS.correctAnswerXp("UNHEARD_OF")).isEqualTo(10);
    }

    @Test
    void twelveHardAnswersInOneDayCountOneHundredAndShadowTheRest() {
        List<Candidate> twelve = hard(12);

        List<XpRules.Award> awards = XpRules.award(DEFAULTS, twelve, 0);

        assertThat(awards).hasSize(12);
        assertThat(awards.stream().filter(XpRules.Award::counted).mapToInt(XpRules.Award::amount).sum())
                .isEqualTo(100);
        assertThat(awards.stream().filter(award -> !award.counted()).mapToInt(XpRules.Award::amount).sum())
                .isEqualTo(140);
    }

    @Test
    void theCapCountsWhatIsAlreadyCountedToday() {
        List<XpRules.Award> awards = XpRules.award(DEFAULTS, hard(2), 90);

        // 90 counted, so a 20-point answer does not fit; both stay shadow.
        assertThat(awards).extracting(XpRules.Award::counted).containsExactly(false, false);
    }

    @Test
    void aLaterCheaperAnswerStillCountsUnderTheCap() {
        List<Candidate> mixed = new ArrayList<>(hard(4));
        mixed.add(new Candidate(UUID.randomUUID(), "EASY"));

        List<XpRules.Award> awards = XpRules.award(DEFAULTS, mixed, 0);

        // 80 counted after four HARD answers; the EASY one fits under 100 and counts.
        assertThat(awards).extracting(XpRules.Award::counted).containsExactly(true, true, true, true, true);
    }

    @Test
    void theLevelGoesUpEveryThousandPoints() {
        assertThat(DEFAULTS.levelOf(0)).isEqualTo(1);
        assertThat(DEFAULTS.levelOf(999)).isEqualTo(1);
        assertThat(DEFAULTS.levelOf(1000)).isEqualTo(2);
        assertThat(DEFAULTS.levelOf(2500)).isEqualTo(3);
    }

    @Test
    void invalidValuesAreRejected() {
        assertThatThrownBy(() -> new Params(0, 15, 20, 10, 100, 1000, 25, 50))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Params(10, 15, 20, 10, -1, 1000, 25, 50))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<Candidate> hard(int count) {
        List<Candidate> candidates = new ArrayList<>();
        for (int index = 0; index < count; index++) candidates.add(new Candidate(UUID.randomUUID(), "HARD"));
        return candidates;
    }
}
