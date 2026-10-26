package com.ieltspath.library.application.result;

import java.time.Instant;
import java.util.UUID;

public record RecentResult(UUID sessionId, String topic, long correct, long total, int percent, Instant finishedAt) {}
