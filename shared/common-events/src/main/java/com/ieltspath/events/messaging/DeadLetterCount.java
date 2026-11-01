package com.ieltspath.events.messaging;

public record DeadLetterCount(String deadLetterQueue, String queue, long messages) { }
