package com.ieltspath.learning.application.result;

import java.util.UUID;

public record StudyContextResult(String sourceType, UUID sourceId, UUID courseId) {}
