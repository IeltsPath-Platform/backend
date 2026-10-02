package com.group01.learning.api.dto.response;

import java.util.UUID;

public record CompletionResponse(UUID lessonId, String status) {}
