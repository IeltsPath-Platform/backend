package com.ieltspath.learning.domain.service;

import java.util.Map;

/**
 * Writes a sound the way a learner's dictionary does. The speech service uses the strict IPA letters, which are right but
 * look odd to a learner: the English r is {@code ɹ} (a turned r), the r-coloured vowels are single letters
 * ({@code ɚ}, {@code ɝ}) and the g is the script {@code ɡ}. A learner reads /r/, /ər/, /ɜr/ and /g/.
 */
public final class LearnerIpa {
    private static final Map<String, String> DICTIONARY_FORM = Map.of(
            "ɹ", "r",
            "ɚ", "ər",
            "ɝ", "ɜr",
            "ɡ", "g");

    private LearnerIpa() {
    }

    /** The sound as written in a learner's dictionary; a sound with no other form, and null, come back unchanged. */
    public static String of(String symbol) {
        return symbol == null ? null : DICTIONARY_FORM.getOrDefault(symbol, symbol);
    }
}
