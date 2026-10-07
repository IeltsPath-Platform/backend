package com.ieltspath.learning.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WritingScoreTest {
    @ParameterizedTest
    @CsvSource({
            "6;6;6;7, 6.5",
            "6;6;7;7, 6.5",
            "6;7;7;7, 7.0",
            "5.5;6;6;6, 6.0",
            "5;5;5;5.5, 5.0",
            "9;9;9;9, 9.0",
            "0;0;0;0.5, 0.0"
    })
    void overallBandRoundsTheMeanToTheIeltsHalfBand(String bands, String expected) {
        List<BigDecimal> values = Arrays.stream(bands.split(";")).map(BigDecimal::new).toList();
        assertEquals(new BigDecimal(expected), WritingScore.overallBand(values));
    }

    @Test
    void bandsAreZeroToNineInHalfSteps() {
        for (String band : List.of("0", "0.5", "6.5", "9", "9.0")) assertTrue(WritingScore.isBand(new BigDecimal(band)));
        for (String band : List.of("-0.5", "9.5", "6.3", "6.25")) assertFalse(WritingScore.isBand(new BigDecimal(band)));
        assertFalse(WritingScore.isBand(null));
    }

    @Test
    void wordsJoinApostrophesAndHyphensButNotPunctuation() {
        assertEquals(0, WritingScore.countWords("  \n "));
        assertEquals(0, WritingScore.countWords(null));
        assertEquals(5, WritingScore.countWords("Don't  stop: well-known, 2026 cities!"));
        assertEquals(3, WritingScore.countWords("one -- two ... three"));
        assertEquals(2, WritingScore.countWords("Việt Nam"));
    }
}
