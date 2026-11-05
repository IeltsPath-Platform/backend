package com.ieltspath.learning.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LearnerIpaTest {

    @Test
    void writesTheSoundsALearnerSeesInADictionary() {
        assertThat(LearnerIpa.of("ɹ")).isEqualTo("r");
        assertThat(LearnerIpa.of("ɚ")).isEqualTo("ər");
        assertThat(LearnerIpa.of("ɝ")).isEqualTo("ɜr");
        assertThat(LearnerIpa.of("ɡ")).isEqualTo("g");
    }

    @Test
    void leavesEveryOtherSoundAsItIs() {
        for (String symbol : new String[]{"æ", "ŋ", "θ", "tʃ", "aɪ", "oʊ", "ə", "ʃ", "r"}) {
            assertThat(LearnerIpa.of(symbol)).isEqualTo(symbol);
        }
        assertThat(LearnerIpa.of(null)).isNull();
    }
}