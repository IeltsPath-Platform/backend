package com.ieltspath.learning.domain.vo;

/**
 * One sound of a word as the speech service scored it: the sound that was expected (IPA), how well it was said (0 to 100)
 * and, when the service offers it, the sound that it heard instead. The sound heard is null when the expected sound was the
 * best match, which is the usual case for a sound said well.
 */
public record SpokenPhoneme(String symbol, double accuracy, String heardAs) {

    /** Sounds below this score are the ones a learner is told to practise. */
    public static final double WEAK_BELOW = 60.0;

    public SpokenPhoneme(String symbol, double accuracy) {
        this(symbol, accuracy, null);
    }

    /** Named {@code weak} and not {@code isWeak} so the stored JSON of a word does not gain a property of its own. */
    public boolean weak() {
        return accuracy < WEAK_BELOW;
    }
}
