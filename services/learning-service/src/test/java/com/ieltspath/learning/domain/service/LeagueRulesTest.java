package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.LeagueRules.Outcome;
import com.ieltspath.learning.domain.vo.LeagueTier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LeagueRulesTest {
    @ParameterizedTest
    @CsvSource({"1,30,PROMOTED", "7,30,PROMOTED", "8,30,STAYED", "22,30,STAYED", "23,30,DEMOTED", "30,30,DEMOTED",
            "23,23,DEMOTED", "8,22,STAYED", "22,22,STAYED"})
    void topSevenGoUpAndRanksTwentyThreeAndBelowGoDown(int rank, int size, Outcome expected) {
        assertThat(LeagueRules.outcome(rank, size)).isEqualTo(expected);
    }

    @Test
    void aSmallGroupDemotesNobody() {
        for (int rank = 1; rank <= 22; rank++) {
            assertThat(LeagueRules.outcome(rank, 22)).isNotEqualTo(Outcome.DEMOTED);
        }
    }

    @Test
    void theTopTierIsKeptOnPromotionAndTheBottomOnDemotion() {
        assertThat(LeagueRules.next(LeagueTier.AMETHYST, Outcome.PROMOTED)).isEqualTo(LeagueTier.AMETHYST);
        assertThat(LeagueRules.next(LeagueTier.BRONZE, Outcome.DEMOTED)).isEqualTo(LeagueTier.BRONZE);
        assertThat(LeagueRules.next(LeagueTier.GOLD, Outcome.PROMOTED)).isEqualTo(LeagueTier.SAPPHIRE);
        assertThat(LeagueRules.next(LeagueTier.GOLD, Outcome.DEMOTED)).isEqualTo(LeagueTier.SILVER);
        assertThat(LeagueRules.next(LeagueTier.GOLD, Outcome.STAYED)).isEqualTo(LeagueTier.GOLD);
    }

    @Test
    void theTableZonesFollowTheOutcome() {
        assertThat(LeagueRules.zone(3, 30)).isEqualTo(LeagueRules.Zone.PROMOTION);
        assertThat(LeagueRules.zone(15, 30)).isEqualTo(LeagueRules.Zone.SAFE);
        assertThat(LeagueRules.zone(25, 30)).isEqualTo(LeagueRules.Zone.DEMOTION);
    }

    @Test
    void aLeagueWeekRunsFromSundayToSundayInUtc() {
        // 2026-10-17 is a Saturday: its week began on Sunday 2026-10-11 and closes on Sunday 2026-10-18 00:00 UTC.
        LocalDate start = LeagueRules.weekStart(Instant.parse("2026-10-17T23:59:00Z"));
        assertThat(start).isEqualTo(LocalDate.of(2026, 10, 11));
        assertThat(LeagueRules.closesAt(start)).isEqualTo(Instant.parse("2026-10-18T00:00:00Z"));
        assertThat(LeagueRules.weekStart(Instant.parse("2026-10-18T00:00:00Z"))).isEqualTo(LocalDate.of(2026, 10, 18));
    }
}
