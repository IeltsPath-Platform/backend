package com.ieltspath.game.domain.vo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Builds the three options of a Rocket round: the answer plus two wrong words from the same dictionary. Wrong words
 * never share the answer's word or meaning (ignoring case), never repeat, and favour a length close to the answer.
 * Everything is driven by a seed so a match can be rebuilt and tests are repeatable.
 */
public final class RocketDistractorPicker {
    public static final int DISTRACTORS = 2;
    /** Words within this many letters of the answer are preferred, so length alone does not give the answer away. */
    private static final int CLOSE_LENGTH = 2;

    private RocketDistractorPicker() {}

    public record Pick(List<RocketOption> options, UUID correctOptionId) {
        public Pick {
            options = List.copyOf(options);
        }
    }

    /** Seed for one round of one match; stable across restarts. */
    public static long seed(UUID matchId, int sequence) {
        return matchId.getMostSignificantBits() ^ Long.rotateLeft(matchId.getLeastSignificantBits(), 17)
                ^ (sequence * 0x9E3779B97F4A7C15L);
    }

    /** O(n) over the pool: one filtering pass, then a partial shuffle of at most the needed picks. */
    public static Pick pick(RocketWord answer, List<RocketWord> pool, long seed) {
        Random random = new Random(seed);
        Map<String, RocketWord> eligible = new LinkedHashMap<>();
        for (RocketWord word : pool) {
            if (word.wordKey().equals(answer.wordKey()) || word.meaningKey().equals(answer.meaningKey())) continue;
            eligible.putIfAbsent(word.wordKey(), word);
        }
        if (eligible.size() < DISTRACTORS)
            throw new IllegalStateException("Not enough vocabulary to build Rocket options");

        int length = answer.wordKey().length();
        List<RocketWord> close = new ArrayList<>();
        List<RocketWord> far = new ArrayList<>();
        for (RocketWord word : eligible.values())
            (Math.abs(word.wordKey().length() - length) <= CLOSE_LENGTH ? close : far).add(word);

        List<RocketWord> chosen = new ArrayList<>(DISTRACTORS);
        takeRandom(close, DISTRACTORS, random, chosen);
        takeRandom(far, DISTRACTORS - chosen.size(), random, chosen);

        List<RocketWord> words = new ArrayList<>(chosen);
        words.add(answer);
        shuffle(words, random);
        List<RocketOption> options = new ArrayList<>(words.size());
        UUID correct = null;
        for (RocketWord word : words) {
            UUID id = new UUID(random.nextLong(), random.nextLong());
            options.add(new RocketOption(id, word.sourceWordId(), word.word().trim()));
            if (word == answer) correct = id;
        }
        return new Pick(options, correct);
    }

    /** Moves up to {@code count} random items from {@code source} into {@code target} (partial Fisher–Yates). */
    private static void takeRandom(List<RocketWord> source, int count, Random random, List<RocketWord> target) {
        int take = Math.min(count, source.size());
        for (int i = 0; i < take; i++) {
            int j = i + random.nextInt(source.size() - i);
            RocketWord swap = source.get(i);
            source.set(i, source.get(j));
            source.set(j, swap);
            target.add(source.get(i));
        }
    }

    private static void shuffle(List<RocketWord> words, Random random) {
        for (int i = words.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            RocketWord swap = words.get(i);
            words.set(i, words.get(j));
            words.set(j, swap);
        }
    }
}
