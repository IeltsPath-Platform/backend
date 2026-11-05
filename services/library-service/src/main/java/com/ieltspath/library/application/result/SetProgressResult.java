package com.ieltspath.library.application.result;

import java.util.UUID;

public record SetProgressResult(UUID setId, long total, long learned, long learning, long needsReview, long notLearned,
                                int percent) {}
