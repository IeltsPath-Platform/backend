package com.ieltspath.game.domain.vo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RocketDistractorPickerTest {
    private static RocketWord word(String text, String meaning) {
        return new RocketWord(UUID.nameUUIDFromBytes(text.getBytes()), text, meaning);
    }

    private final RocketWord actor = word("actor", "Diễn viên (nam)");
    private final List<RocketWord> bank = List.of(actor, word("driver", "Tài xế"), word("career", "Sự nghiệp"),
            word("teacher", "Giáo viên"), word("doctor", "Bác sĩ"), word("journey", "Hành trình"),
            word("library", "Thư viện"), word("weather", "Thời tiết"), word("achieve", "Đạt được"),
            word("improve", "Cải thiện"));

    @Test
    void threeDistinctOptionsWithExactlyOneCorrect() {
        var pick = RocketDistractorPicker.pick(actor, bank, 42);
        assertEquals(3, pick.options().size());
        assertEquals(3, pick.options().stream().map(RocketOption::word).distinct().count());
        assertEquals(3, pick.options().stream().map(RocketOption::optionId).distinct().count());
        var correct = pick.options().stream().filter(o -> o.optionId().equals(pick.correctOptionId())).toList();
        assertEquals(List.of("actor"), correct.stream().map(RocketOption::word).toList());
        assertEquals(actor.sourceWordId(), correct.getFirst().sourceWordId());
    }

    @Test
    void neverOffersTheSameWordOrMeaningAsTheAnswer() {
        var pool = new ArrayList<>(bank);
        pool.add(word("ACTOR", "a duplicate spelling"));
        pool.add(word("performer", "  diễn viên (NAM) "));
        pool.add(word("star", "Ngôi sao"));
        pool.add(word("film", "Bộ phim"));
        for (long seed = 0; seed < 200; seed++) {
            var words = RocketDistractorPicker.pick(actor, pool, seed).options().stream().map(RocketOption::word).toList();
            assertEquals(1, words.stream().filter(w -> w.equalsIgnoreCase("actor")).count());
            assertFalse(words.contains("performer"));
        }
    }

    @Test
    void sameSeedSameRoundAndSeedsVary() {
        long seed = RocketDistractorPicker.seed(UUID.fromString("4f83cb17-a344-40de-a021-87cdf82420f8"), 3);
        assertEquals(RocketDistractorPicker.pick(actor, bank, seed), RocketDistractorPicker.pick(actor, bank, seed));
        UUID match = UUID.randomUUID();
        Set<Long> seeds = new HashSet<>();
        for (int sequence = 1; sequence <= 20; sequence++) seeds.add(RocketDistractorPicker.seed(match, sequence));
        assertEquals(20, seeds.size());
        Set<List<String>> layouts = new HashSet<>();
        for (long s = 0; s < 50; s++)
            layouts.add(RocketDistractorPicker.pick(actor, bank, s).options().stream().map(RocketOption::word).toList());
        assertTrue(layouts.size() > 10, "options and their order should vary between rounds");
    }

    @Test
    void prefersWordsOfSimilarLength() {
        var pool = List.of(actor, word("ocean", "Đại dương"), word("tiger", "Con hổ"), word("extraordinarily", "Phi thường"),
                word("internationalisation", "Quốc tế hóa"));
        for (long seed = 0; seed < 100; seed++) {
            var words = RocketDistractorPicker.pick(actor, pool, seed).options().stream().map(RocketOption::word).toList();
            assertTrue(words.containsAll(List.of("ocean", "tiger")), words.toString());
        }
        var sparse = List.of(actor, word("ocean", "Đại dương"), word("extraordinarily", "Phi thường"));
        assertEquals(Set.of("actor", "ocean", "extraordinarily"), Set.copyOf(RocketDistractorPicker.pick(actor, sparse, 1)
                .options().stream().map(RocketOption::word).toList()));
    }

    @Test
    void failsClearlyWhenTheDictionaryIsTooSmall() {
        var tiny = List.of(actor, word("driver", "Tài xế"), word("performer", "Diễn viên (nam)"));
        assertThrows(IllegalStateException.class, () -> RocketDistractorPicker.pick(actor, tiny, 1));
    }
}
