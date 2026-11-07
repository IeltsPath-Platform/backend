package com.ieltspath.library.domain.study;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class StudyHeartsTest {
    final Instant at=Instant.parse("2026-11-07T08:00:00Z");
    @Test void fullClockStopsAndFirstLossStartsRegeneration() {
        var h=StudyHearts.start(UUID.randomUUID(),5,at,Duration.ofHours(5));h.regen(at.plusSeconds(86400));assertEquals(5,h.hearts());
        var lost=at.plusSeconds(86400+600);h.lose(1,lost);assertEquals(lost.plus(Duration.ofHours(5)),h.nextHeartAt().orElseThrow());
        h.regen(lost.plus(Duration.ofHours(5)));assertEquals(5,h.hearts());assertTrue(h.nextHeartAt().isEmpty());
    }
    @Test void zeroRegeneratesOneEveryFiveHoursAndNeverGoesNegative() {
        var h=StudyHearts.start(UUID.randomUUID(),5,at,Duration.ofHours(5));h.lose(20,at);assertEquals(0,h.hearts());
        h.regen(at.plus(Duration.ofHours(5)));assertEquals(1,h.hearts());h.regen(at.plus(Duration.ofHours(10)));assertEquals(2,h.hearts());
        h.refillFull(at.plus(Duration.ofHours(10)));assertEquals(5,h.hearts());
    }
    @Test void premiumUpgradeDoesNotRefillAndDowngradeClamps() {
        var h=StudyHearts.start(UUID.randomUUID(),5,at,Duration.ofHours(5));h.setMax(7,at);assertEquals(5,h.hearts());assertEquals(7,h.max());
        h.refillFull(at);assertEquals(7,h.hearts());h.setMax(5,at);assertEquals(5,h.hearts());
        h.setMax(7,at);h.lose(2,at);h.setMax(5,at);assertEquals(3,h.hearts());
    }
}
