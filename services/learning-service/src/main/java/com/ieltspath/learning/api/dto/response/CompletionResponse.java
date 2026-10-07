package com.ieltspath.learning.api.dto.response;

import java.util.UUID;

public record CompletionResponse(UUID lessonId, String status) {}
