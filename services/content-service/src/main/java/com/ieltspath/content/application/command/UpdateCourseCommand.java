package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.ContentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCourseCommand(UUID id, String name, BigDecimal bandLevel, ContentStatus status) {}
