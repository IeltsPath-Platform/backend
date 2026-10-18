package com.ieltspath.learning.domain.vo;

/** The seven league tiers, lowest first. {@link #level()} is the number stored in the database. */
public enum LeagueTier {
    BRONZE, SILVER, GOLD, SAPPHIRE, DIAMOND, RUBY, AMETHYST;

    public static final int LOWEST = 1;
    public static final int HIGHEST = values().length;

    public int level() {
        return ordinal() + 1;
    }

    public static LeagueTier ofLevel(int level) {
        return values()[level - 1];
    }
}
