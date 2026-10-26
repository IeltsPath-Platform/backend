package com.ieltspath.library.domain.vo;

import com.ieltspath.library.domain.exception.InvalidDataException;

public enum UnderstandingLevel {
    UNKNOWN(1), FAMILIAR(2), LEARNING(3), MASTERED(4);

    private final int targetBox;

    UnderstandingLevel(int targetBox) { this.targetBox = targetBox; }

    public int targetBox() { return targetBox; }

    public static UnderstandingLevel fromBox(int box) {
        return switch (box) {
            case 1 -> UNKNOWN;
            case 2 -> FAMILIAR;
            case 3 -> LEARNING;
            case 4, 5 -> MASTERED;
            default -> throw new InvalidDataException("Leitner box is invalid");
        };
    }
}
