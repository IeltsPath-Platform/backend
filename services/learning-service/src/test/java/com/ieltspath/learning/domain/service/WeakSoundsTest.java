package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.SpokenPhoneme;
import com.ieltspath.learning.domain.vo.SpokenWord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeakSoundsTest {

    private static SpokenWord word(String text, SpokenPhoneme... sounds) {
        return new SpokenWord(text, 0, 300, 50, "None", List.of(sounds));
    }

    @Test
    void aSoundWeakInSeveralWordsIsOneEntryWithItsCountAndWhatWasHeardInstead() {
        var sounds = WeakSounds.select(List.of(
                word("think", new SpokenPhoneme("θ", 25, "s"), new SpokenPhoneme("k", 90)),
                word("thanks", new SpokenPhoneme("θ", 15, "s")),
                word("three", new SpokenPhoneme("θ", 35, "f")),
                word("fine", new SpokenPhoneme("f", 40))), 5);

        assertThat(sounds).hasSize(2);
        assertThat(sounds.get(0).symbol()).isEqualTo("θ");
        assertThat(sounds.get(0).count()).isEqualTo(3);
        assertThat(sounds.get(0).meanAccuracy()).isEqualTo(25.0);
        assertThat(sounds.get(0).heardAs()).isEqualTo("s");
        assertThat(sounds.get(0).words()).containsExactly("thanks", "think", "three");
        assertThat(sounds.get(1).symbol()).isEqualTo("f");
        assertThat(sounds.get(1).heardAs()).isNull();
    }

    @Test
    void soundsSaidWellAndWordsWithoutSoundScoresAddNothing() {
        assertThat(WeakSounds.select(List.of(word("good", new SpokenPhoneme("g", 95)),
                new SpokenWord("old", 0, 100, 30, "None")), 5)).isEmpty();
    }

    @Test
    void theListIsCutToTheLimit() {
        var many = List.of(word("a", new SpokenPhoneme("p", 10)), word("b", new SpokenPhoneme("b", 10)),
                word("c", new SpokenPhoneme("t", 10)));

        assertThat(WeakSounds.select(many, 2)).hasSize(2);
    }
}