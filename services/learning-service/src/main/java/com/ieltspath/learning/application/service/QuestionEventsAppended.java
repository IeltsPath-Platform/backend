package com.ieltspath.learning.application.service;

import java.util.UUID;

/** Published when question events of a learner were written; {@link FlagDetector} looks at that learner once it commits. */
public record QuestionEventsAppended(UUID userId) {}
