package com.ieltspath.learning.application.result;

/**
 * How much is waiting in the review basket, for a counter that must stay cheap: {@code required} is the number of
 * blocking reviews and {@code due} the number of maintained points whose review date has come. They equal the sizes of
 * the basket's {@code required} and {@code due} groups.
 */
public record ReviewBasketSummaryResult(int required, int due) {}
