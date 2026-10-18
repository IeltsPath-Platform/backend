package com.ieltspath.game.domain.vo;

import java.util.*;

public final class WordlyKeyboard {
    private WordlyKeyboard() {}

    public static Map<String, String> build(
            List<String> guesses, List<List<String>> rows, String revealedLetter) {
        Map<String, String> result = new TreeMap<>();
        Map<String, Integer> priority = Map.of("ABSENT", 0, "PRESENT", 1, "CORRECT", 2);
        for (int r = 0; r < guesses.size(); r++)
            for (int i = 0; i < guesses.get(r).length(); i++) {
                String letter = guesses.get(r).substring(i, i + 1), state = rows.get(r).get(i);
                result.merge(letter, state, (a, b) -> priority.get(a) >= priority.get(b) ? a : b);
            }
        if (revealedLetter != null) result.put(revealedLetter, "CORRECT");
        return result;
    }
}
