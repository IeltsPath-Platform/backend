package com.ieltspath.events.messaging;

/** @param moved how many messages were republished in this call; @param remaining how many are still in the dead-letter queue. */
public record ReplayResult(String deadLetterQueue, int moved, long remaining) { }
