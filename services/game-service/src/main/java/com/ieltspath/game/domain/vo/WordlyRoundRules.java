package com.ieltspath.game.domain.vo;

import java.time.*;
import java.util.*;

public final class WordlyRoundRules {
    private WordlyRoundRules() {}

    public static List<String> evaluate(String answer, String guess) {
        if (answer == null
                || guess == null
                || !answer.matches("[A-Z]{1,100}")
                || !guess.matches("[A-Z]{1,100}"))
            throw new IllegalArgumentException("Words must contain uppercase English letters");
        if (guess.length() != answer.length())
            throw new IllegalArgumentException("Invalid guess length");
        String[] result = new String[answer.length()];
        int[] remaining = new int[26];
        for (int i = 0; i < answer.length(); i++) {
            if (answer.charAt(i) == guess.charAt(i)) result[i] = "CORRECT";
            else remaining[answer.charAt(i) - 'A']++;
        }
        for (int i = 0; i < answer.length(); i++)
            if (result[i] == null) {
                int c = guess.charAt(i) - 'A';
                result[i] = remaining[c] > 0 ? "PRESENT" : "ABSENT";
                if (remaining[c] > 0) remaining[c]--;
            }
        return List.of(result);
    }

    public static int poolPosition(int sequence, int size) {
        if (sequence < 1 || size < 1)
            throw new IllegalArgumentException("Sequence and pool size must be positive");
        return ((sequence - 1) % size) + 1;
    }

    public static Reward reward(
            boolean solved, int streak, Instant started, Instant deadline, Instant received) {
        if (!solved) return new Reward(0, 0, 0, 0);
        long base =
                deadline == null
                        ? 100
                        : Math.max(
                                0, Math.min(180, Duration.between(received, deadline).toSeconds()));
        int next = streak + 1;
        return new Reward(
                base,
                next >= 3 ? 50 : 0,
                next,
                Math.max(0, Duration.between(started, received).toMillis()));
    }

    public record Reward(long base, long bonus, int streak, long solveMs) {}
}
