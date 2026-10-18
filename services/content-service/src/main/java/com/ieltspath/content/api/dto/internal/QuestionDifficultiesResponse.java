package com.ieltspath.content.api.dto.internal;

import com.ieltspath.content.domain.vo.QuestionDifficulty;

import java.util.Map;
import java.util.UUID;

public record QuestionDifficultiesResponse(Map<UUID, QuestionDifficulty> difficulties) {}
