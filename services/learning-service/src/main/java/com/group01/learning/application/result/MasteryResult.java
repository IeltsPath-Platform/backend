package com.group01.learning.application.result;

import java.util.UUID;

public record MasteryResult(UUID knowledgePointId, UUID topicId, double mastery, long evidenceCount) {}
